package de.julielab.jcore.reader.xmi;

import de.julielab.costosys.configuration.FieldConfig;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.XmiSplitConstants;
import de.julielab.xml.binary.BinaryJeDISNodeEncoder;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class XmiDBMultiplier extends DBMultiplier implements Initializable {
    public static final String PARAM_LOG_FINAL_XMI = Initializer.PARAM_LOG_FINAL_XMI;
    private final static Logger log = LoggerFactory.getLogger(XmiDBMultiplier.class);
    @ConfigurationParameter(name = PARAM_LOG_FINAL_XMI, mandatory = false, defaultValue = "false", description = "For debugging purposes. If set to true, before parsing the final XMI data assembled from the annotation modules, it is printed to console.")
    private boolean logFinalXmi;
    private Initializer initializer;
    private CasPopulator casPopulator;
    private String[] xmiModuleAnnotationNames;
    private boolean doGzip;
    private boolean useBinaryFormat;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logFinalXmi = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_LOG_FINAL_XMI)).orElse(false);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        boolean initDone = super.initialized;
        RowBatch rowBatch = null;
        if (!initDone) {
            try {
                rowBatch = JCasUtil.selectSingle(aJCas, RowBatch.class);
                adaptReaderConfigurationForXmiData(rowBatch);
                if (rowBatch.getXmiAnnotationModuleNames() != null)
                    xmiModuleAnnotationNames = rowBatch.getXmiAnnotationModuleNames().toStringArray();
            } catch (ResourceInitializationException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
        try {
            // The DBMultiplier is getting the IDs to read, the table names and schemas an eventually creates
            // the documentDataIterator that we can use in next().
            super.process(aJCas);
            // Now all global variables, most importantly "tables" and "schemaNames" have been initialized
            if (initializer == null) {
                log.debug("Initializing");
                initializer = new Initializer(this, dbc, xmiModuleAnnotationNames, xmiModuleAnnotationNames.length > 0, useBinaryFormat);
                initializer.initialize(rowBatch);
                initializer.setLogFinalXmi(logFinalXmi);
                casPopulator = new CasPopulator(dataTable, initializer, readDataTable, tableName);
            }
        } catch (Throwable t) {
            log.error("Error when initializing: ", t);
            throw new AnalysisEngineProcessException(t);
        }
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        JCas jCas = getEmptyJCas();
        try {
            if (documentDataIterator.hasNext()) {
                log.trace("Returning next CAS");
                try {
                    initializer.initializeAnnotationTableNames(jCas);
                } catch (ResourceInitializationException e) {
                    throw new AnalysisEngineProcessException(e);
                }
                populateCas(jCas);
            }
        } catch (Throwable throwable) {
            log.error("Error while reading document from the database. Releasing the CAS. ", throwable);
            jCas.release();
            throw new AnalysisEngineProcessException(throwable);
        }
        return jCas;
    }

    private void populateCas(JCas jCas) throws AnalysisEngineProcessException {
        if (casPopulator == null)
            throw new AnalysisEngineProcessException(new IllegalStateException("Initialization of the component was not finished. See previous errors to learn the reason. Cannot continue."));
        try {
            final byte[][] data = documentDataIterator.next();
            log.trace("Populating CAS with {}", casPopulator);
            if (data != null)
                casPopulator.populateCas(data, jCas);
        } catch (CasPopulationException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }


    @Override
    public String[] getAdditionalTableNames() {
        // The XMI multiplier doesn't use table joining
        return new String[0];
    }

    @Override
    public String[] getTables() {
        return tables;
    }

    /**
     * Must be called before super.initialize(context). Sets up table schemas for XMI data so the user doesn't have
     * to do it.
     *
     * @param rowBatch
     * @throws ResourceInitializationException
     */
    private void adaptReaderConfigurationForXmiData(RowBatch rowBatch) throws ResourceInitializationException {

        String costosysConfig = rowBatch.getCostosysConfiguration();
        try {
            dbc = new DataBaseConnector(costosysConfig);
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
        try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
            List<Map<String, String>> primaryKeyFields = dbc.getActiveTableFieldConfiguration().getPrimaryKeyFields().collect(Collectors.toList());
            if (rowBatch.getReadsBaseXmiDocument()) {

                tableName = rowBatch.getTableName();
                dataTable = rowBatch.getTables(0);
                determineDataFormat(dataTable);

                List<Map<String, String>> xmiAnnotationColumnsDefinitions = new ArrayList<>();
                for (String qualifiedAnnotation : rowBatch.getXmiAnnotationModuleNames()) {
                    final String columnName = qualifiedAnnotation.toLowerCase().replace('.', '_').replace(':', '$');
                    final Map<String, String> field = FieldConfig.createField(
                            JulieXMLConstants.NAME, columnName,
                            JulieXMLConstants.GZIP, String.valueOf(doGzip),
                            JulieXMLConstants.RETRIEVE, "true",
                            JulieXMLConstants.TYPE, doGzip || useBinaryFormat ? "bytea" : "xml"
                    );
                    xmiAnnotationColumnsDefinitions.add(field);
                }
                FieldConfig xmiDocumentTableSchema = dbc.addXmiTextFieldConfiguration(primaryKeyFields, xmiAnnotationColumnsDefinitions, doGzip);
                dbc.setActiveTableSchema(xmiDocumentTableSchema.getName());
                final String[] tables = rowBatch.getTables().toStringArray();
                String[] additionalTables = Arrays.copyOfRange(tables, 1, tables.length);
                if (additionalTables != null && additionalTables.length > 0) {
                    FieldConfig xmiAnnotationTableSchema = dbc.addXmiAnnotationFieldConfiguration(primaryKeyFields, doGzip);
                    rowBatch.setTableSchemas(1, xmiAnnotationTableSchema.getName());
                }
                XmiReaderUtils.checkXmiTableSchema(dbc, dataTable, xmiDocumentTableSchema, getClass().getSimpleName());
            } else {
                // Complete XMI reading mode
                String table = rowBatch.getTables(0);
                determineDataFormat(table);
                FieldConfig xmiDocumentFieldConfiguration = dbc.addXmiDocumentFieldConfiguration(primaryKeyFields, doGzip);
                dbc.setActiveTableSchema(xmiDocumentFieldConfiguration.getName());
            }
        }
    }

    private void determineDataFormat(String table) throws ResourceInitializationException {
        doGzip = true;
        useBinaryFormat = true;
        dataTable = dbc.getNextOrThisDataTable(table);
        log.debug("Fetching a single row from data table {} in order to determine whether data is in GZIP format", dataTable);
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(String.format("SELECT %s FROM %s LIMIT 1", XmiSplitConstants.BASE_DOC_COLUMN, dataTable));
            while (rs.next()) {
                byte[] xmiData = rs.getBytes(XmiSplitConstants.BASE_DOC_COLUMN);
                try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(xmiData))) {
                    byte[] firstTwoBytes = new byte[2];
                    gzis.read(firstTwoBytes);
                    checkForJeDISBinaryFormat(firstTwoBytes);
                } catch (IOException e) {
                    log.debug("Attempt to read XMI data in GZIP format failed. Assuming non-gzipped XMI data. Expected exception:", e);
                    doGzip = false;
                    checkForJeDISBinaryFormat(xmiData);
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist"))
                log.error("An exception occurred when trying to read the xmi column of the data table \"{}\". It seems the table does not contain XMI data and this is invalid to use with this reader.", dataTable);
            throw new ResourceInitializationException(e);
        }
    }

    private void checkForJeDISBinaryFormat(byte[] firstTwoBytes) {
        short header = (short) ((firstTwoBytes[0] << 8) | (0xff & firstTwoBytes[1]));
        if (header != BinaryJeDISNodeEncoder.JEDIS_BINARY_MAGIC) {
            useBinaryFormat = false;
            log.debug("Is data encoded in JeDIS binary format: false");
        } else {
            log.debug("Is data encoded in JeDIS binary format: true");
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        log.info("Closing database connector.");
        dbc.close();
    }
}
