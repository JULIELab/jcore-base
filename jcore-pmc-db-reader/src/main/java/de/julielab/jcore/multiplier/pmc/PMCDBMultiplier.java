package de.julielab.jcore.multiplier.pmc;

import de.julielab.costosys.configuration.FieldConfig;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.jcore.reader.db.DBMultiplier;
import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.reader.pmc.CasPopulator;
import de.julielab.jcore.reader.pmc.NoDataAvailableException;
import de.julielab.jcore.reader.pmc.PMCReaderBase;
import de.julielab.jcore.reader.pmc.parser.ElementParsingException;
import de.julielab.jcore.types.casflow.ToVisit;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PMCDBMultiplier extends DBMultiplier {
    public static final String PARAM_OMIT_BIB_REFERENCES = PMCReaderBase.PARAM_OMIT_BIB_REFERENCES;
    public static final String PARAM_ADD_SHA_HASH = "AddShaHash";
    public static final String PARAM_TABLE_DOCUMENT = "DocumentTable";
    public static final String PARAM_TABLE_DOCUMENT_SCHEMA = "DocumentTableSchema";
    public static final String PARAM_TO_VISIT_KEYS = "ToVisitKeys";
    protected static final byte[] comma = ",".getBytes();
    private final static Logger log = LoggerFactory.getLogger(PMCDBMultiplier.class);
    @ConfigurationParameter(name = PARAM_OMIT_BIB_REFERENCES, mandatory = false, defaultValue = "false", description = "If set to true, references to the bibliography are omitted from the CAS text.")
    protected boolean omitBibReferences;
    @ConfigurationParameter(name = PARAM_ADD_SHA_HASH, mandatory = false, description = "For use with AnnotationDefinedFlowController. Possible values: document_text, defaults to 'document_text' and thus doesn't need to be specified manually at the moment. This parameter needs to match the value for the same parameter given to the XMIDBWriter in this pipeline. Then, a comparison between the existing hash in the database and the new hash of the CAS read in this pipeline can be made. In case the hashes match, the CAS is directly routed to the components specified in the " + PARAM_TO_VISIT_KEYS + " parameter, skipping all other components. Note that this only works with AAEs where the first component is an 'AnnotationControlledFlow'.")
    private String documentItemToHash;
    @ConfigurationParameter(name = PARAM_TABLE_DOCUMENT, mandatory = false, description = "For use with AnnotationDefinedFlowController. String parameter indicating the name of the " +
            "table where the XMI data and, thus, the hash is stored. The name must be schema qualified. Note that in this component, only the ToVisit annotation is created that determines which components to apply to a CAS with matching (unchanged) hash. The logic to actually control the CAS flow is contained in the AnnotationDefinedFlowController.")
    private String xmiStorageDataTable;
    @ConfigurationParameter(name = PARAM_TABLE_DOCUMENT_SCHEMA, mandatory = false, description = "For use with AnnotationDefinedFlowController. The name of the schema that the document table - given with the " + PARAM_TABLE_DOCUMENT + " parameter - adheres to. Only the primary key part is required for hash value retrieval.")
    private String xmiStorageDataTableSchema;
    @ConfigurationParameter(name = PARAM_TO_VISIT_KEYS, mandatory = false, description = "For use with AnnotationDefinedFlowController. The delegate AE keys of the AEs this CAS should still applied on although the hash has not changed. Can be null or empty indicating that no component should be applied to the CAS. This is, however, the task of the AnnotationDefinedFlowController.")
    private String[] toVisitKeys;

    private CasPopulator casPopulator;
    private Map<String, String> docId2HashMap;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        xmiStorageDataTable = (String) aContext.getConfigParameterValue(PARAM_TABLE_DOCUMENT);
        xmiStorageDataTableSchema = (String) aContext.getConfigParameterValue(PARAM_TABLE_DOCUMENT_SCHEMA);
        documentItemToHash = Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_ADD_SHA_HASH)).orElse("document_text");
        toVisitKeys = (String[]) aContext.getConfigParameterValue(PARAM_TO_VISIT_KEYS);
        omitBibReferences = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_OMIT_BIB_REFERENCES)).orElse(false);
        // We don't know yet which tables to read. Thus, we leave the row mapping out.
        // We will now once the DBMultiplier#process(JCas) will have been run.
        initialized = false;

        if (!(xmiStorageDataTable == null && xmiStorageDataTableSchema == null) && !(xmiStorageDataTable != null && xmiStorageDataTableSchema != null && documentItemToHash != null)) {
            String errorMsg = String.format("From the parameters '%s' and '%s' some are specified and some aren't. To activate hash value comparison in order to add aggregate component keys for CAS visit, specify all those parameters. Otherwise, specify none.", PARAM_TABLE_DOCUMENT, PARAM_TABLE_DOCUMENT_SCHEMA);
            log.error(errorMsg);
            throw new ResourceInitializationException(new IllegalArgumentException(errorMsg));
        }

        try {
            casPopulator = new CasPopulator(omitBibReferences);
        } catch (IOException e) {
            String errorMsg = "Could not initialize the PMC CasPopulator.";
            log.error(errorMsg);
            throw new ResourceInitializationException(e);
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
                byte[][] documentData = documentDataIterator.next();
                String pkString = DBReader.setDBProcessingMetaData(dbc, readDataTable, tableName, documentData, jCas);
                populateCas(jCas, documentData, pkString);
                setToVisitAnnotation(jCas, pkString);
            }
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw new AnalysisEngineProcessException(e);
        }
        return jCas;
    }

    private void populateCas(JCas jCas, byte[][] documentData, String pkString) throws NoDataAvailableException, ElementParsingException {
        List<Integer> pkIndices = dbc.getPrimaryKeyIndices();

        // get index of xmlData;
        // assumes that only one byte[] in arrayArray contains this data
        // and that this byte[] is at the only index position that holds no
        // primary key
        List<Integer> allIndices = new ArrayList<Integer>();
        for (int i = 0; i < documentData.length; i++) {
            allIndices.add(i);
        }
        List<Integer> xmlIndices = new ArrayList<>(allIndices);
        for (Integer pkIndex : pkIndices)
            xmlIndices.remove(pkIndex);
        int xmlIndex = xmlIndices.get(0);
        try {
            casPopulator.populateCas(new ByteArrayInputStream(documentData[xmlIndex]), jCas);
        } catch (Exception e) {
            log.error("Could not parse document {}.", pkString, e);
            throw e;
        }
        // It actually happens that some PMC XML documents do not contain their own ID. We can use the ID obtained
        // via the database primary key, which in turn might be derived from the original file name or some meta file.
        Header header = JCasUtil.selectSingle(jCas, Header.class);
        if (header.getDocId().isBlank()) {
            log.debug("Document has no docId set. Derived the ID {} from the primary key and setting it as the Header#docId feature.", pkString);
            header.setDocId(pkString);
        }
    }

    /**
     * <p>Fetches the hashes of the currently stored documents in the database.</p>
     *
     * @param rowBatch The annotation specifying which documents should be fetched by the multiplier and then be processed by the aggregate.
     * @return A map from a string representation of the RowBatches document IDs to the hashes for the respective IDs.
     * @throws AnalysisEngineProcessException If the SQL request fails.
     */
    private Map<String, String> fetchCurrentHashesFromDatabase(RowBatch rowBatch) throws AnalysisEngineProcessException {
        if (xmiStorageDataTable != null && dbc.tableExists(xmiStorageDataTable) && rowBatch.getIdentifiers() != null && rowBatch.getIdentifiers().size() > 0) {
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

    /**
     * <p>Creates a {@link ToVisit} annotation based on document text hash comparison and the defined parameter values.</p>
     * <p>Computes the hash of the newly read CAS and compares it to the hash for the same document retrieved from the
     * database, if present. If there was a hash in the database and the hash values are equal, creates the <tt>ToVisit</tt>
     * annotation and adds the toVisitKeys passed in the configuration of this component.</p>
     *
     * @param jCas The newly read JCas.
     * @param pkString
     */
    private void setToVisitAnnotation(JCas jCas, String pkString) {
        if (xmiStorageDataTable != null && dbc.tableExists(xmiStorageDataTable)) {
            String existingHash = docId2HashMap.get(pkString);
            if (existingHash != null) {
                String newHash = getHash(jCas);
                if (existingHash.equals(newHash)) {
                    if (log.isTraceEnabled())
                        log.trace("Document {} has a document text hash that equals the one present in the database. Creating a ToVisit annotation routing it only to the components with delegate keys {}.", pkString, toVisitKeys);
                    ToVisit toVisit = new ToVisit(jCas);
                    if (toVisitKeys != null && toVisitKeys.length != 0) {
                        StringArray keysArray = new StringArray(jCas, toVisitKeys.length);
                        keysArray.copyFromArray(toVisitKeys, 0, 0, toVisitKeys.length);
                        toVisit.setDelegateKeys(keysArray);
                    }
                    toVisit.addToIndexes();
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
}
