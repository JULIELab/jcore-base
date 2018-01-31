/** 
 * DBReader.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are protected. Please contact JULIE Lab for further information.  
 *
 * Author: landefeld
 * 
 * Current version: 0.0.1
 * Since version:   0.0.1
 *
 * Creation date: 20.10.2008 
 * 
 * Base class for collection readers which use the database driven jules document management.
 * Parameters:
 * <ul>
 *   	<li>DBDriver: database driver name </li>
 *		<li>DBUrl: database url</li>
 *		<li>DBUser: database user </li>
 *		<li>DBPassword: database users password</li>
 *		<li>BatchSize: batch size of retrieved documents (default 100)</li>
 * </ul>
 **/

package de.julielab.jcore.reader;

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
 * {@link #getNextArrayArray()} is offered to expose the documents read from the
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
public abstract class DBReader extends CollectionReader_ImplBase {

	private static final Logger log = LoggerFactory.getLogger(DBReader.class);

	public static final String PARAM_DB_DRIVER = "DBDriver";
	public static final String PARAM_BATCH_SIZE = "BatchSize";
	/**
	 * String parameter. Determines the table from which rows are read and
	 * returned. Both subset and data tables are allowed. For data tables, an
	 * optional 'where' condition can be specified, restricting the rows to be
	 * returned. Note that only reading from subset tables works correctly for
	 * concurrent access of multiple readers (for data tables each reader will
	 * return the whole table).
	 */
	public static final String PARAM_TABLE = "Table";
	/**
	 * String parameter representing a long value. If not null, only documents
	 * with a timestamp newer then the passed value will be processed.
	 */
	public static final String PARAM_TIMESTAMP = "Timestamp";
	/**
	 * Boolean parameter. Determines whether to return random samples of
	 * unprocessed documents rather than proceeding sequentially. This parameter
	 * is defined for subset reading only.
	 */
	public static final String PARAM_SELECTION_ORDER = "RandomSelection";
	/**
	 * Boolean parameter. Determines whether a background thread should be used
	 * which fetches the next batch of document IDs to process while the former
	 * batch is already being processed. Using the background thread boosts
	 * performance as waiting time is minimized. However, as the next batch of
	 * documents is marked in advance as being in process, this approach is only
	 * suitable when reading all available data.
	 */
	public static final String PARAM_FETCH_IDS_PROACTIVELY = "FetchIdsProactively";
	/**
	 * String parameter. Used only when reading directly from data tables. Only
	 * rows are returned which satisfy the specified 'where' clause. If empty or
	 * set to <code>null</code>, all rows are returned.
	 */
	public static final String PARAM_WHERE_CONDITION = "WhereCondition";
	/**
	 * Integer parameter. Determines the maximum amount of documents being read
	 * by this reader. The reader will also not mark more documents to be in
	 * process as specified with this parameter.
	 */
	public static final String PARAM_LIMIT = "Limit";

	/**
	 * Constant denoting the name of the external dependency representing the
	 * configuration file for the DataBaseConnector.<br>
	 * The name of the resource is assured by convention only as alternative
	 * names are not reject from the descriptor when entering them manually.
	 */
	public static final String PARAM_DBC_CONFIG_NAME = "dbcConfigFile";
	/**
	 * Boolean parameter. Indicates whether the read subset table is to be reset
	 * before reading.
	 */
	public static final String PARAM_RESET_TABLE = "ResetTable";
	/**
	 * Multi-valued String parameter indicating which tables will be read from
	 * additionally to the referenced data table. The tables will be joined to a
	 * single CAS.
	 */
	public static final String PARAM_ADDITIONAL_TABLES = "AdditionalTables";
	/**
	 * Multi-valued String parameter indicating different schemas in case tables
	 * will be joined. The schema for the referenced data table has to be the
	 * first element. The schema for the additional tables has to be the second
	 * element.
	 */
	public static final String PARAM_ADDITIONAL_TABLE_SCHEMA = "AdditionalTableSchema";

	private static final Logger LOG = LoggerFactory.getLogger(DBReader.class);

	/**
	 * Default size of document batches fetched from the database. The default
	 * is {@value #DEFAULT_BATCH_SIZE}.
	 */
	private static final String DEFAULT_BATCH_SIZE = "50";

	@ConfigurationParameter(name = PARAM_BATCH_SIZE, defaultValue = DEFAULT_BATCH_SIZE)
	protected int batchSize;
	/**
	 * Currently unused because the Hikari JDBC library should recognize the
	 * correct driver. However, there seem to be cases where this doesn't work
	 * (HSQLDB). So we keep the parameter for later. When this issue comes up,
	 * the driver would have to be set manually. This isn't done right now.
	 */
	@ConfigurationParameter(name = PARAM_DB_DRIVER)
	protected String driver;
	private String hostName;
	private String pid;
	@ConfigurationParameter(name = PARAM_TIMESTAMP)
	private String timestamp;
	private int totalDocumentCount;
	private int processedDocuments = 0;
	private volatile int numberFetchedDocIDs = 0;

	@ConfigurationParameter(name = PARAM_TABLE, mandatory = true)
	protected String tableName;
	@ConfigurationParameter(name = PARAM_ADDITIONAL_TABLES)
	protected String[] additionalTableNames;
	@ConfigurationParameter(name = PARAM_ADDITIONAL_TABLE_SCHEMA)
	protected String additionalTableSchema;
	protected String[] tables;
	private String[] schemas;
	protected boolean joinTables = false;
	protected int numAdditionalTables;

	protected DataBaseConnector dbc;
	protected boolean hasNext;

	protected DBCIterator<byte[][]> xmlBytes;

	// This variable block is only used when reading subsets.
	protected String dataTable;
	protected RetrievingThread retriever;
	@ConfigurationParameter(name = PARAM_SELECTION_ORDER, defaultValue = "")
	protected String selectionOrder;
	@ConfigurationParameter(name = PARAM_FETCH_IDS_PROACTIVELY, defaultValue = "true")
	protected Boolean fetchIdsProactively;
	@ConfigurationParameter(name = PARAM_WHERE_CONDITION)
	protected String whereCondition;
	@ConfigurationParameter(name = PARAM_LIMIT)
	protected Integer limitParameter;
	@ConfigurationParameter(name = PARAM_DBC_CONFIG_NAME, mandatory = true)
	String dbcConfig;
	@ConfigurationParameter(name = PARAM_RESET_TABLE, defaultValue = "false")
	private Boolean resetTable;

	protected Boolean readDataTable = false;

	@Override
	public void initialize() throws ResourceInitializationException {
		super.initialize();

		hostName = getHostName();
		pid = getPID();

		driver = (String) getConfigParameterValue(PARAM_DB_DRIVER);
		if (driver == null)
			driver = "org.postgresql.Driver";
		Integer batchSize = (Integer) getConfigParameterValue(PARAM_BATCH_SIZE);
		// TODO Error when table not given (due to wrong written parameter name
		// for instance)
		tableName = (String) getConfigParameterValue(PARAM_TABLE);
		additionalTableNames = (String[]) getConfigParameterValue(PARAM_ADDITIONAL_TABLES);
		additionalTableSchema = (String) getConfigParameterValue(PARAM_ADDITIONAL_TABLE_SCHEMA);
		timestamp = (String) getConfigParameterValue(PARAM_TIMESTAMP);
		selectionOrder = (String) getConfigParameterValue(PARAM_SELECTION_ORDER);
		Boolean fetchIdsProactively = (Boolean) getConfigParameterValue(PARAM_FETCH_IDS_PROACTIVELY);
		whereCondition = (String) getConfigParameterValue(PARAM_WHERE_CONDITION);
		limitParameter = (Integer) getConfigParameterValue(PARAM_LIMIT);
		resetTable = (Boolean) getConfigParameterValue(PARAM_RESET_TABLE);
		if (batchSize == null)
			batchSize = Integer.parseInt(DEFAULT_BATCH_SIZE);
		this.batchSize = batchSize;
		if (fetchIdsProactively == null)
			fetchIdsProactively = true;
		this.fetchIdsProactively = fetchIdsProactively;
		if (resetTable == null)
			resetTable = false;
		dbcConfig = (String) getConfigParameterValue(PARAM_DBC_CONFIG_NAME);

		checkParameters();

		InputStream is = null;
		is = getClass().getResourceAsStream(dbcConfig.startsWith("/") ? dbcConfig : "/" + dbcConfig);
		if (is == null && dbcConfig != null && dbcConfig.length() > 0) {
			try {
				is = new FileInputStream(dbcConfig);
			} catch (FileNotFoundException e) {
				LOG.error("File '{}' was not found.", dbcConfig);
				throw new ResourceInitializationException(e);
			}
		}
		// if (is == null)
		// LOG.warn("No DataBaseConfiguration was found, will use default ->
		// prepare for errors!");

		dbc = new DataBaseConnector(is, batchSize);

		// Check whether the table we are supposed to read from actually exists.
		if (!dbc.tableExists(tableName)) {
			throw new ResourceInitializationException(
					new IllegalArgumentException("The configured table \"" + tableName + "\" does not exist."));
		}

		// Check whether a subset table name or a data table name was given.
		if (dbc.getReferencedTable(tableName) == null) {
			if (additionalTableNames != null)
				throw new NotImplementedException("At the moment mutiple tables can only be joined"
						+ " if the data table is referenced by a subset, for which the name has to be"
						+ " given in the Table parameter.");
			dbc.checkTableDefinition(tableName);
			readDataTable = true;
			xmlBytes = dbc.queryDataTable(tableName, whereCondition);
			hasNext = xmlBytes.hasNext();
			Integer tableRows = dbc.countRowsOfDataTable(tableName, whereCondition);
			totalDocumentCount = limitParameter != null ? Math.min(tableRows, limitParameter) : tableRows;
		} else {
			if (batchSize == 0)
				LOG.warn("Batch size of retrieved documents is set to 0. Nothing will be returned.");
			if (resetTable)
				dbc.resetSubset(tableName);

			dbc.checkTableSchemaCompatibility(dbc.getActiveTableSchema(), additionalTableSchema);

			Integer unprocessedDocs = unprocessedDocumentCount();
			totalDocumentCount = limitParameter != null ? Math.min(unprocessedDocs, limitParameter) : unprocessedDocs;
			dataTable = dbc.getReferencedTable(tableName);
			hasNext = dbc.hasUnfetchedRows(tableName);

			if (additionalTableNames != null && additionalTableNames.length > 0) {
				joinTables = true;

				numAdditionalTables = additionalTableNames.length;
				checkAndAdjustAdditionalTables();

				schemas = new String[numAdditionalTables + 1];
				// schemas[0] = additionalTableSchemas[0];
				schemas[0] = dbc.getActiveTableSchema();
				for (int i = 1; i < schemas.length; i++) {
					schemas[i] = additionalTableSchema;
				}
			} else {
				numAdditionalTables = 0;
			}
		}
		logConfigurationState();
	}

	private void logConfigurationState() {
		LOG.info("TableName is: \"{}\"; referenced data table name is: \"{}\"", tableName, dataTable);
		LOG.info("Names of additional tables to join: {}", StringUtils.join(additionalTableNames, ", "));
		LOG.info("BatchSize is set to {}.", batchSize);
		LOG.info("Subset table {} will be reset upon pipeline start: {}", tableName, resetTable);
	}

	/**
	 * Checks whether the given additional tables exist. If not, it is checked
	 * if the table names contain dots which are reserved for schema
	 * qualification in Postgres. It is tried again to find the tables with
	 * underscores ('_'), then. The tables are also searched in the data schema.
	 * When the names contain dots, the substring up to the first dot is tried
	 * as schema qualification before prepending the data schema.
	 */
	private void checkAndAdjustAdditionalTables() {
		List<String> foundTables = new ArrayList<String>();
		foundTables.add(dataTable);
		// tables = new String[numAdditionalTables + 1];
		// tables[0] = dataTable;
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
				LOG.warn("The table " + additionalTableNames[i] + " does not exist!");
				// Use the original given name so that the user get's an
				// understandable error message and nothing about table names
				// being null.
				// tables[i + 1] = additionalTableNames[i];
			} else
				// tables[i + 1] = resultTableName;
				foundTables.add(resultTableName);
		}
		tables = foundTables.toArray(new String[foundTables.size()]);
		// -1 because here we also have added the document table which is not an
		// additional table but the main table!
		numAdditionalTables = tables.length - 1;
	}

	/**
	 * This method checks whether the required parameters are set to meaningful
	 * values and throws an IllegalArgumentException when not. TODO: I guess it
	 * should rather be "ResourceInitializationException"...
	 */
	private void checkParameters() {
		if (tableName == null || tableName.length() == 0) {
			throw new IllegalArgumentException("Parameter '" + PARAM_TABLE + "' is not specified.");
		}
		if (dbcConfig == null || dbcConfig.length() == 0) {
			throw new IllegalArgumentException("Parameter '" + PARAM_DBC_CONFIG_NAME + "' is not specified.");
		}
		if (additionalTableNames != null & additionalTableSchema == null) {
			throw new IllegalArgumentException("If multiple tables will be joined"
					+ " the table schema for the additional tables (besides the base document table which should be configured using the database connector configuration) must be specified.");
		}
	}

	/**
	 * <p>
	 * This class is charged to retrieve batches of document IDs which will be
	 * returned for processing afterwards. Note that this thread only fetches
	 * document IDs, not documents themselves. This is done in
	 * {@link DBReader#getNextArrayArray()}.
	 * </p>
	 * <p>
	 * The class manages itself the <code>FetchIdsProactively</code> parameter
	 * which can be given to the reader. When set to <code>false</code>, no ID
	 * batches are fetched in advance but are fetched exactly on demand in
	 * {@link DBReader#getNextArrayArray()}.
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
				LOG.debug("Fetching new documents in a background thread.");
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
			if (LOG.isTraceEnabled()) {
				List<String> idStrings = new ArrayList<>();
				for (Object[] o : ids) {
					List<String> pkElements = new ArrayList<>();
					for (int i = 0; i < o.length; i++) {
						Object object = o[i];
						pkElements.add(String.valueOf(object));
					}
					idStrings.add(StringUtils.join(pkElements, "-"));
				}
				LOG.trace("Reserved the following document IDs for processing: " + idStrings);
			}
			numberFetchedDocIDs += ids.size();
			LOG.debug("Retrieved {} document IDs to fetch from the database.", ids.size());

			if (ids.size() > 0) {
				LOG.debug("Fetching {} documents from the database.", ids.size());
				if (timestamp == null) {
					if (!joinTables) {
						documents = dbc.queryIDAndXML(ids, dataTable);
					} else {
						documents = dbc.queryIDAndXML(ids, tables, schemas);
					}
				} else
					documents = dbc.queryWithTime(ids, dataTable, timestamp);
			} else {
				LOG.debug("No unfetched documents left.");
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
				LOG.debug("Fetching new documents (without employing a background thread).");
				run();
			}
			try {
				// If this is a background thread started with start(): Wait for
				// the IDs to be retrieved, i.e. that run() ends.
				LOG.debug("Waiting for the background thread to finish fetching documents to return them.");
				join();
				return documents;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
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
	 * byte[] for the XML at [1] or null if there are no unprocessed documents
	 * left.
	 * 
	 * @return Document document - the document
	 * @throws CollectionException
	 */
	public byte[][] getNextArtefactData(CAS aCAS) throws CollectionException {

		byte[][] next = null;
		if (readDataTable)
			next = getNextFromDataTable();
		else
			next = getNextFromSubset();

		if (next != null)
			++processedDocuments;

		return next;
	}

	/**
	 * @return
	 */
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

	/**
	 * @param next
	 * @return
	 */
	private byte[][] getNextFromSubset() {
		byte[][] next = null;

		// When this method is called for the first time, no retriever thread
		// will yet exist. Initialize it.
		if (retriever == null) {
			retriever = new RetrievingThread();
			// if (!joinTables) {
			// xmlBytes = dbc.queryIDAndXML(retriever.getIds(), dataTable);
			// } else {
			// // Join multiple tables.
			// xmlBytes = dbc.queryIDAndXML(retriever.getIds(), tables,
			// schemas);
			// }
			xmlBytes = retriever.getDocuments();
			if (fetchIdsProactively)
				retriever = new RetrievingThread();
		}

		// Can happen when not using proactive fetching of document IDs (set
		// in this method at the bottom).
		// Means we will fetch the documents exactly when requested, not before.
		// if (xmlBytes == null) {
		// fetchDocumentBatch();
		// }

		if (xmlBytes.hasNext()) {
			LOG.debug("Returning next document.");
			next = xmlBytes.next();
		}
		if (!xmlBytes.hasNext()) { // Don't merge with
									// the if above, the
									// check
			// has to happen after xmlBytes.next()
			// fetchDocumentBatch();
			xmlBytes = retriever.getDocuments();
			if (!xmlBytes.hasNext()) {
				LOG.debug("No more documents, settings 'hasNext' to false.");
				hasNext = false;
			} else if (fetchIdsProactively) {
				LOG.debug("Creating new background thread.");
				retriever = new RetrievingThread();
			}
		}
		// else if (!xmlBytes.hasNext()) {
		// // NOTE: This can cause problems when reading with multiple readers
		// // from one table: The reader returns true for hasNext() because
		// // there were unprocessed documents left in the table. But since the
		// // read does not fetch these documents itself, another reader could
		// // fetch them while this reader waits for the next call of this
		// // method (only then new documents are fetched). By then it would be
		// // possible that there are no documents left to read and a
		// // NullPointerException could happen.
		// hasNext = dbc.hasUnfetchedRows(tableName);
		// xmlBytes = null;
		// }
		return next;
	}

	/**
	 * 
	 */
	// private void fetchDocumentBatch() {
	// List<Object[]> ids = retriever.getIds();
	// if (ids.isEmpty()) {
	// hasNext = false;
	// } else {
	// retriever = new RetrievingThread();
	// if (timestamp == null) {
	// if (!joinTables) {
	// xmlBytes = dbc.queryIDAndXML(ids, dataTable);
	// } else {
	// xmlBytes = dbc.queryIDAndXML(ids, tables, schemas);
	// }
	// } else
	// xmlBytes = dbc.queryWithTime(ids, dataTable, timestamp);
	// }
	// }

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
		return new Progress[] { new ProgressImpl(processedDocuments, totalDocumentCount, Progress.ENTITIES, true) };
	}

	public String getPID() {
		String id = ManagementFactory.getRuntimeMXBean().getName();
		return id.substring(0, id.indexOf("@"));
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
				dbMetaData.setSubsetTable(tableName.contains(".") ? tableName : dbc.getActivePGSchema() + "." + tableName);

			dbMetaData.addToIndexes();
			return pkString;
		} catch (CASException e) {
			throw new CollectionException(e);
		}
	}
	
	/**
	 * 
	 * @return The component name of the reader to fill in the subset table's pipeline status field
	 */
	protected abstract String getReaderComponentName();

}
