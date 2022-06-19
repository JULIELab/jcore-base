package de.julielab.jcore.ae.annotationadder.annotationformat;

import de.julielab.jcore.ae.annotationadder.annotationrepresentations.AnnotationData;

import java.util.List;

public interface AnnotationFormat<T extends AnnotationData> {
    T parse(String data);

    void hasHeader(boolean withHeader);

    String[] getHeader();

    List<Class<?>> getColumnDataTypes();

    void setColumnNames(String[] header);

    int getDocumentIdColumnIndex();

    default Class<?> determineDataType(String value) {
        Class<?> dataType = String.class;
        try {
            Integer.parseInt(value);
            dataType = Integer.class;
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(value);
                dataType = Double.class;
            } catch (NumberFormatException e2) {
                if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("true")) {
                    dataType = Boolean.class;
                }
            }
        }
        return dataType;
    }

}
