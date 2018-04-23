package de.julielab.jcore.reader.db;

import de.julielab.jcore.types.casmultiplier.RowBatch;
import de.julielab.xmlData.dataBase.DBCIterator;
import de.julielab.xmlData.dataBase.util.TableSchemaMismatchException;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
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

@ResourceMetaData(name="JCoRe Database Multiplier Reader", description = "A collection reader that receives the IDs of documents from a database table. " +
        "Additional tables may be specified which will, together with the IDs, be sent to a CAS multiplier extending" +
        "the DBMultiplierReader. The multiplier will read documents and the joined additional tables according to the " +
        "list of document IDs sent by this reader. The component leverages the corpus storage system (CoStoSys) for this " +
        "purpose and is part of the Jena Document Information System, JeDIS."
        , vendor = "JULIE Lab Jena, Germany", copyright = "JULIE Lab Jena, Germany")
public class DBMultiplierReader extends DBSubsetReader {
    private final static Logger log = LoggerFactory.getLogger(DBMultiplierReader.class);

    // Internal state fields
    private DBMultiplierReader.RetrievingThread retriever;
    private DBCIterator<Object[]> dataTableDocumentIds;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        // Check whether a subset table name or a data table name was given.
        if (readDataTable) {
            dataTableDocumentIds = dbc.query(tableName, Arrays.asList(dbc.getFieldConfiguration(dbc.getActiveTableSchema()).getPrimaryKey()));
            hasNext = dataTableDocumentIds.hasNext();
        } else {
            hasNext = dbc.hasUnfetchedRows(tableName);
        }
    }

    @Override
    public void getNext(JCas jCas) throws CollectionException {
        List<Object[]> idList = getNextDocumentIdBatch();
        log.trace("Received a list of {} ID from the database.", idList.size());
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
        StringArray schemaArray = new StringArray(jCas, tables.length);
        for (int i = 0; i < tables.length; i++) {
            String table = tables[i];
            String schema = schemas[i];
            tableArray.set(i, table);
            schemaArray.set(i, schema);
        }

        rowbatch.setIdentifiers(ids);
        rowbatch.setTables(tableArray);
        rowbatch.setTableSchemas(schemaArray);
        rowbatch.setCostosysConfiguration(costosysConfig);
        rowbatch.addToIndexes();
    }


    /*
     * If you overwrite this method you have to call super.hasNext().
     *
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
     */
    public boolean hasNext() throws IOException, CollectionException {
        if (retriever != null)
            return !retriever.getDocumentIds().isEmpty();
        return hasNext;
    }

    /**
     * Returns the next batch of document IDs from the database table given by the 'Table' parameter.
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
        while(dataTableDocumentIds.hasNext() && next.size() < batchSize)
          next.add(dataTableDocumentIds.next());
        // totalDocumentCount could be set to the Limit parameter. Thus we
        // should stop when we reach the limit. and not set hasNext back to
        // true.
        if (processedDocuments < totalDocumentCount - 1)
            hasNext = dataTableDocumentIds.hasNext();
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

    public void close() {
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
                ids = dbc.retrieveAndMark(tableName, getClass().getSimpleName(), hostName, pid, limit, selectionOrder);
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
                e.printStackTrace();
            }
            return null;
        }
    }

}
