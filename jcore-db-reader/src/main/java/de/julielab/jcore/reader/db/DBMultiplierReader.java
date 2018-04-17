package de.julielab.jcore.reader.db;

import de.julielab.jcore.types.casmultiplier.DocumentIds;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBMultiplierReader extends DBReaderBase {
    private final static Logger log = LoggerFactory.getLogger(DBMultiplierReader.class);

    // Internal state fields
    private DBMultiplierReader.RetrievingThread retriever;
    private String[] schemas;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize();


        // Check whether a subset table name or a data table name was given.
        if (readDataTable) {
            throw new ResourceInitializationException(new IllegalArgumentException("The parameter " + PARAM_TABLE + " is " +
                    "set to " + tableName + " which is a data table. Reading from data tables is " +
                    "currently not supported."));
        }
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        List<Object[]> idList = getNextDocumentIdBatch();
        DocumentIds documentIds = new DocumentIds(jCas);
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
        documentIds.setIdentifiers(ids);
        documentIds.addToIndexes();
    }


    /**
     * This method checks whether the required parameters are set to meaningful
     * values and throws an IllegalArgumentException when not.
     *
     * @throws ResourceInitializationException
     */
    private void checkParameters() throws ResourceInitializationException {
        if (tableName == null || tableName.length() == 0) {
            throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new Object[]{PARAM_TABLE});
        }
        if (dbcConfig == null || dbcConfig.length() == 0) {
            throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new Object[]{PARAM_COSTOSYS_CONFIG_NAME});
        }
    }

    /*
     * If you overwrite this method you have to call super.hasNext().
     *
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
     */
    public boolean hasNext() throws IOException, CollectionException {
        return hasNext;
    }

    /**
     * Returns the next byte[][] containing a byte[] for the pmid at [0] and a
     * byte[] for the XML at [1] or null if there are no unprocessed documents left.
     *
     * @return Document document - the document
     * @throws CollectionException
     */
    public List<Object[]> getNextDocumentIdBatch() throws CollectionException {

        List<Object[]> next = null;
        if (readDataTable)
            next = getNextFromDataTable();
        else
            next = getNextFromSubset();

        if (next != null)
            ++processedDocuments;

        return next;
    }

    private List<Object[]> getNextFromDataTable() {
//        byte[][] next = null;
//        // Must be set to true again if the iterator has more elements.
//        hasNext = false;
//        next = xmlBytes.next();
//        // totalDocumentCount could be set to the Limit parameter. Thus we
//        // should stop when we reach the limit. and not set hasNext back to
//        // true.
//        if (processedDocuments < totalDocumentCount - 1)
//            hasNext = xmlBytes.hasNext();
//        return next;
        // TODO support this
        return null;
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

    protected int unprocessedDocumentCount() {
        int unprocessed = -1;
        if (readDataTable) {
            unprocessed = totalDocumentCount - processedDocuments;
        } else
            unprocessed = dbc.countUnprocessed(tableName);
        return unprocessed;
    }

    protected void throwCollectionException(CollectionException e) throws CollectionException {
        throw e;
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
            ids = dbc.retrieveAndMark(tableName, getClass().getSimpleName(), hostName, pid, limit, selectionOrder);
            if (log.isTraceEnabled()) {
                List<String> idStrings = new ArrayList<>();
                for (Object[] o : ids) {
                    List<String> pkElements = new ArrayList<>();
                    for (int i = 0; i < o.length; i++) {
                        Object object = o[i];
                        pkElements.add(String.valueOf(object));
                    }
                    idStrings.add(StringUtils.join(pkElements, "-"));
                }
                log.trace("Reserved the following document IDs for processing: " + idStrings);
            }
            numberFetchedDocIDs += ids.size();
            log.debug("Retrieved {} document IDs to fetch from the database.", ids.size());
        }

        public List<Object[]> getDocumentIds() {
            // If we don't use this as a background thread, we have to get the
            // IDs now in a classic sequential manner.
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
