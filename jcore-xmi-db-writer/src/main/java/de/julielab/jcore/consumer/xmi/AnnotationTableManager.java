package de.julielab.jcore.consumer.xmi;

import de.julielab.costosys.cli.TableNotFoundException;
import de.julielab.costosys.configuration.FieldConfig;
import de.julielab.costosys.dbconnection.CoStoSysConnection;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.costosys.dbconnection.util.CoStoSysSQLRuntimeException;
import de.julielab.costosys.dbconnection.util.TableSchemaMismatchException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationTableManager {

    public static final String ANNOTATION_LIST_TABLE = "_annotation_tables";
    public static final String TABLE_NAME = "tablename";
    private static final Logger log = LoggerFactory.getLogger(AnnotationTableManager.class);
    private DataBaseConnector dbc;

    private boolean binaryAnnotationColumns;
    private String documentTableSchema;

    private Boolean storeAll;

    private String dbDocumentTableName;
    private String annotationStorageSchema;
    private String xmiMetaSchema;

    private Boolean storeBaseDocument;

    private List<String> obsoleteAnnotationTables;

    private List<String> annotationsToStore;

    private Map<String, String> annotationPgSchemaMap = new HashMap<>();

    public AnnotationTableManager(DataBaseConnector dbc, String rawDocumentTableName, List<String> annotationsToStore, boolean binaryAnnotationColumns,
                                  String documentTableSchema, Boolean storeAll, Boolean storeBaseDocument, String annotationStorageSchema, String xmiMetaSchema) throws TableSchemaMismatchException {
        this.dbc = dbc;
        this.annotationsToStore = annotationsToStore;
        this.binaryAnnotationColumns = binaryAnnotationColumns;
        this.documentTableSchema = documentTableSchema;
        this.storeAll = storeAll;
        this.storeBaseDocument = storeBaseDocument;
        this.dbDocumentTableName = getEffectiveDocumentTableName(rawDocumentTableName);
        this.annotationStorageSchema = annotationStorageSchema;
        this.xmiMetaSchema = xmiMetaSchema;
        createTable(rawDocumentTableName, annotationsToStore, documentTableSchema);
        createAnnotationListTable();
        for (String qualifiedAnnotation : annotationsToStore) {
            final int colonIndex = qualifiedAnnotation.indexOf(':');
            if (colonIndex >= 0) {
                String typeName = qualifiedAnnotation.substring(colonIndex + 1);
                String schemaName = qualifiedAnnotation.substring(0, colonIndex);
                annotationPgSchemaMap.put(typeName, schemaName);
            }
        }
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
        effectiveTableName = effectiveTableName.replace(".", "_").toLowerCase();
        String schema = annotationPgSchemaMap.get(tableNameParameter);
        if (schema == null && tableNameParameter.contains(":"))
            schema = tableNameParameter.substring(0, tableNameParameter.indexOf(':'));
        return convertQualifiedAnnotationTypeToColumnName((schema != null ? schema + ":" : "") + effectiveTableName);
    }

    /**
     * <p>Converts potentially qualified annotation types to Postgres-valid column names.</p>
     * <p>Examples:
     * <samp>
     * <pre>de.julielab.jcore.types.Token -> de_julielab_types_token</pre>
     * <pre>experiments:de.julielab.jcore.types.Gene -> experiments$de_julielab_jcore_types_gene</pre>
     * </samp>
     * </p>
     *
     * @param qualifiedAnnotationName The annotation name to convert, optionally with a qualification prefix.
     * @return The Postgres compatible column name for this annotation name.
     */
    public static String convertQualifiedAnnotationTypeToColumnName(String qualifiedAnnotationName) {
        final String[] split = qualifiedAnnotationName.split(":");
        final boolean nameIsQualified = qualifiedAnnotationName.contains(":");
        String annotationName = nameIsQualified ? split[1] : split[0];
        String qualifier = nameIsQualified ? split[0] + "$" : "";
        final String pgCompatibleAnnotationName = annotationName.toLowerCase().replace(".", "_");
        return qualifier + pgCompatibleAnnotationName;
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
            try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT " + TABLE_NAME + " FROM " + xmiMetaSchema + "." + ANNOTATION_LIST_TABLE);
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

    void createTable(String tableName, List<String> annotationsToStore, String schema) throws TableSchemaMismatchException {
        String effectiveTableName = convertAnnotationTypeToTableName(tableName, storeAll);
        List<String> annotationColumns = annotationsToStore.stream().map(annotationName -> convertAnnotationTypeToTableName(annotationName, storeAll)).collect(Collectors.toList());
        if (getEffectiveDocumentTableName(tableName).equals(dbDocumentTableName))
            effectiveTableName = dbDocumentTableName;
        try {
                final FieldConfig fieldConfig = dbc.getFieldConfiguration(schema);
            if (!dbc.tableExists(effectiveTableName)) {
                log.info("Creating table '{}' with schema '{}' (columns: {}).",
                        effectiveTableName, schema, fieldConfig.getColumns());
                String pgSchema = getTableSchema(effectiveTableName);
                if (!dbc.schemaExists(pgSchema))
                    dbc.createSchema(pgSchema);
                if (storeAll) {
                    dbc.createTable(effectiveTableName, schema,
                            "Created by " + XMIDBWriter.class.getName() + " on " + new Date());
                } else {
                    dbc.createTable(effectiveTableName, schema, "Created by " + XMIDBWriter.class.getName()
                            + " on " + new Date() + " to store the base documents and"
                            + " linguistic or semantic annotations." + " The annotations are stored in the columns named"
                            + " after the annotation type.");
                }
            }
            dbc.assureColumnsExist(tableName, annotationColumns, binaryAnnotationColumns ? "bytea" : "xml");

           dbc.checkTableHasSchemaColumns(tableName, schema);

            annotationColumns.forEach(this::addAnnotationTableToList);
        } catch (CoStoSysSQLRuntimeException e) {
            log.warn(
                    "SQLException was thrown when creating tables. Possibly it is a concurrency issue and it has been " +
                            "tried to create the tables although they had already been created by another process " +
                            "in the meantime. Error was: {}",
                    e);
        } catch (TableNotFoundException e) {
            log.error("Table not found", e);
        }

    }

    private String getTableSchema(String effectiveTableName) {
        int dotIndex = effectiveTableName.indexOf('.');
        if (dotIndex < 0)
            return null;
        return effectiveTableName.substring(0, dotIndex);
    }

    /**
     * Adds the extract string <tt>tablename</tt> to the list of annotation
     * table names.
     *
     * @param tablename
     * @deprecated Not required any more since all stored annotations are now manifested as a table column
     */
    void addAnnotationTableToList(String tablename) {
        try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
            conn.setAutoCommit(true);
            Statement stmt = conn.createStatement();

            String template = "INSERT INTO %s VALUES('%s')";
            String sql = String.format(template, xmiMetaSchema + "." + ANNOTATION_LIST_TABLE, tablename);
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
        if (!dbc.tableExists(xmiMetaSchema + "." + ANNOTATION_LIST_TABLE)) {
            try (CoStoSysConnection conn = dbc.obtainOrReserveConnection()) {
                conn.setAutoCommit(true);
                if (!dbc.schemaExists(xmiMetaSchema))
                    dbc.createSchema(xmiMetaSchema);
                Statement stmt = conn.createStatement();
                String sql = String.format("CREATE TABLE %s (%s text PRIMARY KEY)",
                        xmiMetaSchema + "." + ANNOTATION_LIST_TABLE, TABLE_NAME);
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
