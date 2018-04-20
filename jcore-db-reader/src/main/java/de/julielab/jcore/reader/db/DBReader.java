/**
 * DBReader.java
 * <p>
 * Copyright (c) 2008, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are protected. Please contact JULIE Lab for further information.
 * <p>
 * Author: landefeld
 * <p>
 * Current version: 0.0.1
 * Since version:   0.0.1
 * <p>
 * Creation date: 20.10.2008
 * <p>
 * Base class for collection readers which use the database driven jules document management.
 * Parameters:
 * <ul>
 * <li>DBDriver: database driver name </li>
 * <li>DBUrl: database url</li>
 * <li>DBUser: database user </li>
 * <li>DBPassword: database users password</li>
 * <li>BatchSize: batch size of retrieved documents (default 100)</li>
 * </ul>
 **/

package de.julielab.jcore.reader.db;

import de.julielab.jcore.types.ext.DBProcessingMetaData;
import de.julielab.xmlData.dataBase.DBCIterator;
import de.julielab.xmlData.dataBase.util.TableSchemaMismatchException;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base for UIMA collection readers using a (PostgreSQL) database to retrieve
 * their documents from.
 * <p>
 * The reader interacts with two tables: One 'subset' table listing the document
 * collection with one document per row. Additionally, each row contains fields
 * for information about current processing status of a document as well as
 * error status and processing host. This table will be locked while getting a
 * batch of documents to process, thus it furthermore serves as a
 * synchronization medium.
 * </p>
 * <p>
 * The second table holds the actual data, thus we say 'data table'. The subset
 * table has to define foreign keys to the data table. In this way, the reader
 * is able to determine from which table to retrieve the document data.
 * </p>
 * <p>
 * This data management is done by the julie-medline-manager package.
 * </p>
 * <p>
 * Please note that this class does not implement
 * {@link #getNext(org.apache.uima.cas.CAS)}. Instead,
 * {@link #getNextArtifactData} is offered to expose the documents read from the
 * database. Until this point, no assumption about the document's structure has
 * been made. That is, we don't care in this class whether we deal with Medline
 * abstracts, plain texts, some HTML documents or whatever. Translating these
 * documents into a CAS with respect to a particular type system is delegated to
 * the extending class.
 * </p>
 *
 * @author landefeld/hellrich/faessler
 */
public abstract class DBReader extends DBSubsetReader {


    private final static Logger log = LoggerFactory.getLogger(DBReader.class);


    // Internal state fields
    private RetrievingThread retriever;

    private DBCIterator<byte[][]> xmlBytes;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        if (readDataTable && hasNext) {
            xmlBytes = dbc.queryDataTable(tableName, whereCondition);
        }
    }


//     super.initialize();
//
//    hostName = getHostName();
//    pid = getPID();
//
//    driver = (String) getConfigParameterValue(PARAM_DB_DRIVER);
//        if (driver == null)
//    driver = "org.postgresql.Driver";
//    Integer batchSize = (Integer) getConfigParameterValue(PARAM_BATCH_SIZE);
//    tableName = (String) getConfigParameterValue(PARAM_TABLE);
//    additionalTableNames = (String[]) getConfigParameterValue(PARAM_ADDITIONAL_TABLES);
//    additionalTableSchemas = (String) getConfigParameterValue(PARAM_ADDITIONAL_TABLE_SCHEMA);
//    dataTimestamp = (String) getConfigParameterValue(PARAM_DATA_TIMESTAMP);
//    selectionOrder = (String) getConfigParameterValue(PARAM_SELECTION_ORDER);
//    Boolean fetchIdsProactively = (Boolean) getConfigParameterValue(PARAM_FETCH_IDS_PROACTIVELY);
//    whereCondition = (String) getConfigParameterValue(PARAM_WHERE_CONDITION);
//    limitParameter = (Integer) getConfigParameterValue(PARAM_LIMIT);
//    resetTable = (Boolean) getConfigParameterValue(PARAM_RESET_TABLE);
//        if (batchSize == null)
//    batchSize = Integer.parseInt(DEFAULT_BATCH_SIZE);
//        this.batchSize = batchSize;
//        if (fetchIdsProactively == null)
//    fetchIdsProactively = true;
//        this.fetchIdsProactively = fetchIdsProactively;
//        if (resetTable == null)
//    resetTable = false;
//    costosysConfig = (String) getConfigParameterValue(PARAM_COSTOSYS_CONFIG_NAME);
//
//    checkParameters();
//
//    InputStream is = null;
//    is = getClass().getResourceAsStream(costosysConfig.startsWith("/") ? costosysConfig : "/" + costosysConfig);
//        if (is == null && costosysConfig != null && costosysConfig.length() > 0) {
//        try {
//            is = new FileInputStream(costosysConfig);
//        } catch (FileNotFoundException e) {
//            log.error("File '{}' was not found.", costosysConfig);
//            throw new ResourceInitializationException(e);
//        }
//    }
//
//    dbc = new DataBaseConnector(is, batchSize);
//
//    // Check whether the table we are supposed to read from actually exists.
//        if (!dbc.tableExists(tableName)) {
//        throw new ResourceInitializationException(
//                new IllegalArgumentException("The configured table \"" + tableName + "\" does not exist."));
//    }
//
//    // Check whether a subset table name or a data table name was given.
//        if (dbc.getReferencedTable(tableName) == null) {
//        if (additionalTableNames != null)
//            throw new NotImplementedException("At the moment mutiple tables can only be joined"
//                    + " if the data table is referenced by a subset, for which the name has to be"
//                    + " given in the Table parameter.");
//        dbc.checkTableDefinition(tableName);
//        readDataTable = true;
//        xmlBytes = dbc.queryDataTable(tableName, whereCondition);
//        hasNext = xmlBytes.hasNext();
//        Integer tableRows = dbc.countRowsOfDataTable(tableName, whereCondition);
//        totalDocumentCount = limitParameter != null ? Math.min(tableRows, limitParameter) : tableRows;
//    } else {
//        if (batchSize == 0)
//            log.warn("Batch size of retrieved documents is set to 0. Nothing will be returned.");
//        if (resetTable)
//            dbc.resetSubset(tableName);
//
//        dbc.checkTableSchemaCompatibility(dbc.getActiveTableSchema(), additionalTableSchemas);
//
//        Integer unprocessedDocs = unprocessedDocumentCount();
//        totalDocumentCount = limitParameter != null ? Math.min(unprocessedDocs, limitParameter) : unprocessedDocs;
//        dataTable = dbc.getReferencedTable(tableName);
//        hasNext = dbc.hasUnfetchedRows(tableName);
//
//        if (additionalTableNames != null && additionalTableNames.length > 0) {
//            joinTables = true;
//
//            numAdditionalTables = additionalTableNames.length;
//            checkAndAdjustAdditionalTables();
//
//            schemas = new String[numAdditionalTables + 1];
//            schemas[0] = dbc.getActiveTableSchema();
//            for (int i = 1; i < schemas.length; i++) {
//                schemas[i] = additionalTableSchemas;
//            }
//        } else {
//            numAdditionalTables = 0;
//        }
//    }
//    logConfigurationState();







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
    public byte[][] getNextArtifactData() throws CollectionException {
log.trace("Fetching next document from the current database batch");

        byte[][] next = null;
        if (readDataTable)
            next = getNextFromDataTable();
        else
            next = getNextFromSubset();

        if (next != null)
            ++processedDocuments;

        return next;
    }

    private byte[][] getNextFromDataTable() {
        byte[][] next = null;
        // Must be set to true again if the iterator has more elements.
        hasNext = false;
        next = xmlBytes.next();
        // totalDocumentCount could be set to the Limit parameter. Thus we
        // should stop when we reach the limit. and not set hasNext back to
        // true.
        if (processedDocuments < totalDocumentCount - 1)
            hasNext = xmlBytes.hasNext();
        return next;
    }

    private byte[][] getNextFromSubset() {
        log.trace("Reading in subset table mode.");
        byte[][] next = null;

        // When this method is called for the first time, no retriever thread
        // will yet exist. Initialize it.
        if (retriever == null) {
            log.trace("Creating new RetrievingThread for fetching the first document batch");
            retriever = new RetrievingThread();
            xmlBytes = retriever.getDocuments();
            if (fetchIdsProactively) {
                log.trace("Creating background RetrievingThread to immediately fetch the next document batch");
                retriever = new RetrievingThread();
            }
        }

        if (xmlBytes.hasNext()) {
            log.debug("Returning next document.");
            next = xmlBytes.next();
        }
        if (!xmlBytes.hasNext()) { // Don't merge with
            // the if above, the
            // check must be executed despite lazy evaluation
            xmlBytes = retriever.getDocuments();
            if (!xmlBytes.hasNext()) {
                log.debug("No more documents, settings 'hasNext' to false.");
                hasNext = false;
            } else if (fetchIdsProactively) {
                log.trace("Creating background RetrievingThread to immediately fetch the next document batch");
                retriever = new RetrievingThread();
            }
        }
        return next;
    }

    protected int unprocessedDocumentCount() {
        int unprocessed = -1;
        if (readDataTable) {
            unprocessed = totalDocumentCount - processedDocuments;
        } else
            unprocessed = dbc.countUnprocessed(tableName);
        return unprocessed;
    }

//    protected void throwCollectionException(CollectionException e) throws CollectionException {
//        throw e;
//    }

    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(processedDocuments, totalDocumentCount, Progress.ENTITIES, true)};
    }

//    public String getPID() {
//        String id = ManagementFactory.getRuntimeMXBean().getName();
//        return id.substring(0, id.indexOf('@'));
//    }
//
//    public String getHostName() {
//        InetAddress address;
//        String hostName;
//        try {
//            address = InetAddress.getLocalHost();
//            hostName = address.getHostName();
//        } catch (UnknownHostException e) {
//            throw new IllegalStateException(e);
//        }
//        return hostName;
//    }

    public void close(){
        if (xmlBytes != null)
            xmlBytes.close();
        dbc.close();
        dbc = null;
    }


    protected String setDBProcessingMetaData(byte[][] data, CAS cas) throws CollectionException {
        String pkString = null;
        try {
            // remove previously added dbMetaData
            JCasUtil.select(cas.getJCas(), DBProcessingMetaData.class).forEach(x -> x.removeFromIndexes());

            DBProcessingMetaData dbMetaData = new DBProcessingMetaData(cas.getJCas());
            List<Integer> pkIndices = dbc.getPrimaryKeyIndices();
            StringArray pkArray = new StringArray(cas.getJCas(), pkIndices.size());
            for (int i = 0; i < pkIndices.size(); ++i) {
                Integer index = pkIndices.get(i);
                String pkElementValue = new String(data[index], Charset.forName("UTF-8"));
                pkArray.set(i, pkElementValue);
            }
            if (log.isDebugEnabled())
                log.debug("Setting primary key to {}", Arrays.toString(pkArray.toArray()));
            dbMetaData.setPrimaryKey(pkArray);

            if (!readDataTable)
                dbMetaData.setSubsetTable(
                        tableName.contains(".") ? tableName : dbc.getActivePGSchema() + "." + tableName);

            dbMetaData.addToIndexes();
            return pkString;
        } catch (CASException e) {
            throw new CollectionException(e);
        }
    }

    /**
     * @return The component name of the reader to fill in the subset table's
     * pipeline status field
     */
    protected abstract String getReaderComponentName();

    /**
     * <p>
     * This class is charged with retrieving batches of document IDs and documents while previously fetched documents
     * are in process.
     * </p>
     * <p>
     * The class manages the <code>FetchIdsProactively</code> parameter which
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
        private DBCIterator<byte[][]> documents;

        public RetrievingThread() {
            // Only fetch ID batches in advance when the parameter is set to
            // true.
            if (fetchIdsProactively) {
                log.debug("Fetching new documents in a background thread.");
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
                ids = dbc.retrieveAndMark(tableName, getReaderComponentName(), hostName, pid, limit, selectionOrder);
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
            } catch (TableSchemaMismatchException e) {
                log.error("Table schema mismatch: The active table schema {} specified in the CoStoSys configuration" +
                                " file {} does not match the columns in the subset table {}: {}", dbc.getActiveTableSchema(),
                        costosysConfig, tableName, e.getMessage());
                throw new IllegalArgumentException(e);
            }
            numberFetchedDocIDs += ids.size();
            log.debug("Retrieved {} document IDs to fetch from the database.", ids.size());

            if (ids.size() > 0) {
                log.debug("Fetching {} documents from the database.", ids.size());
                if (dataTimestamp == null) {
                    if (!joinTables) {
                        documents = dbc.queryIDAndXML(ids, dataTable);
                    } else {
                        documents = dbc.queryIDAndXML(ids, tables, schemas);
                    }
                } else
                    documents = dbc.queryWithTime(ids, dataTable, dataTimestamp);
            } else {
                log.debug("No unfetched documents left.");
                // Return empty iterator to avoid NPE.
                documents = new DBCIterator<byte[][]>() {

                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public byte[][] next() {
                        return null;
                    }

                    @Override
                    public void remove() {
                    }

                    @Override
                    public void close() {
                    }
                };
            }
        }

        public DBCIterator<byte[][]> getDocuments() {
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
                return documents;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
