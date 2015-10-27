/** 
 * FSArrayTypeBuilder.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
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
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;


/**
 * The builder fotr FSArrays. Uses Reflection to build a FSArra Type 
 * @author Weigel
 */
public class FSArrayBuilder implements TypeBuilder {

	public TOP buildType(ConcreteType gtp, JCas jcas) throws CollectionException {
		int size = gtp.getConcreteFeatures().size();
		FSArray fsArray = new FSArray(jcas,size);
		for (int j = 0; j < gtp.getConcreteFeatures().size(); j++) {
			TOP top = gtp.getConcreteFeatures().get(j).getTypeTemplate().getParser().getTypeBuilder().buildType(gtp.getConcreteFeatures().get(j), jcas);
			fsArray.set(j, top);
		}
		return fsArray;
	}
}

