package de.julielab.jcore.consumer.xmi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.julielab.xmlData.dataBase.util.CoStoSysSQLRuntimeException;
import de.julielab.xmlData.dataBase.util.TableSchemaMismatchException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.xmlData.dataBase.DataBaseConnector;

public class AnnotationTableManager {

    public static final String ANNOTATION_LIST_TABLE = "_annotation_tables";
    public static final String TABLE_NAME = "tablename";
    private static final Logger log = LoggerFactory.getLogger(AnnotationTableManager.class);
    private DataBaseConnector dbc;

    private String documentTableSchema;

    private String annotationTableSchema;

    private Boolean storeAll;

    private String dbDocumentTableName;
    private String annotationStorageSchema;

    private Boolean storeBaseDocument;

    private List<String> obsoleteAnnotationTables;

    private List<String> annotationsToStore;

    public AnnotationTableManager(DataBaseConnector dbc, String rawDocumentTableName, List<String> annotationsToStore,
                                  String documentTableSchema, String annotationTableSchema, Boolean storeAll, Boolean storeBaseDocument, String annotationStorageSchema) throws TableSchemaMismatchException {
        this.dbc = dbc;
        this.annotationsToStore = annotationsToStore;
        this.documentTableSchema = documentTableSchema;
        this.annotationTableSchema = annotationTableSchema;
        this.storeAll = storeAll;
        this.storeBaseDocument = storeBaseDocument;
        this.dbDocumentTableName = getEffectiveDocumentTableName(rawDocumentTableName);
        this.annotationStorageSchema = annotationStorageSchema;
        createTable(rawDocumentTableName, documentTableSchema);
        for (String annotation : annotationsToStore)
            createTable(annotation, annotationTableSchema);
        createAnnotationListTable();
    }

    /**
     * Normalizes table names by replacing dots "." with underscores "_" and
     * prepending the active data postgres schema ONLY IF the option
     * <tt>storeAll</tt> is set to FALSE. If <tt>storeAll</tt> is true, the
     * table name is returned unchanged.
     *
     * @param tableNameParameter the table name to normalize
     * @param storeAll           whether or not the complete document XMI is supposed to be
     *                           stored
     * @return The normalized table name
     */
    public String convertAnnotationTypeToTableName(String tableNameParameter, boolean storeAll) {
        if (storeAll || tableNameParameter.equals(dbDocumentTableName))
            return getEffectiveDocumentTableName(tableNameParameter);
        // A table cannot be created if the name contains dots. All annotation
        // tables
        // will thus have dots replaced by underline.
        String effectiveTableName = tableNameParameter.contains(":") ? tableNameParameter.substring(tableNameParameter.indexOf(':') + 1) : tableNameParameter;
        effectiveTableName = effectiveTableName.replace(".", "_");
        String schema = tableNameParameter.contains(":") ? tableNameParameter.substring(0, tableNameParameter.indexOf('.')) : annotationStorageSchema;
        return schema + "." + effectiveTableName;
    }

    /**
     * If <code>documentTableParameter</code> is not schema qualified, prependns the active data postgres schema.
     *
     * @param documentTableParameter The document table, as given in the component parameter.
     * @return The effective document table name.
     */
    String getEffectiveDocumentTableName(String documentTableParameter) {
        // If the table is already schema qualified, accept it.
        if (documentTableParameter.contains("."))
            return documentTableParameter;
        return dbc.getActiveDataPGSchema() + "." + documentTableParameter;
    }

    /**
     * <p>
     * If the base document is stored, returns the names of all annotations
     * tables that
     * <ul>
     * <li>are listed in the _annotation_tables</li>
     * <li>are not listed as additional table in the current consumer.</li>
     * </ul>
     * <p>
     * <p>
     * Otherwise does return the empty list.
     * </p>
     * <p>
     * The idea is that when storing the base document all annotations created
     * before become obsolete due invalid xmi:id references (e.g. to the sofa
     * the annotations are stored in). Thus, we better remove those annotations
     * which are all annotations that are not stored with the base document.
     * </p>
     *
     * @return Annotation table names that are not stored with the base document
     * or, if the base document is not stored, an empty list.
     */
    List<String> getObsoleteAnnotationTableNames() {
        if (!storeBaseDocument)
            return Collections.emptyList();
        if (null == obsoleteAnnotationTables) {
            obsoleteAnnotationTables = new ArrayList<>();
            // first get the names of all annotation tables
            try {
                Connection conn = dbc.obtainConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT " + TABLE_NAME + " FROM " + dbc.getActiveDataPGSchema() + "." + ANNOTATION_LIST_TABLE);
                while (rs.next()) {
                    obsoleteAnnotationTables.add(rs.getString(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // then remove those that are NOT obsolete from the list, leaving us
            // with the obsolete table names
            Set<String> annotationsToStoreTableNameSet = new HashSet<>();
            for (String annotationTypeToStore : annotationsToStore)
                annotationsToStoreTableNameSet.add(convertAnnotationTypeToTableName(annotationTypeToStore, false));
            Iterator<String> it = obsoleteAnnotationTables.iterator();
            while (it.hasNext()) {
                String tablename = it.next();
                if (annotationsToStoreTableNameSet.contains(tablename))
                    it.remove();
            }
        }
        return obsoleteAnnotationTables;
    }

    void createTable(String tableName, String schema) throws TableSchemaMismatchException {
        String effectiveTableName = convertAnnotationTypeToTableName(tableName, storeAll);
        if (getEffectiveDocumentTableName(tableName).equals(dbDocumentTableName))
            effectiveTableName = dbDocumentTableName;
        try {
            if (!dbc.tableExists(effectiveTableName)) {
                log.info("Creating table '{}' with schema '{}' (columns: {}).",
                        effectiveTableName, schema, dbc.getFieldConfiguration(schema).getColumns());
                if (storeAll) {
                    dbc.createTable(effectiveTableName, schema,
                            "Created by " + XMIDBWriter.class.getName() + " on " + new Date());
                } else {
                    if (!effectiveTableName.equals(dbDocumentTableName)) {
                        dbc.createTable(effectiveTableName, dbDocumentTableName, schema,
                                "Created by " + XMIDBWriter.class.getName() + " on " + new Date()
                                        + " to store annotations of type\"" + tableName
                                        + "\" for the documents stored in table \"" + dbDocumentTableName + "\".");
                    } else {
                        dbc.createTable(effectiveTableName, schema, "Created by " + XMIDBWriter.class.getName()
                                + " on " + new Date() + " to store the base documents without"
                                + " linguistic or semantic annotations." + " Those are stored in the tables named"
                                + " after the annotation type they are" + " storing in this Postgres schema.");
                    }
                }
            }
            String tableSchemaNameToCheck = effectiveTableName.equals(dbDocumentTableName) ? documentTableSchema
                    : annotationTableSchema;
            dbc.checkTableDefinition(effectiveTableName, tableSchemaNameToCheck);

            if (!effectiveTableName.equals(dbDocumentTableName))
                addAnnotationTableToList(effectiveTableName);
        } catch (CoStoSysSQLRuntimeException e) {
            log.warn(
                    "SQLException was thrown when creating tables. Possibly it is a concurrency issue and it has been " +
                            "tried to create the tables although they had already been created by another process " +
                            "in the meantime. Error was: {}",
                    e);
        }
    }

    /**
     * Adds the extract string <tt>tablename</tt> to the list of annotation
     * table names.
     *
     * @param tablename
     */
    void addAnnotationTableToList(String tablename) {
        try {
            Connection conn = dbc.obtainConnection();
            conn.setAutoCommit(true);
            Statement stmt = conn.createStatement();

            String template = "INSERT INTO %s VALUES('%s')";
            String sql = String.format(template, dbc.getActiveDataPGSchema() + "." + ANNOTATION_LIST_TABLE, tablename);
            stmt.execute(sql);

        } catch (PSQLException e) {
            log.debug("Tried to add already existing annotation table to annotation list: \"{}\", ignoring.",
                    tablename);
        } catch (SQLException e) {
            e.printStackTrace();
            SQLException ne = e.getNextException();
            if (null != ne)
                ne.printStackTrace();
        }
    }

    private void createAnnotationListTable() {
        if (!dbc.tableExists(dbc.getActiveDataPGSchema() + "." + ANNOTATION_LIST_TABLE)) {
            try {
                Connection conn = dbc.obtainConnection();
                conn.setAutoCommit(true);
                Statement stmt = conn.createStatement();
                String sql = String.format("CREATE TABLE %s (%s text PRIMARY KEY)",
                        dbc.getActiveDataPGSchema() + "." + ANNOTATION_LIST_TABLE, TABLE_NAME);
                stmt.execute(sql);
            } catch (SQLException e) {
                e.printStackTrace();
                SQLException ne = e.getNextException();
                if (null != ne)
                    ne.printStackTrace();
            }
        }
    }
}
