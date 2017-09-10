/** 
 * StringArrayBuilder.java
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
 * Creation date: 13.11.2008 
 **/

package de.julielab.jcore.reader.xmlmapper.typeBuilder;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;


/**
 * The Builder for String Arrays. Uses Reflection to build A StringArray from a ConcreteType
 * 
 * @author weigel
 */
public class StringArrayBuilder implements TypeBuilder {

	public TOP buildType(ConcreteType gtp, JCas jcas) throws CollectionException {
		StringArray stringArray = new StringArray(jcas,gtp.getConcreteFeatures().size());
		
		for (int i = 0; i < gtp.getConcreteFeatures().size(); i++) {
			stringArray.set(i, gtp.getConcreteFeatures().get(i).getValue());
		}
		return stringArray;
	}
}

