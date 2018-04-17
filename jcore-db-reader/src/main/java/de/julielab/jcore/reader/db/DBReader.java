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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.ext.DBProcessingMetaData;
import de.julielab.xmlData.dataBase.DBCIterator;
import de.julielab.xmlData.dataBase.DataBaseConnector;

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
 *
 */
public abstract class DBReader extends DBReaderBase {

    private final static Logger log = LoggerFactory.getLogger(DBReader.class);


    // Internal state fields
    private RetrievingThread retriever;
    private String[] schemas;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize();


        // Check whether a subset table name or a data table name was given.
        if (readDataTable) {
            log.debug("Fetching first batch of data table entries." );
            xmlBytes = dbc.queryDataTable(tableName, whereCondition);
            hasNext = xmlBytes.hasNext();
        } else {
            if (additionalTableNames != null && additionalTableNames.length > 0) {
                joinTables = true;

                numAdditionalTables = additionalTableNames.length;
                checkAndAdjustAdditionalTables();

                schemas = new String[numAdditionalTables + 1];
                schemas[0] = dbc.getActiveTableSchema();
                for (int i = 1; i < schemas.length; i++) {
                    schemas[i] = additionalTableSchema;
                }
            } else {
                numAdditionalTables = 0;
            }
        }
    }



    /**
     * Checks whether the given additional tables exist. If not, it is checked if
     * the table names contain dots which are reserved for schema qualification in
     * Postgres. It is tried again to find the tables with underscores ('_'), then.
     * The tables are also searched in the data schema. When the names contain dots,
     * the substring up to the first dot is tried as schema qualification before
     * prepending the data schema.
     */
    private void checkAndAdjustAdditionalTables() {
        List<String> foundTables = new ArrayList<String>();
        foundTables.add(dataTable);
        for (int i = 0; i < additionalTableNames.length; i++) {
            String resultTableName = null;
            if (dbc.tableExists(additionalTableNames[i]))
                resultTableName = additionalTableNames[i];
            // Try with default data postgres schema prepended.
            if (null == resultTableName) {
                String tn = dbc.getActiveDataPGSchema() + "." + additionalTableNames[i];
                if (dbc.tableExists(tn))
                    resultTableName = tn;
            }
            // Try with all dots converted to underscores but the first dot, so
            // that the substring up to the first dot could be a schema
            // qualification.
            if (null == resultTableName) {
                int dotIndex = additionalTableNames[i].indexOf('.');
                String prefix = additionalTableNames[i].substring(0, dotIndex);
                String rest = additionalTableNames[i].substring(dotIndex + 1);
                String tn = prefix + "." + rest.replaceAll("\\.", "_");
                if (dbc.tableExists(tn))
                    resultTableName = tn;
            }
            // Try with the original name but all dots converted to underscores.
            if (null == resultTableName) {
                String tn = additionalTableNames[i].replaceAll("\\.", "_");
                if (dbc.tableExists(tn))
                    resultTableName = tn;
            }
            // Try with all all dots converted to underscored and the active
            // data schema prepended.
            if (null == resultTableName) {
                String tn = dbc.getActiveDataPGSchema() + "." + additionalTableNames[i].replaceAll("\\.", "_");
                if (dbc.tableExists(tn))
                    resultTableName = tn;
            }
            // We have really tried...
            if (null == resultTableName) {
                log.warn("The table {} does not exist!", additionalTableNames[i]);
            } else
                foundTables.add(resultTableName);
        }
        tables = foundTables.toArray(new String[foundTables.size()]);
        // -1 because here we also have added the document table which is not an
        // additional table but the main table!
        numAdditionalTables = tables.length - 1;
    }

    /**
     * This method checks whether the required parameters are set to meaningful
     * values and throws an IllegalArgumentException when not.
     * @throws ResourceInitializationException
     */
    private void checkParameters() throws ResourceInitializationException {
        if (tableName == null || tableName.length() == 0) {
            throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new Object[]{PARAM_TABLE});
        }
        if (dbcConfig == null || dbcConfig.length() == 0) {
            throw new ResourceInitializationException(ResourceInitializationException.CONFIG_SETTING_ABSENT, new Object[]{PARAM_COSTOSYS_CONFIG_NAME});
        }
        if (additionalTableNames != null && additionalTableSchema == null) {
            throw new ResourceInitializationException(new IllegalArgumentException("If multiple tables will be joined"
                    + " the table schema for the additional tables (besides the base document table which should be configured using the database connector configuration) must be specified."));
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
    public byte[][] getNextArtifactData(CAS aCAS) throws CollectionException {

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
        byte[][] next = null;

        // When this method is called for the first time, no retriever thread
        // will yet exist. Initialize it.
        if (retriever == null) {
            retriever = new RetrievingThread();
            xmlBytes = retriever.getDocuments();
            if (fetchIdsProactively)
                retriever = new RetrievingThread();
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
                log.debug("Creating new background thread.");
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

    protected void throwCollectionException(CollectionException e) throws CollectionException {
        throw e;
    }

    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(processedDocuments, totalDocumentCount, Progress.ENTITIES, true)};
    }

    public String getPID() {
        String id = ManagementFactory.getRuntimeMXBean().getName();
        return id.substring(0, id.indexOf('@'));
    }

    public String getHostName() {
        InetAddress address;
        String hostName;
        try {
            address = InetAddress.getLocalHost();
            hostName = address.getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        return hostName;
    }

    public void close() throws IOException {
        if (xmlBytes != null)
            xmlBytes.close();
        dbc = null;
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getAllRetrievedColumns() {
        List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
        List<Object> numColumnsAndFields = dbc.getNumColumnsAndFields(joinTables, tables, schemas);
        for (int i = 1; i < numColumnsAndFields.size(); i++) {
            List<Map<String, Object>> retrievedSchemaFields = (List<Map<String, Object>>) numColumnsAndFields.get(i);
            for (Map<String, Object> field : retrievedSchemaFields)
                fields.add(field);
        }
        return fields;

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
     *
     * @return The component name of the reader to fill in the subset table's
     *         pipeline status field
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
     *
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
            numberFetchedDocIDs += ids.size();
            log.debug("Retrieved {} document IDs to fetch from the database.", ids.size());

            if (ids.size() > 0) {
                log.debug("Fetching {} documents from the database.", ids.size());
                if (timestamp == null) {
                    if (!joinTables) {
                        documents = dbc.queryIDAndXML(ids, dataTable);
                    } else {
                        documents = dbc.queryIDAndXML(ids, tables, schemas);
                    }
                } else
                    documents = dbc.queryWithTime(ids, dataTable, timestamp);
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
