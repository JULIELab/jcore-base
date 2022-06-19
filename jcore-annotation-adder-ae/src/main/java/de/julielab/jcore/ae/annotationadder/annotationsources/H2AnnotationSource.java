package de.julielab.jcore.ae.annotationadder.annotationsources;

import de.julielab.java.utilities.UriUtilities;
import de.julielab.jcore.ae.annotationadder.annotationformat.AnnotationFormat;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationList;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalDocumentClassAnnotation;
import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.uima.resource.DataResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.julielab.jcore.ae.annotationadder.annotationsources.TextAnnotationProvider.*;

public class H2AnnotationSource<T extends AnnotationData> implements AnnotationSource<AnnotationList<T>> {
    private final static Logger log = LoggerFactory.getLogger(H2AnnotationSource.class);
    private AnnotationFormat<T> format;
    private Path h2DbPath;
    private Statement queryStmt;
    private Class<?> annotationDataClass;

    public H2AnnotationSource(AnnotationFormat<T> format) {
        this.format = format;
        if (format.getHeader() == null)
            throw new IllegalArgumentException("To use the H2AnnotationSource, the input format must define the column headers. The employed format " + format + " does not specify them itself. Thus, the header must be specified in the component descriptor external resource definition.");
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            log.error("Could not load the h2 Driver through 'Class.forName(\"org.h2.Driver\").");
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void loadAnnotations(URI annotationUri) throws IOException {
        final Path annotationFilePath = Path.of(annotationUri);
        h2DbPath = Path.of(annotationFilePath + ".h2");
        if (!Files.exists(h2DbPath) || Files.getLastModifiedTime(annotationFilePath).toMillis() < Files.getLastModifiedTime(h2DbPath).toMillis()) {
            log.info("Source annotation file {} is newer than database file {}. Creating a new database.", annotationFilePath, h2DbPath);
            Files.list(h2DbPath.getParent()).filter(p -> p.toString().startsWith(h2DbPath.toString())).forEach(p -> FileUtils.deleteQuietly(p.toFile()));
            try (Connection conn = DriverManager.
                    getConnection("jdbc:h2:" + h2DbPath, "sa", "")) {
                conn.setAutoCommit(false);
                PreparedStatement ps = null;
                Map<String, Integer> columnIndexes = new HashMap<>();
                try (BufferedReader br = UriUtilities.getReaderFromUri(annotationUri)) {
                    final Iterator<T> iterator = br.lines().map(format::parse).filter(Objects::nonNull).iterator();
                    boolean firstDataItem = true;
                    int psSize = 0;
                    while (iterator.hasNext()) {
                        T annotationData = iterator.next();
                        // We need to create the table after the retrieval of the first annotation item because the
                        // format parser derive the data types from the data
                        if (firstDataItem) {
                            for (int i = 0; i < format.getHeader().length; i++) {
                                if (format.getHeader()[i].equals("begin"))
                                    format.getHeader()[i] = COL_BEGIN;
                                else if (format.getHeader()[i].equals("end"))
                                    format.getHeader()[i] = COL_END;
                            }
                            IntStream.range(0, format.getHeader().length).forEach(i -> columnIndexes.put(format.getHeader()[i], i));
                            annotationDataClass = annotationData.getClass();
                            createAnnotationTable(conn, annotationData);
                            String insertionSql = "INSERT INTO annotations VALUES (" + IntStream.range(0, format.getHeader().length).mapToObj(i -> "?").collect(Collectors.joining(",")) + ")";
                            ps = conn.prepareStatement(insertionSql);
                            firstDataItem = false;
                        }
                        if (annotationData instanceof ExternalDocumentClassAnnotation)
                            throw new NotImplementedException("ExternalDocumentClassAnnotation data is currently not supprted by the H2AnnotationSource.");
                        ExternalTextAnnotation textAnnotation = (ExternalTextAnnotation) annotationData;
                        final Map<String, Object> fieldValues = textAnnotation.getAllFieldValuesAsMap();
                        for (String columnName : format.getHeader()) {
                            ps.setObject(columnIndexes.get(columnName) + 1, fieldValues.get(columnName));
                        }
                        ps.addBatch();
                        ++psSize;
                        if (psSize == 50) {
                            ps.executeBatch();
                            psSize = 0;
                        }
                    }
                    if (psSize > 0)
                        ps.executeBatch();
                }
                if (log.isTraceEnabled()) {
                    int numRows = getCount(conn, "SELECT count(*) FROM annotations");
                    int numDocIds = getCount(conn, "SELECT count(DISTINCT docId) FROM annotations");
                    log.trace("Loaded {} entity annotations for {} document IDs.", numRows, numDocIds);
                }
                conn.commit();
            } catch (SQLException e) {
                log.error("Could not create H2 database at {}", h2DbPath);
                throw new IllegalStateException(e);
            }
        }
    }

    private int getCount(Connection conn, String sql) {
        try {
            final ResultSet rs = conn.createStatement().executeQuery(sql);
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Could not count rows via SQL query {}", sql, e);
            throw new IllegalStateException(e);
        }
        return 0;
    }

    private void createAnnotationTable(Connection conn, T annotationData) throws SQLException {
        final Statement stmt = conn.createStatement();
        String tableCreationSql = getTableCreationSql(format.getHeader(), format.getColumnDataTypes(), annotationData);
        try {
            stmt.execute(tableCreationSql);
        } catch (SQLException e) {
            log.error("Could not create the annotation SQL table with command {}", tableCreationSql, e);
            throw new IllegalStateException(e);
        }
        final String indexCreationSql = "CREATE INDEX annotations_doc_id_idx ON annotations (" + format.getHeader()[format.getDocumentIdColumnIndex()] + ")";
        try {
            stmt.execute(indexCreationSql);
        } catch (SQLException e) {
            log.error("Could not create index on document ID column which should be found at index {} of the header {} with SQL {}.", format.getDocumentIdColumnIndex(), format.getHeader(), indexCreationSql, e);
            throw new IllegalStateException(e);
        }
    }

    private String getTableCreationSql(String[] header, List<Class<?>> columnDataTypes, T annotationData) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE annotations (");
        for (int i = 0; i < header.length; i++) {
            String columnName = header[i];
            Class<?> dataType = columnDataTypes.get(i);
            String dbDataType = getDbDataType(dataType);
            sb.append(columnName).append(" ").append(dbDataType);
            if (i < header.length - 1)
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    private String getDbDataType(Class<?> dataType) {
        if (dataType.equals(Integer.class))
            return "INT";
        else if (dataType.equals(Double.class))
            return "DOUBLE";
        else if (dataType.equals(Boolean.class))
            return "BOOL";
        return "VARCHAR";
    }

    @Override
    public void initialize(DataResource dataResource) throws IOException {
        log.info("Loading entity annotations from {}", dataResource.getUri());
        loadAnnotations(dataResource.getUri());
    }

    @Override
    public AnnotationList<T> getAnnotations(String id) {
        try {
            if (queryStmt == null) {
                Connection queryConn = DriverManager.
                        getConnection("jdbc:h2:" + h2DbPath, "sa", "");
                queryStmt = queryConn.createStatement();
            }
        } catch (SQLException e) {
            log.error("Could not connect to database at {}", h2DbPath, e);
            throw new IllegalStateException(e);
        }
        final String sql = "SELECT * FROM annotations WHERE docId='" + id + "'";
        try {
            final ResultSet rs = queryStmt.executeQuery(sql);
            final AnnotationList<T> annotationList = new AnnotationList<>();
            while (rs.next()) {
                T textAnnotation = null;
                if (annotationDataClass == null)
                    throw new IllegalStateException("The annotation data class should have been recorded when data was read from file but it is null.");
                try {
                    if (annotationDataClass.equals(ExternalTextAnnotation.class))
                        textAnnotation = (T) annotationDataClass.getConstructor(String.class, int.class, int.class, String.class).newInstance(rs.getString(COL_DOC_ID), rs.getInt(COL_BEGIN), rs.getInt(COL_END), rs.getString(COL_UIMA_TYPE));
                    else
                        throw new NotImplementedException("The annotation class " + annotationDataClass + " is currently not supported by the H2AnnotationSource.");
                } catch (Exception e) {
                    log.error("Could not create instance of annotation data class {}", annotationDataClass, e);
                }
                for (String columnName : format.getHeader()) {
                        final Object value = rs.getObject(columnName);
                    if (value != null && textAnnotation instanceof ExternalTextAnnotation && !columnName.equals(COL_UIMA_TYPE) && !columnName.equals(COL_DOC_ID)) {
                        ExternalTextAnnotation a = (ExternalTextAnnotation) textAnnotation;
                        String payLoadKey = columnName;
                        if(payLoadKey.equals(COL_BEGIN))
                            payLoadKey = "begin";
                        else if (payLoadKey.equals(COL_END))
                            payLoadKey = "end";
                        a.addPayload(payLoadKey, value);
                    }
                }
                annotationList.add(textAnnotation);
            }
            return annotationList;
        } catch (SQLException e) {
            log.error("Could not retrieve annotation values from the H2 database via SQL query '{}'", sql);
            throw new IllegalStateException(e);
        }
    }
}
