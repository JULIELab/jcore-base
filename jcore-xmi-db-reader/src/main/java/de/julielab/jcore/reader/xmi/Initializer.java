package de.julielab.jcore.reader.xmi;

import de.julielab.xml.JulieXMLConstants;
import de.julielab.xml.XmiBuilder;
import de.julielab.xml.XmiSplitConstants;
import de.julielab.xml.XmiSplitUtilities;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Initializer {
    public static final String PARAM_STORE_XMI_ID = "StoreMaxXmiId";
    public static final String PARAM_LOG_FINAL_XMI = "LogFinalXmi";
    public static final String PARAM_READS_BASE_DOCUMENT = "ReadsBaseDocument";
    public static final String PARAM_INCREASED_ATTRIBUTE_SIZE = "IncreasedAttributeSize";
    public static final String PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE = "XercesAttributeBufferSize";
    private final static Logger log = LoggerFactory.getLogger(Initializer.class);
    private Boolean storeMaxXmiId;
    private int maxXmlAttributeSize;
    private int xercesAttributeBufferSize;
    private Boolean readsBaseDocument;
    private boolean initializationComplete;
    private int numAdditionalTables;
    private int numDataRetrievedDataFields;

    private XmiBuilder builder;
    private Boolean logFinalXmi;
    private DataBaseConnector dbc;
    private final String[] additionalTableNames;
    private final boolean joinTables;
    private Initializable initializable;

    public XmiBuilder getXmiBuilder() {
        return builder;
    }

    public String[] getAdditionalTableNames() {

        return additionalTableNames;
    }

    public boolean isJoinTables() {
        return joinTables;
    }


    public Initializer(Initializable initializable, DataBaseConnector dbc, String[] additionalTableNames, boolean joinTables) {
        this.initializable = initializable;
        this.dbc = dbc;
        this.additionalTableNames = additionalTableNames;
        this.joinTables = joinTables;
    }

    public void initialize(UimaContext context) {
        // If the field is defined with gzip=true in the field configuration,
        // the unzipping happens automatically.
        boolean fieldIsSetToGzip = Boolean
                .parseBoolean(dbc.getFieldConfiguration().getFields().get(1).get(JulieXMLConstants.GZIP));
        storeMaxXmiId = (Boolean) (context.getConfigParameterValue(PARAM_STORE_XMI_ID) == null ? false
                : context.getConfigParameterValue(PARAM_STORE_XMI_ID));
        logFinalXmi = (Boolean) (context.getConfigParameterValue(PARAM_LOG_FINAL_XMI) == null ? false
                : context.getConfigParameterValue(PARAM_LOG_FINAL_XMI));
        readsBaseDocument = (Boolean) (context.getConfigParameterValue(PARAM_READS_BASE_DOCUMENT) == null ? false
                : context.getConfigParameterValue(PARAM_READS_BASE_DOCUMENT));
        Optional.ofNullable((Integer) context.getConfigParameterValue(PARAM_INCREASED_ATTRIBUTE_SIZE))
                .ifPresent(v -> maxXmlAttributeSize = v);
        Optional.ofNullable((Integer) context.getConfigParameterValue(PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE))
                .ifPresent(v -> xercesAttributeBufferSize = v);
        initializationComplete = true;
        numAdditionalTables = additionalTableNames == null ? 0 : additionalTableNames.length;
        if (joinTables)
            for (String annotation : additionalTableNames) {
                if (!annotation.contains(".")) {
                    initializationComplete = false;
                    log.debug(annotation
                            + " is not the fully qualified java name. Will retrieve the fully qualified java name"
                            + " from the types namespace and use this as table name.");
                }
            }
        // If we don't join tables, we assume that the read documents are
        // complete and valid. Thus, ignore the namespace table.
        Map<String, String> nsAndXmiVersion = null;
        if (joinTables || readsBaseDocument)
            nsAndXmiVersion = getNamespaceMap();
        // if the maxXmlAttributeSize is 0, the default is used
        builder = new XmiBuilder(nsAndXmiVersion, additionalTableNames, maxXmlAttributeSize);

        numDataRetrievedDataFields = dbc.getFieldConfiguration().getColumnsToRetrieve().length;

        log.info("{}: {}", PARAM_STORE_XMI_ID, storeMaxXmiId);
        log.info("{}: {}", PARAM_LOG_FINAL_XMI, logFinalXmi);
        log.info("{}: {}", PARAM_READS_BASE_DOCUMENT, readsBaseDocument);
        log.info("{}: {}", PARAM_INCREASED_ATTRIBUTE_SIZE, maxXmlAttributeSize);
        log.info("{}: {}", PARAM_XERCES_ATTRIBUTE_BUFFER_SIZE, xercesAttributeBufferSize);
        log.info("Data columns set for retrieval: {}",
                Arrays.toString(dbc.getFieldConfiguration().getColumnsToRetrieve()));


    }

    public void initializeAnnotationTableNames(JCas jCas) throws ResourceInitializationException {
        // If annotations have not been given as fully qualified java names
        // (i.e. e.g. de.julielab.jules.types.Token)
        // the default types namespace will be added and it will be checked if
        // the type system contains the thus
        // constructed type.
        if (!initializationComplete) {
            log.debug(
                    "Initializing annotation table table names from type system, if any additional table names are given.");
            String[] additionalTableNames = initializable.getAdditionalTableNames();
            String[] tables = initializable.getTables();
            TypeSystem typeSystem = jCas.getTypeSystem();
            for (int i = 0; i < additionalTableNames.length; i++) {
                if (!additionalTableNames[i].contains(".")) {
                    String typeName = XmiSplitUtilities.TYPES_NAMESPACE + additionalTableNames[i];
                    if (typeSystem.getType(typeName) != null) {
                        // A table cannot be created if the name contains dots.
                        // All annotation tables created
                        // via jules-cas-xmi-to-db-consumer will have dots
                        // replaced by underline.
                        String tableName = typeName.replace(".", "_");
                        tableName = dbc.getActiveDataPGSchema() + "." + tableName;
                        tables[i + 1] = tableName;
                    } else {
                        throw new ResourceInitializationException(new IllegalStateException(
                                "Could not retrieve the fully qualified java name for type " + additionalTableNames[i]
                                        + " from the types namespace in order to use it as table name."
                                        + " Please specify the fully qualified java name for this type"));
                    }
                }
            }
        }
    }

    private Map<String, String> getNamespaceMap() {
        Map<String, String> map = null;
        if (dbc.tableExists(dbc.getActiveDataPGSchema() + "." + XmiSplitConstants.XMI_NS_TABLE)) {
            try (Connection conn = dbc.getConn()) {
                map = new HashMap<>();
                conn.setAutoCommit(true);
                Statement stmt = conn.createStatement();
                String sql = String.format("SELECT %s,%s FROM %s", XmiSplitConstants.PREFIX, XmiSplitConstants.NS_URI,
                        dbc.getActiveDataPGSchema() + "." + XmiSplitConstants.XMI_NS_TABLE);
                ResultSet rs = stmt.executeQuery(String.format(sql));
                while (rs.next())
                    map.put(rs.getString(1), rs.getString(2));
            } catch (SQLException e) {
                e.printStackTrace();
                SQLException ne = e.getNextException();
                if (null != ne)
                    ne.printStackTrace();
            }
        } else {
            log.warn(
                    "Table \"{}\" was not found it is assumed that the table from which is read contains complete XMI documents.",
                    dbc.getActiveDataPGSchema() + "." + XmiSplitConstants.XMI_NS_TABLE);
        }
        return map;
    }

    public boolean getReadsBaseDocument() {
        return readsBaseDocument;
    }

    public Boolean getStoreMaxXmiId() {
        return storeMaxXmiId;
    }

    public int getNumAdditionalTables() {
        return numAdditionalTables;
    }

    public int getNumDataRetrievedDataFields() {
        return numDataRetrievedDataFields;
    }

    public Boolean getLogFinalXmi() {
        return logFinalXmi;
    }

    public DataBaseConnector getDataBaseConnector() {
        return dbc;
    }

    public int getXercesAttributeBufferSize() {
        return xercesAttributeBufferSize;
    }
}