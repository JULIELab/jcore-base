package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.reader.db.DBMultiplierReader;
import de.julielab.jcore.reader.db.SubsetReaderConstants;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.CoStoSysConnection;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@ResourceMetaData(name = "XMI Database Multiplier Reader", description = "This is an extension of the " +
        "DBMultiplierReader to handle JeDIS XMI annotation module data.")
public class XmiDBMultiplierReader extends DBMultiplierReader {
    public static final String PARAM_STORE_XMI_ID = Initializer.PARAM_STORE_XMI_ID;
    public static final String PARAM_READS_BASE_DOCUMENT = Initializer.PARAM_READS_BASE_DOCUMENT;
    public static final String PARAM_INCREASED_ATTRIBUTE_SIZE = Initializer.PARAM_INCREASED_ATTRIBUTE_SIZE;
    public static final String PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE = Initializer.PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE;
    private final static Logger log = LoggerFactory.getLogger(XmiDBMultiplierReader.class);
    @ConfigurationParameter(name = PARAM_READS_BASE_DOCUMENT, description = "Indicates if this reader reads segmented " +
            "annotation data. If set to false, the XMI data is expected to represent complete annotated documents. " +
            "If it is set to true, a segmented annotation graph is expected and the table given with the 'Table' parameter " +
            "will contain the document text together with some basic annotations. What exactly is stored in which manner " +
            "is determined by the jcore-xmi-db-consumer used to write the data into the database.")
    private Boolean readsBaseDocument;
    @ConfigurationParameter(name = PARAM_STORE_XMI_ID, mandatory = false, description = "This parameter is required " +
            "to be set to true, if this reader is contained in a pipeline that also contains a jcore-xmi-db-writer and" +
            "the writer will segment the CAS annotation graph and store only parts of it. Then, it is important to " +
            "keep track of the free XMI element IDs that may be assigned to new annotation elements to avoid " +
            "ID clashes when assembling an XMI document from separately stored annotation graph segments.")
    private Boolean storeMaxXmiId;
    @ConfigurationParameter(name = PARAM_INCREASED_ATTRIBUTE_SIZE, mandatory = false, description = "Maxmimum XML attribute " +
            "size in bytes. Since the CAS " +
            "document text is stored as an XMI attribute, it might happen for large documents that there is an error " +
            "because the maximum attribute size is exceeded. This parameter allows to specify the maxmimum " +
            " attribute size in order to avoid such errors. Should only be set if required.")
    private int maxXmlAttributeSize;
    @ConfigurationParameter(name = PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE, mandatory = false, description = "Initial XML " +
            "parser buffer size in bytes. For large documents, " +
            "it can happen that XMI parsing is extremely slow. By employing monitoring tools like the jconsole or " +
            "(j)visualvm, the hot spots of work can be identified. If one of those is the XML attribute buffer " +
            "resizing, this parameter should be set to a size that makes buffer resizing unnecessary.")
    private int xercesAttributeBufferSize;
    private boolean doGzip;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        adaptReaderConfigurationForXmiData();
        super.initialize(context);
        readsBaseDocument = (Boolean) (context.getConfigParameterValue(PARAM_READS_BASE_DOCUMENT) == null ? false
                : context.getConfigParameterValue(PARAM_READS_BASE_DOCUMENT));
        storeMaxXmiId = (Boolean) (context.getConfigParameterValue(PARAM_STORE_XMI_ID) == null ? false
                : context.getConfigParameterValue(PARAM_STORE_XMI_ID));
        readsBaseDocument = (Boolean) (context.getConfigParameterValue(PARAM_READS_BASE_DOCUMENT) == null ? false
                : context.getConfigParameterValue(PARAM_READS_BASE_DOCUMENT));
        Optional.ofNullable((Integer) context.getConfigParameterValue(PARAM_INCREASED_ATTRIBUTE_SIZE))
                .ifPresent(v -> maxXmlAttributeSize = v);
        Optional.ofNullable((Integer) context.getConfigParameterValue(PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE))
                .ifPresent(v -> xercesAttributeBufferSize = v);
    }

    @Override
    public void getNext(JCas jCas) throws CollectionException {
        try {
            super.getNext(jCas);
            final RowBatch rowBatch = JCasUtil.selectSingle(jCas, RowBatch.class);
            rowBatch.setReadsBaseXmiDocument(readsBaseDocument);
            if (additionalTableNames != null)
                rowBatch.setXmiAnnotationModuleNames(JCoReTools.newStringArray(jCas, additionalTableNames));
            rowBatch.setStoreMaxXmiId(storeMaxXmiId);
            rowBatch.setIncreasedAttributeSize(maxXmlAttributeSize);
            rowBatch.setXercesAttributeBufferSize(xercesAttributeBufferSize);
        } catch (Throwable throwable) {
            log.error("Exception ocurred while trying to get the next document", throwable);
            throw throwable;
        }
    }

    /**
     * Must be called before super.initialize(context). Sets up table schemas for XMI data so the user doesn't have
     * to do it.
     *
     * @throws ResourceInitializationException
     */
    private void adaptReaderConfigurationForXmiData() throws ResourceInitializationException {
        costosysConfig = (String) getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);
        try {
            dbc = new DataBaseConnector(costosysConfig);
        } catch (FileNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
        try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
            List<Map<String, String>> primaryKeyFields = dbc.getActiveTableFieldConfiguration().getPrimaryKeyFields().collect(Collectors.toList());
            if ((Boolean) getConfigParameterValue(PARAM_READS_BASE_DOCUMENT)) {

                String table = (String) getConfigParameterValue(PARAM_TABLE);
                determineDataInGzipFormat(table);

                FieldConfig xmiDocumentTableSchema = dbc.addXmiTextFieldConfiguration(primaryKeyFields, doGzip);
                dbc.setActiveTableSchema(xmiDocumentTableSchema.getName());
                String[] additionalTables = (String[]) getConfigParameterValue(SubsetReaderConstants.PARAM_ADDITIONAL_TABLES);
                if (additionalTables != null && additionalTables.length > 0) {
                    FieldConfig xmiAnnotationTableSchema = dbc.addXmiAnnotationFieldConfiguration(primaryKeyFields, doGzip);
                    setConfigParameterValue(SubsetReaderConstants.PARAM_ADDITIONAL_TABLE_SCHEMAS, new String[]{xmiAnnotationTableSchema.getName()});
                }
                XmiReaderUtils.checkXmiTableSchema(dbc, tableName, xmiDocumentTableSchema, getMetaData().getName());
            } else {
                // Complete XMI reading mode
                String table = (String) getConfigParameterValue(PARAM_TABLE);
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
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
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
