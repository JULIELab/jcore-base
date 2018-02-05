/** 
 * DBMedlineReader.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are protected. Please contact JULIE Lab for further information.  
 *
 * Author: landefeld
 * 
 * Current version: 1.0 	
 * Since version:   1.9
 *
 * Creation date: 15.09.2008 
 * 
 * An UIMA CollcetionReader that implements DBReader (a class that gets Documents by DB-based informations) using the MedlineMapper 
 **/

package de.julielab.jcore.reader.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import de.julielab.jcore.reader.db.DBReader;
import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import de.julielab.xml.JulieXMLConstants;

/**
 * An UIMA CollectionReader that implements DBReader (a class that gets
 * Documents by DB-based informations) using the XMLMapper
 * 
 * @author faessler/hellrich/landefeld
 */
public class XMLDBReader extends DBReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLDBReader.class);

	public static final String PARAM_ROW_MAPPING = "RowMapping";
	/**
	 * Configuration parameter defined in the descriptor
	 */
	public static final String PARAM_MAPPING_FILE = "MappingFile";
	/**
	 * Mapper which maps medline XML to a CAS with the specified UIMA type system
	 * via an XML configuration file.
	 */
	protected XMLMapper xmlMapper;
	protected int totalDocumentCount;
	protected int processedDocuments;

	protected static final int TYPE = 0;
	protected static final int FEATURE_AND_DATATYPE = 1;
	protected static final int FEATURE_DATATYPE = 2;
	protected static final int SETTER = 0;
	protected static final int ADD_TO_INDEXES = 1;
	protected LinkedHashMap<Integer, RowMapElement> rowMapping;
	protected Method addToIndexes;

	@ConfigurationParameter(name = PARAM_ROW_MAPPING)
	protected String[] rowMappingArray;
	@ConfigurationParameter(name = PARAM_MAPPING_FILE, mandatory = true)
	protected String mappingFileStr;

	protected static final byte[] comma = ",".getBytes();

	@Override
	public void initialize() throws ResourceInitializationException {
		super.initialize();

		mappingFileStr = (String) getConfigParameterValue(PARAM_MAPPING_FILE);
		InputStream is = null;

		File mappingFile = new File(mappingFileStr);
		if (mappingFile.exists()) {
			try {
				is = new FileInputStream(mappingFile);
			} catch (FileNotFoundException e1) {
				throw new ResourceInitializationException(e1);
			}
		} else {
			if (!mappingFileStr.startsWith("/"))
				mappingFileStr = "/" + mappingFileStr;

			is = getClass().getResourceAsStream(mappingFileStr);
			if (is == null) {
				throw new IllegalArgumentException("MappingFile " + mappingFileStr
						+ " could not be found as a file or on the classpath (note that the prefixing '/' is added automatically if not already present for classpath lookup)");
			}
		}

		try {
			xmlMapper = new XMLMapper(is);
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
		rowMappingArray = (String[]) getConfigParameterValue(PARAM_ROW_MAPPING);
		rowMapping = buildRowMapping(rowMappingArray);
		if (rowMapping != null)
			try {
				addToIndexes = TOP.class.getDeclaredMethod("addToIndexes");
			} catch (SecurityException e) {
				throw new ResourceInitializationException(e);
			} catch (NoSuchMethodException e) {
				LOGGER.error("Class \"" + TOP.class.getCanonicalName()
						+ "\" does not have an \"addToIndexes()\" method. Is this really the UIMA class?");
				throw new ResourceInitializationException(e);
			}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas
	 * .CAS)
	 */
	public void getNext(CAS cas) throws IOException, CollectionException {
		byte[][] arrayArray = getNextArtefactData(cas);
		List<Integer> pkIndices = dbc.getPrimaryKeyIndices();

		// get index of xmlData;
		// assumes that only one byte[] in arrayArray contains this data
		// and that this byte[] is at the only index position that holds no
		// primary key
		List<Integer> allIndices = new ArrayList<Integer>();
		for (int i = 0; i < arrayArray.length; i++) {
			allIndices.add(i);
		}
		List<Integer> xmlIndices = new ArrayList<>(allIndices);
		for (Integer pkIndex : pkIndices)
			xmlIndices.remove(pkIndex);
		int xmlIndex = xmlIndices.get(0);

		ArrayList<byte[]> primaryKey = new ArrayList<byte[]>();
		int lengthIdentifier = pkIndices.size() - 1;
		for (Integer index : pkIndices) {
			byte[] pkElementValue = arrayArray[index];
			primaryKey.add(pkElementValue);
			lengthIdentifier = lengthIdentifier + pkElementValue.length;
		}

		// build byte[] identifier out of primary key values, separated by
		// comma;
		// this identifier is used for method parse() in XMLMapper
		byte[] identifier = new byte[lengthIdentifier];
		int currentPosition = 0;
		for (int j = 0; j < primaryKey.size(); j++) {
			System.arraycopy(primaryKey.get(j), 0, identifier, currentPosition, primaryKey.get(j).length);
			currentPosition = currentPosition + primaryKey.get(j).length;
			if (j < primaryKey.size() - 1) {
				System.arraycopy(comma, 0, identifier, currentPosition, 1);
				currentPosition = currentPosition + 1;
			}
		}

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("getNext(CAS), primaryKeyValue = {}", new String(identifier));
		try {
			xmlMapper.parse(arrayArray[xmlIndex], identifier, cas.getJCas());
			// Are there additional rows besides the primary key columns and the
			// document XML?
			if (arrayArray.length > (pkIndices.size() + 1)) {
				if (null == rowMapping || rowMapping.size() < (xmlIndices.size() - 1)) {
					throw new NullPointerException("There are elements in the returned array that cannot"
							+ " be mapped to UIMA type classes. Row mapping: " + rowMapping);
				} else {
					mapRowToType(arrayArray, cas.getJCas());
				}
			}
			setDBProcessingMetaData(arrayArray, cas);
		} catch (Exception e) {
			LOGGER.error("getNext(CAS), primaryKeyValue = " + new String(identifier), e);
			throw new CollectionException(e);
		} catch (Throwable e) {
			throw new CollectionException(e);
		}
	}

	/**
	 * @param arrayArray
	 * @throws CollectionException
	 */
	private void mapRowToType(byte[][] arrayArray, JCas jcas) throws CollectionException {

		// Temporary map to cache already created UIMA type objects which could
		// be referenced multiple times (for multiple features of the same type,
		// for example).
		Map<String, TOP> typeObjects = new HashMap<>();
		Set<Annotation> typesToAddToIndexes = new HashSet<Annotation>();

		for (Entry<Integer, RowMapElement> entry : rowMapping.entrySet()) {
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
					List<Map<String, Object>> allRetrievedColumns = getAllRetrievedColumns();
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

	@Override
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(processedDocuments, totalDocumentCount, Progress.ENTITIES, true) };
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

	@Override
	protected String getReaderComponentName() {
		return getClass().getSimpleName();
	}

}
