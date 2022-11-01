package de.julielab.jcore.reader.xml;

import de.julielab.costosys.configuration.FieldConfig;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.jcore.types.casflow.ToVisit;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.types.ext.DBProcessingMetaData;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@ResourceMetaData(name = "JCoRe XML Database Multiplier", description = "This CAS multiplier receives information about " +
        "documents to be read from an instance of the XML Database Multiplier reader from the jcore-db-reader project. " +
        "The multiplier employs the jcore-xml-mapper to map the document XML structure into CAS instances. It also " +
        "supports additional tables sent by the DB Multiplier Reader that are then joined to the main table. This " +
        "mechanism is used to load separate data from additional database tables and populate the " +
        "CAS with them via the 'RowMapping' parameter. This component is part of the Jena Document Information System, " +
        "JeDIS."
        , vendor = "JULIE Lab Jena, Germany", copyright = "JULIE Lab Jena, Germany")
@TypeCapability(inputs = {"de.julielab.jcore.types.casmultiplier.RowBatch"}, outputs = {"de.julielab.jcore.types.casflow.ToVisit"})
public class XMLDBMultiplier extends DBMultiplier {
    public static final String PARAM_ROW_MAPPING = Initializer.PARAM_ROW_MAPPING;
    public static final String PARAM_MAPPING_FILE = Initializer.PARAM_MAPPING_FILE;
    public static final String PARAM_ADD_SHA_HASH = "AddShaHash";
    public static final String PARAM_TABLE_DOCUMENT = "DocumentTable";
    public static final String PARAM_TABLE_DOCUMENT_SCHEMA = "DocumentTableSchema";
    public static final String PARAM_TO_VISIT_KEYS = "ToVisitKeys";
    public static final String PARAM_ADD_TO_VISIT_KEYS = "AddToVisitKeys";
    public static final String PARAM_ADD_UNCHANGED_DOCUMENT_TEXT_FLAG = "AddUnchangedDocumentTextFlag";
    public static final String PARAM_TRUNCATE_AT_SIZE = "TruncateAtSize";

    private final static Logger log = LoggerFactory.getLogger(XMLDBMultiplier.class);
    /**
     * Mapper which maps medline XML to a CAS with the specified UIMA type system
     * via an XML configuration file.
     */
    protected XMLMapper xmlMapper;
    @ConfigurationParameter(name = PARAM_ROW_MAPPING, mandatory = false, description = XMLDBReader.DESC_ROW_MAPPING)
    protected String[] rowMappingArray;
    @ConfigurationParameter(name = PARAM_MAPPING_FILE, description = XMLDBReader.DESC_MAPPING_FILE)
    protected String mappingFileStr;
    @ConfigurationParameter(name = PARAM_ADD_SHA_HASH, mandatory = false, description = "For use with AnnotationDefinedFlowController and XMIDBWriter. Possible values: document_text, defaults to 'document_text' and thus doesn't need to be specified manually at the moment. This parameter needs to match the value for the same parameter given to the XMIDBWriter in this pipeline. Then, a comparison between the existing hash in the database and the new hash of the CAS read in this pipeline can be made. In case the hashes match, the CAS is directly routed to the components specified in the " + PARAM_TO_VISIT_KEYS + " parameter, skipping all other components. Note that this only works with AAEs where the first component is an 'AnnotationControlledFlow'. Additionally, the DBProcessingMetaData#hasDocumentHashChanged is set. This can be used by the XMIDBWriter to omit the reset of mirror subsets when updating the base document when the actual CAS text stayed the same.")
    private String documentItemToHash;
    @ConfigurationParameter(name = PARAM_TABLE_DOCUMENT, mandatory = false, description = "For use with AnnotationDefinedFlowController. String parameter indicating the name of the " +
            "table where the XMI data and, thus, the hash is stored. The name must be schema qualified. Note that in this component, only the ToVisit annotation is created that determines which components to apply to a CAS with matching (unchanged) hash. The logic to actually control the CAS flow is contained in the AnnotationDefinedFlowController.")
    private String xmiStorageDataTable;
    @ConfigurationParameter(name = PARAM_TABLE_DOCUMENT_SCHEMA, mandatory = false, description = "For use with AnnotationDefinedFlowController. The name of the schema that the document table - given with the " + PARAM_TABLE_DOCUMENT + " parameter - adheres to. Only the primary key part is required for hash value retrieval.")
    private String xmiStorageDataTableSchema;
    @ConfigurationParameter(name = PARAM_TO_VISIT_KEYS, mandatory = false, description = "For use with AnnotationDefinedFlowController. Specifies the delegate AE keys of the AEs this CAS should still applied on although the hash has not changed. Can be null or empty indicating that no component should be applied to the CAS. The task of the AnnotationDefinedFlowController is then to read those annotations and route the CAS accordingly.")
    private String[] toVisitKeys;
    @ConfigurationParameter(name = PARAM_ADD_TO_VISIT_KEYS, mandatory = false, description = "Toggles the creation of annotations for the AnnotationDefinedFlowController. Only needed when such a flow controller is used in the pipeline. For details, see the description of " + PARAM_TO_VISIT_KEYS + ".")
    private boolean addToVisitKeys;
    @ConfigurationParameter(name = PARAM_ADD_UNCHANGED_DOCUMENT_TEXT_FLAG, mandatory = false, description = "Toggles the addition of the 'document text is unchanged' flag. The value of this flag is determined via a SHA256 hash of the CAS document text. When " + PARAM_TABLE_DOCUMENT + " and " + PARAM_TABLE_DOCUMENT_SCHEMA + " are specified, the hash value of the document in storage is retrieved and compared to the current value. The flag is then set with respect to the comparison result.")
    private boolean addUnchangedDocumentTextFlag;
    @ConfigurationParameter(name = PARAM_TRUNCATE_AT_SIZE, mandatory = false, description = "Specify size in bytes of the XML document size. If the document surpasses that size, it is not populated from XMI but given some placeholder information. This can be necessary when large documents cannot be handled by subsequent components in the pipeline.")
    private int truncationSize;


    private Row2CasMapper row2CasMapper;
    private CasPopulator casPopulator;
    private Map<String, String> docId2HashMap;
    private boolean initialized;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        mappingFileStr = (String) aContext.getConfigParameterValue(PARAM_MAPPING_FILE);
        rowMappingArray = (String[]) aContext.getConfigParameterValue(PARAM_ROW_MAPPING);
        xmiStorageDataTable = (String) aContext.getConfigParameterValue(PARAM_TABLE_DOCUMENT);
        xmiStorageDataTableSchema = (String) aContext.getConfigParameterValue(PARAM_TABLE_DOCUMENT_SCHEMA);
        documentItemToHash = Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_ADD_SHA_HASH)).orElse("document_text");
        toVisitKeys = (String[]) aContext.getConfigParameterValue(PARAM_TO_VISIT_KEYS);
        addToVisitKeys = (boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ADD_TO_VISIT_KEYS)).orElse(false);
        addUnchangedDocumentTextFlag = (boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ADD_UNCHANGED_DOCUMENT_TEXT_FLAG)).orElse(false);
        truncationSize = Optional.ofNullable((Integer)aContext.getConfigParameterValue(PARAM_TRUNCATE_AT_SIZE)).orElse(Integer.MAX_VALUE);
        // We don't know yet which tables to read. Thus, we leave the row mapping out.
        // We will now once the DBMultiplier#process(JCas) will have been run.
        Initializer initializer = new Initializer(mappingFileStr, null, null);
        xmlMapper = initializer.getXmlMapper();
        initialized = false;

        if ((addToVisitKeys || addUnchangedDocumentTextFlag)) {
            if (!(xmiStorageDataTable == null && xmiStorageDataTableSchema == null) && !(xmiStorageDataTable != null && xmiStorageDataTableSchema != null && documentItemToHash != null)) {
                String errorMsg = String.format("From the parameters '%s' and '%s' some are specified and some aren't. To activate hash value comparison in order to add aggregate component keys for CAS visit, specify all those parameters. Otherwise, specify none.", PARAM_TABLE_DOCUMENT, PARAM_TABLE_DOCUMENT_SCHEMA);
                log.error(errorMsg);
                throw new ResourceInitializationException(new IllegalArgumentException(errorMsg));
            }
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        super.process(aJCas);
        docId2HashMap = fetchCurrentHashesFromDatabase(JCasUtil.selectSingle(aJCas, RowBatch.class));
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        JCas jCas = getEmptyJCas();
        try {
            if (documentDataIterator.hasNext()) {
                if (!initialized) {
                    try {
                        row2CasMapper = new Row2CasMapper(rowMappingArray, () -> getAllRetrievedColumns());
                    } catch (ResourceInitializationException e) {
                        throw new AnalysisEngineProcessException(e);
                    }
                    // The DBC is initialized in the super class in the process() method. Thus, at this point
                    // the DBC should be set.
                    if (xmiStorageDataTable != null && !dbc.withConnectionQueryBoolean(d -> d.tableExists(xmiStorageDataTable)))
                        throw new AnalysisEngineProcessException(new IllegalArgumentException("The data table" + xmiStorageDataTable + " to retrieve hash values from for document text change detection does not exist in the database: " + dbc.getDbURL()));
                    casPopulator = new CasPopulator(dbc, xmlMapper, row2CasMapper, rowMappingArray, truncationSize);
                    initialized = true;
                }
                byte[][] documentData = documentDataIterator.next();
                populateCas(jCas, documentData);
                setToVisitAnnotation(jCas);
            }
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw e;
        }
        return jCas;
    }

    /**
     * <p>Creates a {@link ToVisit} annotation based on document text hash comparison and the defined parameter values.</p>
     * <p>Computes the hash of the newly read CAS and compares it to the hash for the same document retrieved from the
     * database, if present. If there was a hash in the database and the hash values are equal, creates the <tt>ToVisit</tt>
     * annotation and adds the toVisitKeys passed in the configuration of this component.</p>
     *
     * @param jCas The newly read JCas.
     */
    private void setToVisitAnnotation(JCas jCas) {
        if (addToVisitKeys || addUnchangedDocumentTextFlag) {
            DBProcessingMetaData dbProcessingMetaData = JCasUtil.selectSingle(jCas, DBProcessingMetaData.class);
            StringArray pkArray = dbProcessingMetaData.getPrimaryKey();
            String pkString = String.join(",", pkArray.toArray());
            String existingHash = docId2HashMap.get(pkString);
            if (existingHash != null) {
                String newHash = getHash(jCas);
                if (existingHash.equals(newHash)) {
                    if (log.isTraceEnabled())
                        log.trace("Document {} has a document text hash that equals the one present in the database. Creating a ToVisit annotation routing it only to the components with delegate keys {}.", pkString, toVisitKeys);
                    if (addUnchangedDocumentTextFlag)
                        dbProcessingMetaData.setIsDocumentHashUnchanged(true);
                    if (addToVisitKeys) {
                        ToVisit toVisit = new ToVisit(jCas);
                        if (toVisitKeys != null && toVisitKeys.length != 0) {
                            StringArray keysArray = new StringArray(jCas, toVisitKeys.length);
                            keysArray.copyFromArray(toVisitKeys, 0, 0, toVisitKeys.length);
                            toVisit.setDelegateKeys(keysArray);
                        }
                        toVisit.addToIndexes();
                    }
                }
            } else {
                log.trace("No existing hash was found for document {}", pkString);
            }
        }
    }

    private String getHash(JCas newCas) {
        final String documentText = newCas.getDocumentText();
        final byte[] sha = DigestUtils.sha256(documentText.getBytes());
        return Base64.encodeBase64String(sha);
    }

    private void populateCas(JCas jCas, byte[][] documentData) throws AnalysisEngineProcessException {
        try {
            casPopulator.populateCas(jCas, documentData,
                    (docData, jcas) -> DBReader.setDBProcessingMetaData(dbc, readDataTable, tableName, docData, jcas));
        } catch (CasPopulationException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    protected List<Map<String, Object>> getAllRetrievedColumns() {
        Pair<Integer, List<Map<String, String>>> numColumnsAndFields = dbc.getNumColumnsAndFields(tables.length > 1, schemaNames);
        return numColumnsAndFields.getRight().stream().map(HashMap<String, Object>::new).collect(Collectors.toList());
    }

    /**
     * <p>Fetches the hashes of the currently stored documents in the database.</p>
     *
     * @param rowBatch The annotation specifying which documents should be fetched by the multiplier and then be processed by the aggregate.
     * @return A map from a string representation of the RowBatches document IDs to the hashes for the respective IDs.
     * @throws AnalysisEngineProcessException If the SQL request fails.
     */
    private Map<String, String> fetchCurrentHashesFromDatabase(RowBatch rowBatch) throws AnalysisEngineProcessException {
        if ((addToVisitKeys || addUnchangedDocumentTextFlag) && rowBatch.getIdentifiers() != null && rowBatch.getIdentifiers().size() > 0) {
            String hashColumn = documentItemToHash + "_sha256";
            // Extract the document IDs in this RowBatch. The IDs could be composite keys.
            List<String[]> documentIds = new ArrayList<>(rowBatch.getIdentifiers().size());
            Iterator<FeatureStructure> documentIDsIt = rowBatch.getIdentifiers().iterator();
            while (documentIDsIt.hasNext()) {
                StringArray pkArray = (StringArray) documentIDsIt.next();
                documentIds.add(pkArray.toStringArray());
            }
            Map<String, String> id2hash = new HashMap<>(documentIds.size());
            // This is the map we want to fill that lets us look up the hash of the document text by document ID.
            String sql = null;
            // Query the database for the document IDs in the current RowBatch and retrieve hashes.
            try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
                FieldConfig xmiTableSchema = dbc.getFieldConfiguration(xmiStorageDataTableSchema);
                String idQuery = documentIds.stream()
                        .map(key -> Arrays.stream(key).map(part -> "%s='" + part + "'").toArray(String[]::new))
                        .map(xmiTableSchema::expandPKNames).map(expandedKeys -> String.join(" AND ", expandedKeys))
                        .collect(Collectors.joining(" OR "));
                sql = String.format("SELECT %s,%s FROM %s WHERE %s", xmiTableSchema.getPrimaryKeyString(), hashColumn, xmiStorageDataTable, idQuery);
                ResultSet rs = conn.createStatement().executeQuery(sql);
                while (rs.next()) {
                    StringBuilder pkSb = new StringBuilder();
                    for (int i = 0; i < xmiTableSchema.getPrimaryKey().length; i++)
                        pkSb.append(rs.getString(i + 1)).append(',');
                    // Remove trailing comma
                    pkSb.deleteCharAt(pkSb.length() - 1);
                    String hash = rs.getString(xmiTableSchema.getPrimaryKey().length + 1);
                    id2hash.put(pkSb.toString(), hash);
                }
            } catch (SQLException e) {
                log.error("Could not retrieve hashes from the database. SQL query was '{}':", sql, e);
                throw new AnalysisEngineProcessException(e);
            }
            return id2hash;
        }
        return null;
    }
}
