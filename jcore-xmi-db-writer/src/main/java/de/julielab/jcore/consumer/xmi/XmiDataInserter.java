package de.julielab.jcore.consumer.xmi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.xml.JulieXMLConstants;
import de.julielab.xmlData.Constants;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.DataBaseConnector;

public class XmiDataInserter {

	private static final Logger log = LoggerFactory.getLogger(XmiDataInserter.class);

	private static final String FIELD_MAX_XMI_ID = "max_xmi_id";

	private Boolean updateMode;
	private String schemaDocument;
	private String schemaAnnotation;
	private Boolean storeAll;
	private String docTableName;
	private String effectiveDocTableName;
	private DataBaseConnector dbc;
	private List<String> annotationsToStore;
	private Boolean storeBaseDocument;
	private Map<String, Integer> maxXmiIdMap;
	private String componentDbName;

	private List<String> processedDocumentIds;

	public XmiDataInserter(List<String> annotationsToStore, String docTableName, String effectiveDocTableName,
			DataBaseConnector dbc, String schemaDocument, String schemaAnnotation, Boolean storeAll,
			Boolean storeBaseDocument, Boolean updateMode, String componentDbName) {
		super();
		this.annotationsToStore = annotationsToStore;
		this.docTableName = docTableName;
		this.effectiveDocTableName = effectiveDocTableName;
		this.dbc = dbc;
		this.schemaDocument = schemaDocument;
		this.schemaAnnotation = schemaAnnotation;
		this.storeAll = storeAll;
		this.storeBaseDocument = storeBaseDocument;
		this.updateMode = updateMode;
		this.componentDbName = componentDbName;
		this.maxXmiIdMap = new HashMap<String, Integer>();
		this.processedDocumentIds = new ArrayList<>();
	}

	/**
	 * Constructs row iterators for the different tables (document and
	 * annotations) conforming to the expectations of the DataBaseConnector API.
	 * If update mode is <code>true</code>, the CAS data will be added as an
	 * update. It will just be inserted otherwise (throwing an error if there
	 * will be a primary key constraint violation, i.e. duplicates).
	 * 
	 * @param updateMode
	 * @param tablesWithoutData
	 * @param serializedCASes
	 * @param schemaAnnotation
	 * @param schemaDocument
	 * @param storeAll
	 * @param effectiveDocTableName
	 * @throws XmiDataInsertionException
	 * 
	 * @throws AnalysisEngineProcessException
	 */
	public void sendXmiDataToDatabase(LinkedHashMap<String, List<XmiData>> serializedCASes,
			Map<String, List<String>> tablesWithoutData, String subsetTableName) throws XmiDataInsertionException {

		class RowIterator implements Iterator<Map<String, Object>> {

			private int index = 0;
			private List<XmiData> tableDataList;

			public RowIterator(String table) {
				tableDataList = serializedCASes.get(table);
			}

			@Override
			public boolean hasNext() {
				return index < tableDataList.size();
			}

			@Override
			public Map<String, Object> next() {
				Map<String, Object> row = new HashMap<String, Object>();
				XmiData results = tableDataList.get(index);

				// get the appropriate table schema: the document schema or
				// annotation schema
				FieldConfig fieldConfig = results.getClass().equals(DocumentXmiData.class)
						? dbc.getFieldConfiguration(schemaDocument) : dbc.getFieldConfiguration(schemaAnnotation);
				List<Map<String, String>> fields = fieldConfig.getFields();
				// this lambda says "give me the name of ith field of the
				// current field configuration"
				Function<Integer, String> fName = num -> fields.get(num).get(JulieXMLConstants.NAME);
				row.put(fName.apply(0), results.docId);
				row.put(fName.apply(1), results.data);
				log.trace("{}={}", fName.apply(0), results.docId);
				log.trace("{}={}", fName.apply(1), results.data);
				if (results.getClass().equals(DocumentXmiData.class)) {
					DocumentXmiData docResults = (DocumentXmiData) results;
					row.put(fName.apply(2), docResults.newXmiId);
					row.put(fName.apply(3), docResults.serializedSofaXmiIdMap);
					log.trace("{}={}", fName.apply(2), docResults.newXmiId);
					log.trace("{}={}", fName.apply(3), docResults.serializedSofaXmiIdMap);
				}

				index++;
				return row;
			}

			@Override
			public void remove() {
				throw new NotImplementedException();
			}
		}

		Connection conn = dbc.getConn();
		try {

			conn.setAutoCommit(false);
			for (String tableName : serializedCASes.keySet()) {
				if (serializedCASes.get(tableName).size() == 0) {
					log.trace("No XMI data for table \"" + tableName + "\" (annotation type \"" + tableName
							+ "\"), skipping.");
					continue;
				}

				RowIterator iterator = new RowIterator(tableName);
				try {
					if (updateMode) {
						log.debug("Updating {} XMI CAS data in database table '{}'.",
								serializedCASes.get(tableName).size(), tableName);
						if (storeAll) {
							dbc.updateFromRowIterator(iterator, tableName, conn, false, schemaDocument);
						} else {
							dbc.updateFromRowIterator(iterator, tableName, conn, false,
									tableName.equals(effectiveDocTableName) ? schemaDocument : schemaAnnotation);
						}
					} else {
						log.debug("Inserting {} XMI CAS data into database table '{}'.",
								serializedCASes.get(tableName).size(), tableName);
						if (storeAll) {
							dbc.importFromRowIterator(iterator, tableName, conn, false, schemaDocument);
						} else {
							dbc.importFromRowIterator(iterator, tableName, conn, false,
									tableName.equals(effectiveDocTableName) ? schemaDocument : schemaAnnotation);
						}
					}
				} catch (Exception e) {
					log.error("Error occurred while sending data to database. Exception:", e);
					throw new XmiDataInsertionException(e);
				}
			}
			updateMaxXmiId(conn);
			deleteRowsFromTablesWithoutData(tablesWithoutData, conn, dbc, annotationsToStore);
			setLastComponent(conn, subsetTableName);
			log.debug("Committing XMI data to database.");
			conn.commit();
		} catch (SQLException e) {
			log.error("Database error occurred while updating max-xmi-IDs: {}", e);
			e.printStackTrace();
			SQLException ne = e.getNextException();
			if (null != ne)
				ne.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Writes the component name to the database subset
	 * @param conn
	 * @throws XmiDataInsertionException 
	 */
	private void setLastComponent(Connection conn, String subsetTableName) throws XmiDataInsertionException {
		if (processedDocumentIds.isEmpty() || StringUtils.isBlank(subsetTableName))
			return;
		
		FieldConfig annotationFieldConfig = dbc.getFieldConfiguration(schemaAnnotation);
		String[] primaryKey = annotationFieldConfig.getPrimaryKey();
		if (primaryKey.length > 1)
			throw new IllegalArgumentException("Currently, only one-element primary keys are supported.");
		String primaryKeyString = annotationFieldConfig.getPrimaryKeyString();
		
		log.debug("Marking {} documents to having been processed by component \"{}\".", processedDocumentIds.size(), componentDbName);
		
		String sql = String.format("UPDATE %s SET %s='%s' WHERE %s=?", subsetTableName, Constants.LAST_COMPONENT, componentDbName, primaryKeyString);
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			for (String docId : processedDocumentIds)
			{
				ps.setString(1, docId);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			e.printStackTrace();
			SQLException nextException = e.getNextException();
			if (null == nextException)
				throw new XmiDataInsertionException(e);
			else
				nextException.printStackTrace();
			throw new XmiDataInsertionException(nextException);
		} finally {
			processedDocumentIds.clear();
		}
	}

	/**
	 * When performing updates, it might happen that an annotation which was
	 * present in the former version of a document isn't present in the new
	 * version. Then, we have a deprecated annotation and, more importantly, it
	 * might have the same xmi id as another annotation being written in another
	 * table. This collision can create documents where XMI elements reference
	 * wrong other XMI elements. Thus, where a Token should be, there is
	 * suddenly a ChunkADVP (this actually happened). This method should delete
	 * such deprecated annotations.
	 * 
	 * @param tablesWithoutData
	 * 
	 * @param conn
	 * @throws XmiDataInsertionException
	 * @throws AnalysisEngineProcessException
	 */

	private void deleteRowsFromTablesWithoutData(Map<String, List<String>> tablesWithoutData, Connection conn,
			DataBaseConnector dbc, List<String> annotationsToStore) throws XmiDataInsertionException {
		if (!updateMode || storeAll || annotationsToStore.isEmpty())
			return;

		FieldConfig annotationFieldConfig = dbc.getFieldConfiguration(schemaAnnotation);
		String[] primaryKey = annotationFieldConfig.getPrimaryKey();
		if (primaryKey.length > 1)
			throw new IllegalArgumentException("Currently, only one-element primary keys are supported.");
		String primaryKeyString = annotationFieldConfig.getPrimaryKeyString();

		for (Entry<String, List<String>> entry : tablesWithoutData.entrySet()) {
			List<String> pmids = entry.getValue();
			if (pmids.size() == 0)
				continue;

			String tableName = entry.getKey();
			String deleteString = "DELETE FROM " + tableName + " WHERE " + primaryKeyString + " = ?";

			try {
				PreparedStatement deleteRowWithoutData = conn.prepareStatement(deleteString);
				for (String pmid : pmids) {
					deleteRowWithoutData.setString(1, pmid);
					deleteRowWithoutData.addBatch();
				}
				deleteRowWithoutData.executeBatch();
			} catch (SQLException e) {
				e.printStackTrace();
				SQLException nextException = e.getNextException();
				if (null == nextException)
					throw new XmiDataInsertionException(e);
				else
					nextException.printStackTrace();
				throw new XmiDataInsertionException(nextException);
			} finally {
				pmids.clear();
			}
		}
	}

	/**
	 * Stores the next possible xmi id that can be assigned to new annotations
	 * in order to make sure that there aren't any clashes with already existing
	 * ids.
	 * 
	 * @throws XmiDataInsertionException
	 * 
	 * @throws AnalysisEngineProcessException
	 */
	public void updateMaxXmiId(Connection conn) throws XmiDataInsertionException {
		if (storeAll || storeBaseDocument)
			return;

		log.debug("Updating {} max XMI IDs.", maxXmiIdMap.size());

		FieldConfig annotationFieldConfig = dbc.getFieldConfiguration(schemaAnnotation);
		String[] primaryKey = annotationFieldConfig.getPrimaryKey();
		if (primaryKey.length > 1)
			throw new IllegalArgumentException("Currently, only one-element primary keys are supported.");
		String primaryKeyString = annotationFieldConfig.getPrimaryKeyString();

		String updateString = "UPDATE " + dbc.getActiveDataPGSchema() + "." + docTableName + " SET " + FIELD_MAX_XMI_ID
				+ " = ? WHERE " + primaryKeyString + " = ?";

		try {
			PreparedStatement updateMaxXmiId = conn.prepareStatement(updateString);
			for (String pmid : maxXmiIdMap.keySet()) {
				Integer maxXmiId = maxXmiIdMap.get(pmid);
				updateMaxXmiId.setInt(1, maxXmiId);
				updateMaxXmiId.setString(2, pmid);
				updateMaxXmiId.addBatch();
			}
			updateMaxXmiId.executeBatch();
		} catch (SQLException e) {
			e.printStackTrace();
			SQLException nextException = e.getNextException();
			if (null == nextException)
				throw new XmiDataInsertionException(e);
			throw new XmiDataInsertionException(nextException);
		} finally {
			maxXmiIdMap.clear();
		}
	}

	public void putXmiIdMapping(String docId, Integer newXmiId) {
		maxXmiIdMap.put(docId, newXmiId);
	}

	public void addProcessedDocumentId(String docId) {
		processedDocumentIds.add(docId);
	}

}
