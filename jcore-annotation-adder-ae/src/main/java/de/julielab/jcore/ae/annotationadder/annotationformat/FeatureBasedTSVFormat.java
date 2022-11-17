package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.ExternalTextAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static de.julielab.jcore.ae.annotationadder.annotationsources.TextAnnotationProvider.COL_UIMA_TYPE;

public class FeatureBasedTSVFormat implements AnnotationFormat<ExternalTextAnnotation> {
    private final static Logger log = LoggerFactory.getLogger(FeatureBasedTSVFormat.class);
    private String[] header;
    private boolean withHeader;
    private Integer uimaTypeIndex;
    private List<Class<?>> columnDataTypes;

    @Override
    public ExternalTextAnnotation parse(String data) {
        if (data == null || data.startsWith("#"))
            return null;
        final String[] record = data.split("\t");
        if (record.length < 3)
            throw new IllegalArgumentException("Expected at least 3 column format providing document ID, begin and end offset for the annotation but got " + record.length + " columns: " + data);
        if (withHeader && header == null) {
            header = record;
            return null;
        }
        if (columnDataTypes == null)
            columnDataTypes = new ArrayList<>(header.length);
        if (uimaTypeIndex == null) {
            uimaTypeIndex = -1;
            for (int i = 0; i < header.length; i++) {
                if (header[i].equals(COL_UIMA_TYPE))
                    uimaTypeIndex = i;
            }
            if (uimaTypeIndex == 0)
                throw new IllegalArgumentException("Found the uima_type column at index 0. However, the first column is reserved for the document ID.");
        }
        if (columnDataTypes.isEmpty())
            determineColumnDataTypes(record);
        String docId = record[0];
        String type = uimaTypeIndex >= 0 ? record[uimaTypeIndex] : null;
        ExternalTextAnnotation externalTextAnnotation = new ExternalTextAnnotation(docId, 0, 0, type);
        externalTextAnnotation.setPayloadFeatureValues(true);
        for (int i = 1; i < Math.min(header.length, record.length); i++) {
            String featureName = header[i];
            String columnValue = record[i];
            if (!featureName.equals(COL_UIMA_TYPE))
                externalTextAnnotation.addPayload(featureName, convertValueToFieldDataType(columnValue, i));
        }

        return externalTextAnnotation;
    }

    private Object convertValueToFieldDataType(String columnValue, int columnIndex) {
        final Class<?> columnDataType = columnDataTypes.get(columnIndex);
        if (columnDataType.equals(Integer.class))
            return Integer.parseInt(columnValue);
        else if (columnDataType.equals(Double.class))
            return Double.parseDouble(columnValue);
        else if (columnDataType.equals(Boolean.class))
            return Boolean.parseBoolean(columnValue);
        return columnValue.intern();
    }

    private void determineColumnDataTypes(String[] record) {
        for (int i = 0; i < record.length; i++) {
            String value = record[i];
            Class<?> dataType = determineDataType(value);
            columnDataTypes.add(dataType);
        }
        log.info("Identified the data types of columns {} as {}", header, columnDataTypes);
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
