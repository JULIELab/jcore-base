package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.jcore.reader.db.SubsetReaderConstants;
import de.julielab.jcore.reader.db.TableReaderConstants;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.xml.XmiBuilder;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.CoStoSysConnection;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static de.julielab.jcore.reader.xmi.XmiDBReader.PARAM_READS_BASE_DOCUMENT;

public class XmiDBMultiplier extends DBMultiplier implements Initializable {

    private final static Logger log = LoggerFactory.getLogger(XmiDBMultiplier.class);

    private Initializer initializer;
    private CasPopulator casPopulator;
    private String[] xmiModuleAnnotationNames;
    private boolean doGzip;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
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
        // The DBMultiplier is getting the IDs to read, the table names and schemas an eventually creates
        // the documentDataIterator that we can use in next().
        super.process(aJCas);
        // Now all global variables, most importantly "tables" and "schemaNames" have been initialized
        if (initializer == null) {
            initializer = new Initializer(this, dbc, xmiModuleAnnotationNames, getAdditionalTableNames().length > 0);
            initializer.initialize(rowBatch);
            casPopulator = new CasPopulator(dataTable, initializer, readDataTable, tableName);
        }
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        JCas jCas = getEmptyJCas();
        if (documentDataIterator.hasNext()) {
            try {
                initializer.initializeAnnotationTableNames(jCas);
            } catch (ResourceInitializationException e) {
                throw new AnalysisEngineProcessException(e);
            }
            populateCas(jCas);
        }
        return jCas;
    }

    private void populateCas(JCas jCas) throws AnalysisEngineProcessException {
        try {
            final byte[][] data = documentDataIterator.next();
            if (data != null)
                casPopulator.populateCas(data, jCas);
        } catch (CasPopulationException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }


    @Override
    public String[] getAdditionalTableNames() {
        return tables.length > 1 ? Arrays.copyOfRange(tables, 1, tables.length) : new String[0];
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
                determineDataInGzipFormat(dataTable);

                FieldConfig xmiDocumentTableSchema = dbc.addXmiTextFieldConfiguration(primaryKeyFields, doGzip);
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
                determineDataInGzipFormat(table);
                FieldConfig xmiDocumentFieldConfiguration = dbc.addXmiDocumentFieldConfiguration(primaryKeyFields, doGzip);
                dbc.setActiveTableSchema(xmiDocumentFieldConfiguration.getName());
            }
        }
    }

    private void determineDataInGzipFormat(String table) throws ResourceInitializationException {
        doGzip = true;
        dataTable = dbc.getNextOrThisDataTable(table);
        log.debug("Fetching a single row from data table {} in order to determine whether data is in GZIP format", dataTable);
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()){
            ResultSet rs = conn.createStatement().executeQuery(String.format("SELECT xmi FROM %s LIMIT 1", dataTable));
            while (rs.next()) {
                byte[] xmiData = rs.getBytes("xmi");
                try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(xmiData))) {
                    gzis.read();
                } catch (IOException e) {
                    log.debug("Attempt to read XMI data in GZIP format failed. Assuming non-gzipped XMI data. Expected exception:", e);
                    doGzip = false;
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist"))
                log.error("An exception occurred when trying to read the xmi column of the data table \"{}\". It seems the table does not contain XMI data and this is invalid to use with this reader.", dataTable);
            throw new ResourceInitializationException(e);
        }
    }
}
