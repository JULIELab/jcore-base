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

import de.julielab.jcore.types.ace.Document;
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
    private Map<DocumentId, Integer> maxXmiIdMap;
    private String componentDbName;

    private List<DocumentId> processedDocumentIds;

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
        this.maxXmiIdMap = new HashMap<DocumentId, Integer>();
        this.processedDocumentIds = new ArrayList<DocumentId>();
    }

    /**
     * Constructs row iterators for the different tables (document and
     * annotations) conforming to the expectations of the DataBaseConnector API.
     * If update mode is <code>true</code>, the CAS data will be added as an
     * update. It will just be inserted otherwise (throwing an error if there
     * will be a primary key constraint violation, i.e. duplicates).
     *
     * @param serializedCASes
     * @param tablesWithoutData
     * @throws XmiDataInsertionException
     * @throws AnalysisEngineProcessException
     */
    public void sendXmiDataToDatabase(LinkedHashMap<String, List<XmiData>> serializedCASes,
                                      Map<String, List<DocumentId>> tablesWithoutData, String subsetTableName) throws XmiDataInsertionException {

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

                // We need to fill the, possibly complex, primary key with the values from the actual
                // document ID. We here assume that the order of document ID elements directly corresponds
                // to the order of the primary key definition in the field configuration.
                int i = 0;
                for (Integer pkIndex : fieldConfig.getPrimaryKeyFieldNumbers()) {
                    row.put(fName.apply(pkIndex), results.docId.getId()[i++]);
                    if (log.isTraceEnabled())
                        log.trace("{}={}", fName.apply(pkIndex), row.get(fName.apply(pkIndex)));
                }
                row.put(fName.apply(i++), results.data);
                if (log.isTraceEnabled())
                    log.trace("{}={}", fName.apply(i - 1), row.get(fName.apply(i - 1)));
                if (results.getClass().equals(DocumentXmiData.class)) {
                    if (fieldConfig.getFields().size() - fieldConfig.getPrimaryKey().length < 3)
                        throw new IllegalArgumentException("The XMI data table schema is set to the schema with name " +
                                "\"" + schemaDocument + "\" that specifies the fields \"" +
                                StringUtils.join(fieldConfig.getColumns(), ",") + "\". However, this schema " +
                                "is not compatible with XMI base " +
                                "document storage since the storage requires two extra fields to store the maximum XMI " +
                                "ID of the document and the sofa mapping.");
                    DocumentXmiData docResults = (DocumentXmiData) results;
                    row.put("max_xmi_id", docResults.newXmiId);
                    log.trace("{}={}", "max_xmi_id", docResults.newXmiId);
                    row.put("sofa_mapping", docResults.serializedSofaXmiIdMap);
                    log.trace("{}={}", "sofa_mapping", docResults.serializedSofaXmiIdMap);
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
     *
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
        // create a string for the prepared statement in the form "pk1 = ? AND pk2 = ? ..."
        String primaryKeyPsString = StringUtils.join(annotationFieldConfig.expandPKNames("%s = ?"), " AND ");

        log.debug("Marking {} documents to having been processed by component \"{}\".", processedDocumentIds.size(), componentDbName);

        String sql = String.format("UPDATE %s SET %s='%s' WHERE %s", subsetTableName, Constants.LAST_COMPONENT, componentDbName, primaryKeyPsString);
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            for (DocumentId docId : processedDocumentIds) {
                for (int i = 0; i < docId.getId().length; i++) {
                    String pkElement = docId.getId()[i];
                    ps.setString(i + 1, pkElement);
                }
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
     * @param conn
     * @throws XmiDataInsertionException
     * @throws AnalysisEngineProcessException
     */

    private void deleteRowsFromTablesWithoutData(Map<String, List<DocumentId>> tablesWithoutData, Connection conn,
                                                 DataBaseConnector dbc, List<String> annotationsToStore) throws XmiDataInsertionException {
        if (!updateMode || storeAll || annotationsToStore.isEmpty())
            return;

        FieldConfig annotationFieldConfig = dbc.getFieldConfiguration(schemaAnnotation);
        String[] primaryKey = annotationFieldConfig.getPrimaryKey();
        if (primaryKey.length > 1)
            throw new IllegalArgumentException("Currently, only one-element primary keys are supported.");

        for (Entry<String, List<DocumentId>> entry : tablesWithoutData.entrySet()) {
            List<DocumentId> docIds = entry.getValue();
            if (docIds.size() == 0)
                continue;

            String tableName = entry.getKey();
            // Create the primary key string for the prepared statement:
            // pk1 = ? AND pk2 = ? AND ...
            String pkElementPsString = StringUtils.join(annotationFieldConfig.expandPKNames("%s = ?"), " AND ");
            String deleteString = "DELETE FROM " + tableName + " WHERE " + pkElementPsString;

            try {
                PreparedStatement deleteRowWithoutData = conn.prepareStatement(deleteString);
                for (DocumentId docId : docIds) {
                    for (int i = 0; i < docId.getId().length; i++) {
                        String pkElement = docId.getId()[i];
                        deleteRowWithoutData.setString(i + 1, pkElement);
                    }
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
                docIds.clear();
            }
        }
    }

    /**
     * Stores the next possible xmi id that can be assigned to new annotations
     * in order to make sure that there aren't any clashes with already existing
     * ids.
     *
     * @throws XmiDataInsertionException
     * @throws AnalysisEngineProcessException
     */
    public void updateMaxXmiId(Connection conn) throws XmiDataInsertionException {
        if (storeAll || storeBaseDocument)
            return;

        log.debug("Updating {} max XMI IDs.", maxXmiIdMap.size());

        FieldConfig annotationFieldConfig = dbc.getFieldConfiguration(schemaAnnotation);
        // gives:
        // pk1 = ?
        // pk2 = ?
        // ...
        String[] pkPsPlaceholder = annotationFieldConfig.expandPKNames("%s = ?");
        // gives:
        // pk1 = ? AND pk2 = ? AND pk3 = ? ...
        String pkElementCondition = StringUtils.join(pkPsPlaceholder, " AND ");

        String updateString = "UPDATE " + dbc.getActiveDataPGSchema() + "." + docTableName + " SET " + FIELD_MAX_XMI_ID
                + " = ? WHERE " + pkElementCondition;

        try {
            PreparedStatement updateMaxXmiId = conn.prepareStatement(updateString);
            for (DocumentId docId : maxXmiIdMap.keySet()) {
                Integer maxXmiId = maxXmiIdMap.get(docId);
                updateMaxXmiId.setInt(1, maxXmiId);
                // fill the placeholders with actual primary key values
                for (int i = 0; i < docId.getId().length; i++) {
                    String pkElement = docId.getId()[i];
                    updateMaxXmiId.setString(i + 2, pkElement);
                }
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

    public void putXmiIdMapping(DocumentId docId, Integer newXmiId) {
        maxXmiIdMap.put(docId, newXmiId);
    }

    public void addProcessedDocumentId(DocumentId docId) {
        processedDocumentIds.add(docId);
    }

}
