package de.julielab.jcore.reader.db;

import de.julielab.costosys.cli.TableNotFoundException;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DBCIterator;
import de.julielab.costosys.dbconnection.util.TableSchemaMismatchException;
import de.julielab.jcore.types.casmultiplier.RowBatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.ducc.Workitem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ResourceMetaData(name = "JCoRe Database Multiplier Reader", description = "A collection reader that receives the IDs of documents from a database table. " +
        "Additional tables may be specified which will, together with the IDs, be sent to a CAS multiplier extending " +
        "the DBMultiplierReader. The multiplier will read documents and the joined additional tables according to the " +
        "list of document IDs sent by this reader. The component leverages the corpus storage system (CoStoSys) for this " +
        "purpose and is part of the Jena Document Information System, JeDIS."
        , vendor = "JULIE Lab Jena, Germany", copyright = "JULIE Lab Jena, Germany")
public class DBMultiplierReader extends DBSubsetReader {
    public static final String PARAM_RESET_TABLE = SubsetReaderConstants.PARAM_RESET_TABLE;
    public final static String PARAM_TABLE = TableReaderConstants.PARAM_TABLE;
    public final static String PARAM_COSTOSYS_CONFIG_NAME = TableReaderConstants.PARAM_COSTOSYS_CONFIG_NAME;
    public final static String PARAM_DATA_TIMESTAMP = SubsetReaderConstants.PARAM_DATA_TIMESTAMP;
    public final static String PARAM_ADDITIONAL_TABLES = SubsetReaderConstants.PARAM_ADDITIONAL_TABLES;
    public final static String PARAM_ADDITIONAL_TABLE_SCHEMAS = SubsetReaderConstants.PARAM_ADDITIONAL_TABLE_SCHEMAS;
    public final static String PARAM_FETCH_IDS_PROACTIVELY = SubsetReaderConstants.PARAM_FETCH_IDS_PROACTIVELY;
    public static final String PARAM_SEND_CAS_TO_LAST = "SendCasToLast";

    @ConfigurationParameter(name = PARAM_SEND_CAS_TO_LAST, mandatory = false, defaultValue = "false", description = "UIMA DUCC relevant parameter when using a CAS multiplier. When set to true, the worker CAS from the collection reader is forwarded to the last component in the pipeline. This can be used to send information about the progress to the CAS consumer in order to have it perform batch operations. For this purpose, a feature structure of type WorkItem from the DUCC library is added to the worker CAS. This feature structure has information about the current progress.")
    private boolean sendCasToLast;

    private final static Logger log = LoggerFactory.getLogger(DBMultiplierReader.class);
    // Internal state fields
    private DBMultiplierReader.RetrievingThread retriever;
    private DBCIterator<Object[]> dataTableDocumentIds;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        // Check whether a subset table name or a data table name was given.
        if (readDataTable) {
            log.debug("Reading from data table {}", tableName);
            dataTableDocumentIds = dbc.query(tableName, Arrays.asList(dbc.getFieldConfiguration(dbc.getActiveTableSchema()).getPrimaryKey()));
            hasNext = dataTableDocumentIds.hasNext();
        } else {
            log.debug("Reading from subset table {}", tableName);
            hasNext = dbc.withConnectionQueryBoolean(c -> c.hasUnfetchedRows(tableName));
        }
    }

    @Override
    public void getNext(JCas jCas) throws CollectionException {
        log.trace("Requesting next batch of document IDs from the database.");
        List<Object[]> idList = getNextDocumentIdBatch();
        log.trace("Received a list of {} ID from the database.", idList.size());
        if (log.isTraceEnabled()) {
            log.trace("IDs of the current batch: {}", idList.stream().map(Arrays::toString).collect(Collectors.joining(", ")));
        }
        RowBatch rowbatch = new RowBatch(jCas);
        FSArray ids = new FSArray(jCas, idList.size());
        for (int i = 0; i < idList.size(); i++) {
            Object[] objects = idList.get(i);
            StringArray keys = new StringArray(jCas, objects.length);
            for (int j = 0; j < objects.length; j++) {
                Object object = objects[j];
                // Here we convert all key elements to strings. UIMA doesn't allow untyped objects.
                keys.set(j, String.valueOf(object));
            }
            ids.set(i, keys);
        }

        StringArray tableArray = new StringArray(jCas, tables.length);
        StringArray schemaArray = new StringArray(jCas, schemas.length);
        for (int i = 0; i < tables.length; i++) {
            String table = tables[i];
            String schema = schemas[i];
            tableArray.set(i, table);
            schemaArray.set(i, schema);
        }

        rowbatch.setIdentifiers(ids);
        rowbatch.setTables(tableArray);
        rowbatch.setTableName(tableName);
        rowbatch.setTableSchemas(schemaArray);
        rowbatch.setCostosysConfiguration(costosysConfig);
        rowbatch.addToIndexes();

        if (sendCasToLast) {
            try {
                Workitem workitem = new Workitem(jCas);
                // Send the work item CAS also to the consumer. Normally, only the CASes emitted by the CAS multiplier
                // will be routed to the consumer. We do this to let the consumer know that the work item has been
                // finished.
                workitem.setSendToLast(true);
                workitem.setBlockindex(processedDocuments / batchSize);
                if (!hasNext())
                    workitem.setLastBlock(true);
                workitem.addToIndexes();
            } catch (IOException e) {
                log.error("Error occurred while creating Workitem feature structure", e);
                throw new CollectionException(e);
            }
        }
    }


    /*
     * If you overwrite this method you have to call super.hasNext().
     *
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
     */
    public boolean hasNext() throws IOException, CollectionException {
        boolean hasNext = this.hasNext;
        if (retriever != null)
            hasNext = !retriever.getDocumentIds().isEmpty();
        if (!hasNext)
            close();
        return hasNext;
    }

    /**
     * Returns the next batch of document IDs from the database table given by the 'Table' parameter.
     *
     * @return A list of document IDs from the read table.
     */
    public List<Object[]> getNextDocumentIdBatch() {

        List<Object[]> next;
        if (readDataTable)
            next = getNextFromDataTable();
        else
            next = getNextFromSubset();

        if (next != null)
            processedDocuments += next.size();


        return next;
    }

    private List<Object[]> getNextFromDataTable() {
        List<Object[]> next = new ArrayList<>(batchSize);
        // Must be set to true again if the iterator has more elements.
        hasNext = false;
        log.trace("Filling document ID list with the next batch of documents.");
        while (dataTableDocumentIds.hasNext() && next.size() < batchSize)
            next.add(dataTableDocumentIds.next());
        // totalDocumentCount could be set to the Limit parameter. Thus we
        // should stop when we reach the limit. and not set hasNext back to
        // true.
        if (processedDocuments < totalDocumentCount - 1) {
            log.trace("Checking if there are more documents to read from the data table.");
            hasNext = dataTableDocumentIds.hasNext();
        }
        return next;
    }

    private List<Object[]> getNextFromSubset() {
        List<Object[]> idList;

        // When this method is called for the first time, no retriever thread
        // will yet exist. Initialize it.
        if (retriever == null) {
            retriever = new DBMultiplierReader.RetrievingThread();
        }
        idList = retriever.getDocumentIds();
        // While returning the current set of IDs, already fetch the next batch
        if (fetchIdsProactively)
            retriever = new DBMultiplierReader.RetrievingThread();

        return idList;
    }


    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(processedDocuments, totalDocumentCount, Progress.ENTITIES, true)};
    }

    @Override
    public void close() {
        if (dbc != null)
            dbc.close();
        dbc = null;
    }

    /**
     * <p>
     * This class is charged to retrieve batches of document IDs which will be
     * returned for processing afterwards.
     * </p>
     * <p>
     * The class manages itself the <code>FetchIdsProactively</code> parameter which
     * can be given to the reader. When set to <code>false</code>, no ID batches are
     * fetched in advance but are fetched exactly on demand in
     * {@link DBReader#getNextArtifactData}.
     * </p>
     * <p>
     * This class is only in use when reading from a subset table.
     * </p>
     *
     * @author hellrich/faessler
     */
    protected class RetrievingThread extends Thread {
        private List<Object[]> ids;

        public RetrievingThread() {
            // Only fetch ID batches in advance when the parameter is set to
            // true.
            if (fetchIdsProactively) {
                log.debug("Fetching ID batches in a background thread.");
                setName(DBMultiplierReader.class.getSimpleName() + " RetrievingThread (" + getName() + ")");
                start();
            }
        }

        public void run() {
            // Remember: If the Limit parameter is set, totalDocumentCount is
            // that limit (or the remaining number of documents, if that's
            // lower).
            // Hence, we fetch the next "normal" sized batch of documents or, if
            // the limit comes to its end or almost all documents in the
            // database have been read, only the rest of documents.
            int limit = Math.min(batchSize, totalDocumentCount - numberFetchedDocIDs);
            try {
                try (CoStoSysConnection ignored = dbc.obtainOrReserveConnection()) {
                    log.trace("Using connection {} to retrieveAndMark", ignored.getConnection());
                    ids = dbc.retrieveAndMark(tableName, getClass().getSimpleName(), hostName, pid, limit, selectionOrder);
                }
                if (log.isTraceEnabled()) {
                    List<String> idStrings = new ArrayList<>();
                    for (Object[] o : ids) {
                        List<String> pkElements = new ArrayList<>();
                        for (Object object : o) {
                            pkElements.add(String.valueOf(object));
                        }
                        idStrings.add(StringUtils.join(pkElements, "-"));
                    }
                    log.trace("Reserved the following document IDs for processing: " + idStrings);
                }
                numberFetchedDocIDs += ids.size();
                log.debug("Retrieved {} document IDs to fetch from the database.", ids.size());
            } catch (TableSchemaMismatchException e) {
                log.error("Table schema mismatch: The active table schema {} specified in the CoStoSys configuration" +
                                " file {} does not match the columns in the subset table {}: {}", dbc.getActiveTableSchema(),
                        costosysConfig, tableName, e.getMessage());
                throw new IllegalArgumentException(e);
            } catch (TableNotFoundException e) {
                log.error("A table to read from could not be found", e);
                throw new IllegalArgumentException(e);
            }
        }

        public List<Object[]> getDocumentIds() {
            // If we don't use this as a background thread, we have to get the
            // IDs now in a sequential manner.
            if (!fetchIdsProactively) {
                // Use run as we don't have a use for real threads anyway.
                log.debug("Fetching new documents (without employing a background thread).");
                run();
            }
            try {
                // If this is a background thread started with start(): Wait for
                // the IDs to be retrieved, i.e. that run() ends.
                log.debug("Waiting for the background thread to finish fetching documents to return them.");
                join();
                return ids;
            } catch (InterruptedException e) {
                log.error("Background ID fetching thread was interrupted", e);
            }
            return null;
        }
    }

}
