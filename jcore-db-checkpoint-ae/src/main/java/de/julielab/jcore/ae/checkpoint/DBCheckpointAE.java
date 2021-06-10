package de.julielab.jcore.ae.checkpoint;

import com.google.common.collect.Sets;
import de.julielab.costosys.Constants;
import de.julielab.costosys.configuration.FieldConfig;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.jcore.types.ext.DBProcessingMetaData;
import de.julielab.jcore.utility.JCoReTools;
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
import java.util.*;

@ResourceMetaData(name = "JCoRe Database Checkpoint AE", description = "This component can be used when using a JCoRe database reader that reads from a CoStoSys/JeDIS subset. Enters the configured component name in the 'last component' column. Can also mark documents as being completely processed.")
public class DBCheckpointAE extends JCasAnnotator_ImplBase {

    public static final String PARAM_CHECKPOINT_NAME = "CheckpointName";
    public static final String PARAM_INDICATE_FINISHED = "IndicateFinished";
    public static final String PARAM_COSTOSYS_CONFIG = "CostosysConfigFile";
    public static final String PARAM_WRITE_BATCH_SIZE = "WriteBatchSize";
    private final static Logger log = LoggerFactory.getLogger(DBCheckpointAE.class);
    private DataBaseConnector dbc;

    @ConfigurationParameter(name = PARAM_CHECKPOINT_NAME, description = "String parameter. A name that identifies this checkpoint in the database.")
    private String componentDbName;

    @ConfigurationParameter(name = PARAM_INDICATE_FINISHED, mandatory = false, description = "Whether or not the checkpoint should mark the end of processing of the pipeline. If set to true, this component will not only set its name as checkpoint in the subset table but also set the 'is processed' flag to true and the 'is in process' flag to false.")
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

    @ConfigurationParameter(name = DocumentReleaseCheckpoint.PARAM_JEDIS_SYNCHRONIZATION_KEY, mandatory = false, description = DocumentReleaseCheckpoint.SYNC_PARAM_DESC)
    private String jedisSyncKey;


    private String subsetTable;

    private Set<DocumentId> docIds;

    private DocumentReleaseCheckpoint docReleaseCheckpoint;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
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
        docIds = new HashSet<>();

        if (indicateFinished) {
            jedisSyncKey = (String) Optional.ofNullable(aContext.getConfigParameterValue(DocumentReleaseCheckpoint.PARAM_JEDIS_SYNCHRONIZATION_KEY)).orElse(getClass().getCanonicalName() + componentDbName);
            docReleaseCheckpoint = DocumentReleaseCheckpoint.get();
            docReleaseCheckpoint.register(jedisSyncKey);
        }

        log.info("{}: {}", PARAM_CHECKPOINT_NAME, componentDbName);
        log.info("{}: {}", PARAM_INDICATE_FINISHED, indicateFinished);
        log.info("{}: {}", PARAM_CHECKPOINT_NAME, componentDbName);
        log.info("{}: {}", PARAM_WRITE_BATCH_SIZE, writeBatchSize);
    }

    @Override
    public void batchProcessComplete() throws AnalysisEngineProcessException {
        super.batchProcessComplete();
        log.debug("BatchProcessComplete called, stashing {} documents to be ready for marked as being finished", docIds.size());
        if (indicateFinished)
            docReleaseCheckpoint.release(jedisSyncKey, docIds.stream());
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            setLastComponent(conn, subsetTable, indicateFinished, dbc.getActiveTableFieldConfiguration());
        }
        docIds.clear();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        log.debug("BatchProcessComplete called, stashing {} documents to be ready for marked as being finished", docIds.size());
        if (indicateFinished)
            docReleaseCheckpoint.release(jedisSyncKey, docIds.stream());
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            setLastComponent(conn, subsetTable, indicateFinished, dbc.getActiveTableFieldConfiguration());
        }
        docIds.clear();
        log.info("Closing database connector.");
        dbc.close();
    }

    private void customBatchProcessingComplete() throws AnalysisEngineProcessException {
        if (indicateFinished)
            docReleaseCheckpoint.release(jedisSyncKey, docIds.stream());
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            setLastComponent(conn, subsetTable, indicateFinished, dbc.getActiveTableFieldConfiguration());
        }
        docIds.clear();
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        DocumentId documentId;
        try {
            final DBProcessingMetaData dbProcessingMetaData = JCasUtil.selectSingle(aJCas, DBProcessingMetaData.class);
            if (!dbProcessingMetaData.getDoNotMarkAsProcessed()) {
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
                if (docIds.size() >= writeBatchSize) {
                    log.debug("Cached documents have reached the configured batch size of {}, sending to database.", writeBatchSize);
                    customBatchProcessingComplete();
                }
            }
        } catch (IllegalArgumentException e) {
            String docId = JCoReTools.getDocId(aJCas);
            log.error("The document with document ID {} does not have an annotation of type {}. This annotation ought to contain the name of the subset table. It should be set by the DB reader. Cannot write the checkpoint to the database since the target subset table or its schema is unknown.", docId, DBProcessingMetaData.class.getCanonicalName());
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
            subsetTableName, boolean markIsProcessed, FieldConfig annotationFieldConfig) throws AnalysisEngineProcessException {
        // When we just want to set the document DB processing checkpoint we will do this for the current batch of documents no matter if they have been released by other components
        Set<DocumentId> processedDocumentIds = Collections.emptySet();
        if (markIsProcessed) {
            // Add all documents released from all the different components to the current cache of released documents. The
            // cache is a multiset, counting the number of released for each document. This is important because we only
            // want to mark documents as being processed if all registered components are finished with it, i.e. have
            // stored their information about the document to the database, preventing data loss.
            processedDocumentIds = docReleaseCheckpoint.getReleasedDocumentIds();
        }
        Set<DocumentId> documentIdsToSetLastComponent = Sets.difference(docIds, processedDocumentIds);
        if ((documentIdsToSetLastComponent.isEmpty() && processedDocumentIds.isEmpty()) || StringUtils.isBlank(subsetTableName)) {
            log.debug("Not setting the last component to {} because the processed document IDs list is empty (size: {}) or the subset table name could not be retrieved (is: {})", componentDbName, documentIdsToSetLastComponent.size(), subsetTableName);
            return;
        }

        String[] primaryKey = annotationFieldConfig.getPrimaryKey();
        if (primaryKey.length > 1)
            throw new IllegalArgumentException("Currently, only one-element primary keys are supported.");
        // create a string for the prepared statement in the form "pk1 = ? AND pk2 = ? ..."
        String primaryKeyPsString = StringUtils.join(annotationFieldConfig.expandPKNames("%s = ?"), " AND ");


        String sqlSetLastComponent = String.format("UPDATE %s SET %s='%s' WHERE %s", subsetTableName, Constants.LAST_COMPONENT, componentDbName, primaryKeyPsString);
        String sqlMarkIsProcessed = null;
        if (markIsProcessed)
            sqlMarkIsProcessed = String.format("UPDATE %s SET %s='%s', %s=TRUE, %s=FALSE WHERE %s", subsetTableName, Constants.LAST_COMPONENT, componentDbName, Constants.IS_PROCESSED, Constants.IN_PROCESS, primaryKeyPsString);

        if (!documentIdsToSetLastComponent.isEmpty()) {
            log.debug("Setting the last component to {} for {} documents", componentDbName, documentIdsToSetLastComponent.size());
            updateSubsetTable(conn, documentIdsToSetLastComponent, sqlSetLastComponent);
        }
        if (markIsProcessed) {
            log.debug("Marking {} documents to having been processed by component \"{}\".", documentIdsToSetLastComponent.size(), componentDbName);
            updateSubsetTable(conn, processedDocumentIds, sqlMarkIsProcessed);
        }
    }

    private void updateSubsetTable(CoStoSysConnection conn, Collection<DocumentId> documentIdsToMark, String sql) throws AnalysisEngineProcessException {
        try {
            boolean tryagain;
            do {
                tryagain = false;
                PreparedStatement ps = conn.prepareStatement(sql);
                for (DocumentId docId : documentIdsToMark) {
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
        }
    }

}
