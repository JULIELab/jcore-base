package de.julielab.jcore.reader.xmi;

import de.julielab.costosys.cli.TableNotFoundException;
import de.julielab.costosys.configuration.FieldConfig;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.reader.db.DBMultiplierReader;
import de.julielab.jcore.reader.db.SubsetReaderConstants;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.XmiSplitConstants;
import de.julielab.xml.binary.BinaryJeDISNodeEncoder;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

@ResourceMetaData(name = "JCoRe XMI Database Multiplier Reader", description = "This is an extension of the " +
        "DBMultiplierReader to handle JeDIS XMI annotation module data.")
public class XmiDBMultiplierReader extends DBMultiplierReader {
    public static final String PARAM_STORE_XMI_ID = Initializer.PARAM_STORE_XMI_ID;
    public static final String PARAM_READS_BASE_DOCUMENT = Initializer.PARAM_READS_BASE_DOCUMENT;
    public static final String PARAM_INCREASED_ATTRIBUTE_SIZE = Initializer.PARAM_INCREASED_ATTRIBUTE_SIZE;
    public static final String PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE = Initializer.PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE;
    public static final String PARAM_ANNOTATIONS_TO_LOAD = Initializer.PARAM_ANNOTATIONS_TO_LOAD;
    public static final String PARAM_XMI_META_SCHEMA = "XmiMetaTablesSchema";
    private final static Logger log = LoggerFactory.getLogger(XmiDBMultiplierReader.class);
    @ConfigurationParameter(name = PARAM_READS_BASE_DOCUMENT, description = "Indicates if this reader reads segmented " +
            "annotation data. If set to false, the XMI data is expected to represent complete annotated documents. " +
            "If it is set to true, a segmented annotation graph is expected and the table given with the 'Table' parameter " +
            "will contain the document text together with some basic annotations. What exactly is stored in which manner " +
            "is determined by the jcore-xmi-db-consumer used to write the data into the database.")
    private Boolean readsBaseDocument;
    @ConfigurationParameter(name = PARAM_ANNOTATIONS_TO_LOAD, mandatory = false, description = "An array of qualified UIMA type names. The provided names will be converted to database table column names in an equivalent manner as the XMIDBWriter does when storing the annotations. Thus, by default the columns of the XMI table holding annotation module information are named by lowercased UIMA type name where dots are replaced by underscores.. This can be overwritten by appending '<schema>:' to a table name. The given type names will be converted to valid Postgres columns names by replacing dots with underscores and the colon will be converted to the dollar character. From the resolved columns, annotation modules in segmented XMI format are read where an annotation module contains all annotation instances of a specific type in a specific document. All annotation modules read this way are merged with the base document, resulting in valid XMI data which is then deserialized into the CAS.")
    protected String[] qualifiedAnnotationColumnNames;
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
    @ConfigurationParameter(name = PARAM_XMI_META_SCHEMA, mandatory = false, defaultValue = "public", description = "Each XMI file defines a number of XML namespaces according to the types used in the document. Those namespaces are stored in a table named '" +XmiSplitConstants.XMI_NS_TABLE + "' when splitting annotations in annotation modules by the XMI DB writer. This parameter allows to specify in which Postgres schema this table should be looked for. Also, the table listing the annotation tables is stored in this Postgres schema. Defaults to 'public'.")
    private String xmiMetaSchema;
    private boolean doGzip;
    private String[] additionalTableNames;
    private boolean useBinaryFormat;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        this.qualifiedAnnotationColumnNames = (String[]) context.getConfigParameterValue(PARAM_ANNOTATIONS_TO_LOAD);
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
        xmiMetaSchema = Optional.ofNullable((String) context.getConfigParameterValue(PARAM_XMI_META_SCHEMA)).orElse("public");
        super.initialize(context);
    }

    @Override
    public void getNext(JCas jCas) throws CollectionException {
        try {
            super.getNext(jCas);
            final RowBatch rowBatch = JCasUtil.selectSingle(jCas, RowBatch.class);
            rowBatch.setReadsBaseXmiDocument(readsBaseDocument);
            if (qualifiedAnnotationColumnNames != null)
                rowBatch.setXmiAnnotationModuleNames(JCoReTools.newStringArray(jCas, qualifiedAnnotationColumnNames));
            rowBatch.setStoreMaxXmiId(storeMaxXmiId);
            rowBatch.setIncreasedAttributeSize(maxXmlAttributeSize);
            rowBatch.setXercesAttributeBufferSize(xercesAttributeBufferSize);
            rowBatch.setXmiMetaTablesPostgresSchema(xmiMetaSchema);
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
            String table = (String) getConfigParameterValue(PARAM_TABLE);
            if (!dbc.tableExists(table))
                throw new ResourceInitializationException(new TableNotFoundException("Table " + table + " does not exist in database " + dbc.getDbURL()));
            if ((Boolean) getConfigParameterValue(PARAM_READS_BASE_DOCUMENT)) {

                determineDataFormat(table);

                List<Map<String, String>> xmiAnnotationColumnsDefinitions = new ArrayList<>();
                for (String qualifiedAnnotation : qualifiedAnnotationColumnNames) {
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

                XmiReaderUtils.checkXmiTableSchema(dbc, tableName, xmiDocumentTableSchema, getMetaData().getName());
            } else {
                // Complete XMI reading mode
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
        short header = (short) ((firstTwoBytes[0]<<8) | (0xff & firstTwoBytes[1]));
        if (header != BinaryJeDISNodeEncoder.JEDIS_BINARY_MAGIC) {
            useBinaryFormat = false;
            log.debug("Is data encoded in JeDIS binary format: false");
        } else {
            log.debug("Is data encoded in JeDIS binary format: true");
        }
    }
}
