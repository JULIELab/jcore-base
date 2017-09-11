/** 
 * MedlineTextSentenceBuilder.java
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
 * Creation date: 21.03.2009 
 **/

package de.julielab.jcore.reader.xmlmapper.typeBuilder;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteFeature;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;


/**
 * TODO insert description
 * @author bernd
 */
public class MedlineTextSentenceBuilder implements TypeBuilder {

	public TOP buildType(ConcreteType concreteType, JCas jcas) throws CollectionException {
		StandardTypeBuilder standardBuilder = new StandardTypeBuilder();
		for(ConcreteFeature concreteFeature:concreteType.getConcreteFeatures()){
			standardBuilder.buildType(concreteFeature, jcas);
		}
		return null;
	}
}

