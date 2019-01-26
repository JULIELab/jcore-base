package de.julielab.jcore.ae.checkpoint;

import de.julielab.jcore.types.ext.DBProcessingMetaData;
import de.julielab.jcore.utility.JCoReTools;
import de.julielab.xmlData.Constants;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.CoStoSysConnection;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ResourceMetaData(name = "JCoRe Database Checkpoint AE", description = "This component can be used when using a JCoRe database reader that reads from a CoStoSys/JeDIS subset. Enters the configured component name in the 'last component' column. Can also mark documents as being completely processed.")
public class DBCheckpointAE extends JCasAnnotator_ImplBase {

    public static final String PARAM_CHECKPOINT_NAME = "CheckpointName";
    public static final String PARAM_INDICATE_FINISHED = "IndicateFinished";
    public static final String PARAM_COSTOSYS_CONFIG = "CostosysConfigFile";
    public static final String PARAM_WRITE_BATCH_SIZE = "WriteBatchSize";
    private final static Logger log = LoggerFactory.getLogger(DBCheckpointAE.class);
    private DataBaseConnector dbc;

    @ConfigurationParameter(name = PARAM_CHECKPOINT_NAME)
    private String componentDbName;

    @ConfigurationParameter(name = PARAM_INDICATE_FINISHED, mandatory = false)
    private boolean indicateFinished;

    @ConfigurationParameter(name = PARAM_COSTOSYS_CONFIG, description = "File path or classpath resource location " +
            "of a Corpus Storage System (CoStoSys) configuration file. This file specifies the database to " +
            "write the XMI data into and the data table schema. This schema must at least define the primary key " +
            "columns that the storage tables should have for each document. The primary key is currently just " +
            "the document ID. Thus, at the moment, primary keys can only consist of a single element when using " +
            "this component. This is a shortcoming of this specific component and must be changed here, if necessary.")
    private String dbcConfigPath;

    @ConfigurationParameter(name = PARAM_WRITE_BATCH_SIZE, mandatory = false, defaultValue = "50", description =
            "The number of processed CASes after which the checkpoint should be written into the database. Defaults to 50.")
    private int writeBatchSize;

    private String subsetTable;

    private List<DocumentId> docIds;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        componentDbName = (String) aContext.getConfigParameterValue(PARAM_CHECKPOINT_NAME);
        dbcConfigPath = (String) aContext.getConfigParameterValue(PARAM_COSTOSYS_CONFIG);
        indicateFinished = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_INDICATE_FINISHED)).orElse(false);
        writeBatchSize = Optional.ofNullable((Integer) aContext.getConfigParameterValue(PARAM_WRITE_BATCH_SIZE)).orElse(50);
        try {
            dbc = new DataBaseConnector(dbcConfigPath);
        } catch (FileNotFoundException e) {
            log.error("Could not initiate database connector", e);
            throw new ResourceInitializationException(e);
        }
        docIds = new ArrayList<>();
        log.info("{}: {}", PARAM_CHECKPOINT_NAME, componentDbName);
        log.info("{}: {}", PARAM_INDICATE_FINISHED, indicateFinished);
        log.info("{}: {}", PARAM_CHECKPOINT_NAME, componentDbName);
        log.info("{}: {}", PARAM_WRITE_BATCH_SIZE, writeBatchSize);
    }

    @Override
    public void batchProcessComplete() throws AnalysisEngineProcessException {
        super.batchProcessComplete();
        log.debug("BatchProcessComplete called, writing checkpoints {} to database", docIds.size());
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            setLastComponent(conn, subsetTable, docIds, indicateFinished, dbc.getActiveTableFieldConfiguration());
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        log.debug("CollectionProcessComplete called, writing {} checkpoints to database", docIds.size());
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            setLastComponent(conn, subsetTable, docIds,indicateFinished, dbc.getActiveTableFieldConfiguration());
        }
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        DocumentId documentId;
        String docId;
        try {
            final DBProcessingMetaData dbProcessingMetaData = JCasUtil.selectSingle(aJCas, DBProcessingMetaData.class);
            documentId = new DocumentId(dbProcessingMetaData);
            if (subsetTable == null)
                subsetTable = dbProcessingMetaData.getSubsetTable();
            if (subsetTable == null) {
                if (dbProcessingMetaData.getSubsetTable() == null) {
                    log.error("The subset table retrieved from the DBProcessingMetaData is null. Cannot continue without the table name.");
                    throw new AnalysisEngineProcessException(new IllegalStateException("The subset table retrieved from the DBProcessingMetaData is null. Cannot continue without the table name."));
                }
                subsetTable = dbProcessingMetaData.getSubsetTable();
            }
            docIds.add(documentId);
            log.trace("Adding document ID {} for subset table {} for checkpoint marking", documentId, subsetTable);
        } catch (IllegalArgumentException e) {
            docId = JCoReTools.getDocId(aJCas);
            log.error("The document with document ID {} does not have an annotation of type {}. This annotation ought to contain the name of the subset table. It should be set by the DB reader. Cannot write the checkpoint to the datbase since the target subset table or its schema is unknown.", docId, DBProcessingMetaData.class.getCanonicalName());
            throw new AnalysisEngineProcessException(e);
        }
    }

    /**
     * Writes the component name to the database subset
     *
     * @param conn
     * @throws AnalysisEngineProcessException
     */
    private void setLastComponent(CoStoSysConnection conn, String
            subsetTableName, List<DocumentId> processedDocumentIds, boolean markIsProcessed, FieldConfig annotationFieldConfig) throws AnalysisEngineProcessException {
        if (processedDocumentIds.isEmpty() || StringUtils.isBlank(subsetTableName)) {
            log.debug("Not setting the last component because the processed document IDs list is empty (size: {}) or the subset table name wasn't found (is: {})", processedDocumentIds.size(), subsetTableName);
            return;
        }

        String[] primaryKey = annotationFieldConfig.getPrimaryKey();
        if (primaryKey.length > 1)
            throw new IllegalArgumentException("Currently, only one-element primary keys are supported.");
        // create a string for the prepared statement in the form "pk1 = ? AND pk2 = ? ..."
        String primaryKeyPsString = StringUtils.join(annotationFieldConfig.expandPKNames("%s = ?"), " AND ");

        log.debug("Marking {} documents to having been processed by component \"{}\".", processedDocumentIds.size(), componentDbName);

        String sql = String.format("UPDATE %s SET %s='%s' WHERE %s", subsetTableName, Constants.LAST_COMPONENT, componentDbName, primaryKeyPsString);
        if (markIsProcessed)
            sql = String.format("UPDATE %s SET %s='%s', %s=TRUE WHERE %s", subsetTableName, Constants.LAST_COMPONENT, componentDbName, Constants.IS_PROCESSED, primaryKeyPsString);

        try {
            boolean tryagain;
            do {
                tryagain = false;
                PreparedStatement ps = conn.prepareStatement(sql);
                for (DocumentId docId : processedDocumentIds) {
                    for (int i = 0; i < docId.getId().length; i++) {
                        String pkElement = docId.getId()[i];
                        ps.setString(i + 1, pkElement);
                    }
                    ps.addBatch();
                }
                try {
                    ps.executeBatch();
                } catch (BatchUpdateException e) {
                    if (e.getMessage().contains("deadlock detected")) {
                        log.debug("Database transaction deadlock detected while trying to set the last component. Trying again.");
                        tryagain = true;
                    }
                }
            } while (tryagain);
        } catch (SQLException e) {
            e.printStackTrace();
            SQLException nextException = e.getNextException();
            if (null == nextException)
                throw new AnalysisEngineProcessException(e);
            else
                nextException.printStackTrace();
            throw new AnalysisEngineProcessException(nextException);
        } finally {
            processedDocumentIds.clear();
        }
    }

}
