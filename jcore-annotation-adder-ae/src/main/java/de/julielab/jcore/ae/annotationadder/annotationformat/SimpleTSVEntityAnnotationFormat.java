package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.julielab.jcore.ae.annotationadder.annotationsources.TextAnnotationProvider.*;

public class SimpleTSVEntityAnnotationFormat implements AnnotationFormat<ExternalTextAnnotation> {
    private String[] header;
    private boolean withHeader;
    private List<Class<?>> columnDataTypes;

    @Override
    public ExternalTextAnnotation parse(String data) {
        if (data == null || data.startsWith("#"))
            return null;
        final String[] record = data.split("\t");
        if (record.length < 3)
            throw new IllegalArgumentException("Expected a 3 or 4-column format providing document ID, begin, end and UIMA type (optional if the default type is set to the AnnotationAdderAnnotator) for the annotation but got " + record.length + " columns: " + data);
        if (withHeader && header == null) {
            header = record;
            return null;
        }
        boolean columnDataTypesWasNull = columnDataTypes == null;
        if (columnDataTypesWasNull) {
            columnDataTypes = Stream.of(String.class, Integer.class, Integer.class).collect(Collectors.toList());
        }
        String docId = record[0];
        int begin = Integer.parseInt(record[1]);
        int end = Integer.parseInt(record[2]);
        String type = null;
        if (record.length > 3) {
            type = record[3];
            if (columnDataTypesWasNull)
                columnDataTypes.add(String.class);
        }
        if (header == null && record.length <= 3)
            header = new String[]{COL_DOC_ID, COL_BEGIN, COL_END, COL_UIMA_TYPE};
        ExternalTextAnnotation externalTextAnnotation = new ExternalTextAnnotation(docId, begin, end, type);
        if (record.length > 4) {
            if (header != null) {
                for (int i = 4; i < record.length; i++) {
                    externalTextAnnotation.addPayload(header[i], record[i]);
                    if (columnDataTypesWasNull) {
                        columnDataTypes.add(determineDataType(record[i]));
                    }
                }
            }
        }
        return externalTextAnnotation;
    }

    @Override
    public void hasHeader(boolean withHeader) {
        this.withHeader = withHeader;
    }

    @Override
    public String[] getHeader() {
        return header;
    }

    @Override
    public List<Class<?>> getColumnDataTypes() {
        if (columnDataTypes == null)
            throw new IllegalStateException("The column data types are not yet set. This call must come after the first line of data has been read.");
        return columnDataTypes;
    }

    @Override
    public void setColumnNames(String[] header) {
        this.header = header;
    }

    @Override
    public int getDocumentIdColumnIndex() {
        return 0;
    }
}
