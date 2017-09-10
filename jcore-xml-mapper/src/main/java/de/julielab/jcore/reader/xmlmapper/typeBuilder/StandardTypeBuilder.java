/** 
 * StandardTypeBuilder.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: bernd
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 03.11.2008 
 **/
package de.julielab.jcore.reader.xmlmapper.typeBuilder;

import static org.fest.reflect.core.Reflection.constructor;
import static org.fest.reflect.core.Reflection.method;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteFeature;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;

/**
 * In this class, the actual UIMA types are built from the templates which have
 * been filled with values by the type parsers before. The standard type builder
 * uses reflection to build a Type of the typesystem from a ConcreteType
 * 
 * @author weigel
 */
public class StandardTypeBuilder implements TypeBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(StandardTypeBuilder.class);
	private HashMap<String, Class<?>> standardJavaTypesMap;

	/**
	 * creates an new instance of the StandardTypeBuilder
	 */
	public StandardTypeBuilder() {
		initStandardTypes();
	}

	private void initStandardTypes() {
		// TODO liste einfacher Typen vervollständigen
		standardJavaTypesMap = new HashMap<String, Class<?>>();
		standardJavaTypesMap.put("char", char.class);
		standardJavaTypesMap.put("int", int.class);
		standardJavaTypesMap.put("boolean", boolean.class);
		standardJavaTypesMap.put("float", float.class);
		standardJavaTypesMap.put("long", long.class);
		standardJavaTypesMap.put("double", double.class);
		standardJavaTypesMap.put("byte", byte.class);
	}

	/**
	 * Instantiates a UIMA type object belonging to the class given by
	 * {@link ConcreteType#getFullClassName()}.
	 * <p>
	 * All the type's features are set appropriately to their values given by
	 * {@link ConcreteType#getConcreteFeatures()}.
	 * 
	 * @param concreteType
	 *            A <code>ConcreteType</code> object holding all information
	 *            required to instantiate an actual object of the UIMA type
	 *            represented by this object.
	 * @param jcas
	 *            The <code>JCas</code> object the returned
	 *            <code>Annotation</code> object should be added to.
	 * @return A UIMA <code>Annotation</code> object of the class given by
	 *         {@link ConcreteType#getFullClassName()} with all features and
	 *         their values given by {@link ConcreteType#getConcreteFeatures()}.
	 */
	public Annotation buildType(ConcreteType concreteType, JCas jcas) throws CollectionException {
		if (concreteType.getTypeTemplate().isMultipleInstances() || concreteType.getTypeTemplate().isInlineAnnotation()) {
			for (ConcreteFeature concreteFeature : concreteType.getConcreteFeatures()) {
				this.buildType(concreteFeature, jcas);
			}
			return null;
		} else {
			return buildSingleInstance(concreteType, jcas);
		}
	}

	/**
	 * Builds an actual UIMA type from a ConcreteType object which holds all
	 * values for the UIMA type.
	 * 
	 * @param concreteType
	 *            The wrapper object for the type template to create a real UIMA
	 *            type from.
	 * @param jcas
	 *            The JCas to which the created UIMA type should be added.
	 * @return The built type. Returns <code>null</code> if
	 *         <code>concreteType</code> does not define any features.
	 * @throws CollectionException
	 */
	private Annotation buildSingleInstance(ConcreteType concreteType, JCas jcas) throws CollectionException {
		if (concreteType.getFullClassName() == null) {
			// this will happen at special cases like the documentText
			concreteType.getTypeTemplate().getParser().getTypeBuilder().buildType(concreteType, jcas);
			return null;
		}
		Class<?> typeClass = null;
		Annotation type = null;
		try {
			typeClass = Class.forName(concreteType.getFullClassName());
		} catch (ClassNotFoundException e) {
			// FIXME auto generatet catch block
			e.printStackTrace();
		}
		// Has this type any features at all?
		if (concreteType.getConcreteFeatures() != null) {
			// Create the UIMA type corresponding to the type description in
			// concreteType.
			type = (Annotation) constructor().withParameterTypes(JCas.class).in(typeClass).newInstance(jcas);

			// For each feature this type has, set the corret feature value.
			for (ConcreteFeature concreteFeature : concreteType.getConcreteFeatures()) {
				if ((concreteFeature.getValue() == null || concreteFeature.getValue().equals("")) && !concreteFeature.isType()) {
					continue;
				}
				Class<?> featureClass;
				try {
					// Get the setter for the feature value, e.g.
					// 'setSpecificType'.
					// The setter convention say that the method's name is
					// prefixed by 'set'. Then,
					// the name of the feature is appended with the first
					// character in upper case.
					String methodName = "set" + concreteFeature.getTsName().substring(0, 1).toUpperCase() + concreteFeature.getTsName().substring(1);

					// Now set the actual value for the feature. We have to
					// determine the data type
					// of the feature's value. The primitive types are found in
					// 'standardJavaTypesMap'
					// (see above). The String is a special case. If the value
					// is neither a Java
					// primitive nor a String, we expect it to be an UIMA type
					// itself.
					if (standardJavaTypesMap.get(concreteFeature.getFullClassName()) != null) {
						featureClass = standardJavaTypesMap.get(concreteFeature.getFullClassName());
						method(methodName).withParameterTypes(featureClass).in(type)
								.invoke(parseValueStringToValueType(concreteFeature.getValue(), concreteFeature.getFullClassName()));
					} else if (concreteFeature.getFullClassName().equals("String") || concreteFeature.getFullClassName().equals("java.lang.String")) {
						featureClass = Class.forName(concreteFeature.getFullClassName());
						method(methodName).withParameterTypes(featureClass).in(type).invoke(concreteFeature.getValue());
					} else {
						String featureClassName = concreteFeature.getFullClassName();
						if (StringUtils.isBlank(featureClassName))
							throw new IllegalStateException("For the feature \"" + concreteFeature.getTsName() + "\" of the type \""
									+ concreteType.getFullClassName()
									+ "\" the feature value class (e.g. String, Integer, another type...) was not defined in the mapping file.");
						featureClass = Class.forName(featureClassName);
						TOP top = concreteFeature.getTypeTemplate().getParser().getTypeBuilder().buildType(concreteFeature, jcas);
						method(methodName).withParameterTypes(featureClass).in(type).invoke(top);
					}
				} catch (Throwable e) {
					LOGGER.error("Wrong Feature Type: " + concreteFeature.getFullClassName(), e);
					throw new CollectionException(UIMAException.STANDARD_MESSAGE_CATALOG, null);
				}
			}
			type.setBegin(concreteType.getBegin());
			type.setEnd(concreteType.getEnd());
			type.addToIndexes();
		} else
			LOGGER.warn("Type " + concreteType.getFullClassName() + " does not define any features and is omitted.");
		return type;
	}

	private Object parseValueStringToValueType(String value, String type) {
		if (type.equals("boolean")) {
			return Boolean.parseBoolean(value);
		} else if (type.equals("int")) {
			return Integer.parseInt(value);
		} else if (type.equals("long")) {
			return Long.parseLong(value);
		} else if (type.equals("float")) {
			return Float.parseFloat(value);
		} else if (type.equals("double")) {
			return Double.parseDouble(value);
		} else if (type.equals("byte")) {
			return Byte.parseByte(value);
		} else if (type.equals("char")) {
			return value.charAt(0);
		}
		return null;
	}
}
