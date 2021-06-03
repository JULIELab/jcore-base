package de.julielab.jcore.reader.xmi.flowcontroller;

import de.julielab.costosys.configuration.FieldConfig;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.JCasFlowController_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>Prereque</p>
 * <p>Expects a jCas as being output by the {@link de.julielab.jcore.reader.xmi.XmiDBMultiplierReader}, i.e. the CAS
 * should contain a {@link de.julielab.jcore.types.casmultiplier.RowBatch} annotation. Then, Retrieves the sha256 hashes for
 * the passed documents from the database.</p>
 */
@ResourceMetaData(name = "JCoRe Hash Comparison Flow Controller", description = "This flow controller aims to skip processing for CASes that already exist in the database and haven't changed with regards to a newly read version. For this purpose, the sha256 hash of the CAS document text is compared to the the existing hash in the database for the same document ID. If the hashes match, the text is the same and, thus, the annotations will be the same.")
public class HashComparisonFlowController extends JCasFlowController_ImplBase {
    public static final String PARAM_ADD_SHA_HASH = "AddShaHash";
    public static final String PARAM_TABLE_DOCUMENT = "DocumentTable";
    private final static Logger log = LoggerFactory.getLogger(HashComparisonFlowController.class);
    @ConfigurationParameter(name = DBReader.PARAM_COSTOSYS_CONFIG_NAME, description = "Path to the CoStoSys configuration XML file that specifies the database this pipeline writes to, i.e. the same file that the DB XMI Writer is using. If there is no DB Writer in use, this flow controller is not applicable.")
    private String costosysConfig;
    @ConfigurationParameter(name = PARAM_ADD_SHA_HASH, description = "Possible values: document_text, defaults to 'document_text' and thus doesn't need to be specified manually at the moment. This parameter needs to match the value for the same parameter given to the XMIDBWriter in this pipeline. Then, a comparison between the existing hash in the database and the new hash of the CAS read in this pipeline can be made. In case the hashes match, the CAS skips all component except the DBCheckpointAE to mark the document as processed.")
    private String documentItemToHash;
    @ConfigurationParameter(name = PARAM_TABLE_DOCUMENT, description = "String parameter indicating the name of the " +
            "table where the XMI data will be stored. The name must be schema qualified.")
    private String docTableParamValue;

    private DataBaseConnector dbc;

    @Override
    public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
        this.costosysConfig = (String) aContext.getConfigParameterValue(DBReader.PARAM_COSTOSYS_CONFIG_NAME);
        this.documentItemToHash = Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_ADD_SHA_HASH)).orElse("document_text");
        try {
            dbc = new DataBaseConnector(this.costosysConfig);
        } catch (FileNotFoundException e) {
            log.error("Could not create the CoStoSys DatabaseConnector:", e);
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public Flow computeFlow(JCas jCas) throws AnalysisEngineProcessException {
        RowBatch rowBatch;
        try {
            rowBatch = JCasUtil.selectSingle(jCas, RowBatch.class);
        } catch (IllegalArgumentException e) {
            log.error("Could not select the RowBatch annotation from the JCas:", e);
            throw new AnalysisEngineProcessException(e);
        }
        Map<String, String> id2hash = fetchCurrentHashesFromDatabase(rowBatch);
        return new HashComparisonOuterFlow(id2hash, documentItemToHash, getContext().getAggregateMetadata().getFlowConstraints());
    }

    /**
     * <p>Fetches the hashes of the currently stored documents in the database.</p>
     * @param rowBatch The annotation specifying which documents should be fetched by the multiplier and then be processed by the aggregate.
     * @return A map from a string representation of the RowBatches document IDs to the hashes for the respective IDs.
     * @throws AnalysisEngineProcessException If the SQL request fails.
     */
    private Map<String, String> fetchCurrentHashesFromDatabase(RowBatch rowBatch) throws AnalysisEngineProcessException {
        String dataTable = dbc.getNextDataTable(rowBatch.getTableName());
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
            FieldConfig activeTableFieldConfiguration = dbc.getActiveTableFieldConfiguration();
            String idQuery = documentIds.stream()
                    .map(key -> Arrays.stream(key).map(part -> "%s='" + part + '"').toArray(String[]::new))
                    .map(activeTableFieldConfiguration::expandPKNames).map(expandedKeys -> String.join(" AND ", expandedKeys))
                    .collect(Collectors.joining(" OR "));
            sql = String.format("SELECT %s,%s FROM %s WHERE %s", activeTableFieldConfiguration.getPrimaryKeyString(), hashColumn, dataTable, idQuery);
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                StringBuilder pkSb = new StringBuilder();
                for (int i = 0; i < activeTableFieldConfiguration.getPrimaryKey().length; i++)
                    pkSb.append(rs.getString(i)).append(',');
                // Remove training comma
                pkSb.deleteCharAt(pkSb.length());
                String hash = rs.getString(activeTableFieldConfiguration.getPrimaryKey().length);
                id2hash.put(pkSb.toString(), hash);
            }
        } catch (SQLException e) {
            log.error("Could not retrieve hashes from the database. SQL query was {}:", sql, e);
            throw new AnalysisEngineProcessException(e);
        }
        return id2hash;
    }
}
