package de.julielab.jcore.reader.xml;

import de.julielab.xml.JulieXMLConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

public class Row2CasMapper {

    protected static final int TYPE = 0;
    protected static final int FEATURE_AND_DATATYPE = 1;
    private final static Logger LOGGER = LoggerFactory.getLogger(Row2CasMapper.class);
    protected LinkedHashMap<Integer, RowMapElement> rowMapping;
    protected Method addToIndexes;
    private Supplier<List<Map<String, Object>>> columnsToRetrieveSupplier;


    public Row2CasMapper(String[] rowMappingArray, Supplier<List<Map<String, Object>>> columnsToRetrieveSupplier) throws ResourceInitializationException {
        this.columnsToRetrieveSupplier = columnsToRetrieveSupplier;
        buildRowMapping(rowMappingArray);
        try {
            addToIndexes = TOP.class.getDeclaredMethod("addToIndexes");
        } catch (NoSuchMethodException e) {
            throw new ResourceInitializationException(e);
        }
    }

    public LinkedHashMap<Integer, RowMapElement> getRowMapping() {
        return rowMapping;
    }

    public void mapRowToType(byte[][] arrayArray, JCas jcas) throws CollectionException {

        // Temporary map to cache already created UIMA type objects which could
        // be referenced multiple times (for multiple features of the same type,
        // for example).
        Map<String, TOP> typeObjects = new HashMap<>();
        Set<Annotation> typesToAddToIndexes = new HashSet<Annotation>();

        for (Map.Entry<Integer, RowMapElement> entry : rowMapping.entrySet()) {
            Integer index = entry.getKey();

            if (index >= arrayArray.length) {
                LOGGER.warn(
                        "There is a mapping definition for column {}. However, only {} columns were retrieved from the database.",
                        index, arrayArray.length);
            }

            RowMapElement rowMapElement = entry.getValue();
            byte[] data = arrayArray[index];
            if (null == data) {
                if (null == rowMapElement.defaultValue) {
                    List<Map<String, Object>> allRetrievedColumns = columnsToRetrieveSupplier.get();
                    throw new IllegalArgumentException("A mapping for database data column " + index
                            + " (column name \"" + allRetrievedColumns.get(index).get(JulieXMLConstants.NAME)
                            + "\") has been defined for the Medline reader,"
                            + " however the returned value is null (does not exist in the database) for this "
                            + "document and no default value was specified in the mapping.");
                }
                data = rowMapElement.defaultValue;
            }

            try {
                String typeClassName = rowMapElement.typeConstructor.getDeclaringClass().getName();
                TOP typeObject = typeObjects.get(typeClassName);
                if (typeObject == null) {
                    typeObject = (TOP) rowMapElement.typeConstructor.newInstance(jcas);
                    typeObjects.put(typeClassName, typeObject);
                }
                Method setter = rowMapElement.setter;
                if (data != null) {
                    FeatureValueCreator valueCreator = rowMapElement.featureValueCreator;
                    Object featureValue = valueCreator.getFeatureValue(data);
                    setter.invoke(typeObject, featureValue);
                    addToIndexes.invoke(typeObject);
                }
            } catch (InstantiationException e) {
                LOGGER.error("Instantiation of the type class \"" + rowMapElement.typeConstructor.getName()
                        + "\" specified in the DBMedlineReader descriptor failed.");
                throw new CollectionException(e);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw new CollectionException(e);
            }
        }
        try {
            for (Annotation typeObject : typesToAddToIndexes)
                addToIndexes.invoke(typeObject, jcas);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new CollectionException(e);
        }

    }

    /**
     * @param rowMappingArray
     * @return
     * @throws ResourceInitializationException
     */
    private LinkedHashMap<Integer, RowMapElement> buildRowMapping(String[] rowMappingArray)
            throws ResourceInitializationException {
        if (rowMappingArray == null || rowMappingArray.length == 0)
            return null;

        LinkedHashMap<Integer, RowMapElement> rowMapping = new LinkedHashMap<>();

        for (String mapping : rowMappingArray) {
            // A mapping item has the following form:
            // <column index>=<uima type>#<type feature>:<feature
            // datatype>:defaultValue
            // where the defaultValue is optional. Example:
            // 2=de.julielab.jules.types.max_xmi_id#id:int:0
            // maps the content of the third (index 2) retrieved column (may
            // also belong to an additional table!) to feature "id" of the type
            // "d.j.j.t.max_xmi_id" which is a int. In case there is no value
            // returned from the database for a document, use a 0 as default.

            String[] indexToType = mapping.split("=");
            Integer index = Integer.parseInt(indexToType[0].trim());

            String[] typeAndFeature = indexToType[1].trim().split("#");
            String type = typeAndFeature[TYPE].trim();
            String[] featureDatatypeAndDefault = typeAndFeature[FEATURE_AND_DATATYPE].split(":");
            String feature = featureDatatypeAndDefault[0].trim();
            String datatype = featureDatatypeAndDefault[1].trim();
            byte[] defaultValue = featureDatatypeAndDefault.length > 2 ? featureDatatypeAndDefault[2].trim().getBytes()
                    : null;

            String setterMethod = "set" + StringUtils.capitalize(feature);
            try {
                Class<?> typeClass = Class.forName(type);
                Class<?> featureDataTypeClass = null;
                try {
                    // We use this Spring helper class to be able to dynamically
                    // look up the class even for primitives. At the time of
                    // writing, Spring was included anyway because of the UIMA
                    // fit dependency. If this should be removed some day (we
                    // don't rely further on Spring, AFAIK), the respective
                    // method could just be copied from the Spring source.
                    featureDataTypeClass = ClassUtils.forName(datatype, null);
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Feature datatype class \"" + datatype
                            + "\" has not been found. Please deliver the fully qualified Java name.");
                    throw new ResourceInitializationException(new IllegalArgumentException(
                            "Cannot proceed because feature datatype could not be found, see error log message."));
                }
                Constructor<?> constructor = typeClass.getDeclaredConstructor(JCas.class);
                Method featureSetter = null;
                try {
                    featureSetter = typeClass.getDeclaredMethod(setterMethod, featureDataTypeClass);
                } catch (NoSuchMethodException e) {
                    LOGGER.error("The type class \"" + type
                            + "\" specified in the DBMedlineReader descriptor does not seem to have a feature called \""
                            + feature + "\" or this feature is not a String feature. No setter method \"" + setterMethod
                            + "(" + datatype + ")\" has been found.");
                    throw new ResourceInitializationException(e);
                }

                RowMapElement mapElement = new RowMapElement(featureSetter, constructor,
                        new FeatureValueCreator(datatype), defaultValue);
                rowMapping.put(index, mapElement);
            } catch (ClassNotFoundException e) {
                LOGGER.error("The type class \"" + type
                        + "\" specified in the DBMedlineReader descriptor has not been found.");
                throw new ResourceInitializationException(e);
            } catch (SecurityException | IllegalArgumentException | NoSuchMethodException e) {
                throw new ResourceInitializationException(e);
            }
        }
        return rowMapping;
    }

    private class RowMapElement {

        Method setter;
        Constructor<?> typeConstructor;
        FeatureValueCreator featureValueCreator;
        byte[] defaultValue;

        public RowMapElement(Method setter, Constructor<?> typeConstructor, FeatureValueCreator featureValueCreator,
                             byte[] defaultValue) {
            this.setter = setter;
            this.typeConstructor = typeConstructor;
            this.featureValueCreator = featureValueCreator;
            this.defaultValue = defaultValue;

        }

        @Override
        public String toString() {
            return "RowMapElement [setter=" + setter + ", typeConstructor=" + typeConstructor + ", featureValueCreator="
                    + featureValueCreator + ", defaultValue=" + (defaultValue != null ? new String(defaultValue) : null)
                    + "]";
        }
    }

    private class FeatureValueCreator {
        private String featureDatatype;

        public FeatureValueCreator(String featureDatatype) {
            this.featureDatatype = featureDatatype;
        }

        Object getFeatureValue(byte[] data) {
            Object ret;
            if (null == data)
                throw new IllegalArgumentException(
                        "The data to be converted to a feature value must not be null, but it is.");
            switch (featureDatatype) {
                case "Integer":
                case "int":
                    ret = Integer.parseInt(new String(data));
                    break;
                case "String":
                    ret = new String(data);
                    break;
                default:
                    throw new IllegalArgumentException("Type \"" + featureDatatype
                            + "\" is currently not supported. You  may however just add it to the "
                            + getClass().getCanonicalName() + " class, if you have access to it.");
            }
            return ret;
        }
    }
}
