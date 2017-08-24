/** 
 * TypeBuilder.java
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

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;


/**
 * TODO insert description
 * @author weigel
 */
public interface TypeBuilder {
	/**
	 * builds a concrete Type to a subtype of TOP, by using reflection 
	 * 
	 * @param concreteType
	 * @param jcas
	 * @return a subtype of TOP
	 * @throws CollectionException
	 */
	public TOP buildType(ConcreteType concreteType,JCas jcas) throws CollectionException;
}

