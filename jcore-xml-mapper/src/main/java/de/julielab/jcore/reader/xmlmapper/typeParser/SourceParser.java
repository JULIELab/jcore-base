/** 
 * SourceParser.java
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
 * Creation date: 28.11.2008 
 **/

package de.julielab.jcore.reader.xmlmapper.typeParser;

import org.apache.uima.jcas.JCas;

import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteFeature;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.StandardTypeBuilder;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;

/**
 * Handels to parse a Source Feature from a TypeTemplate to a ConcreteType
 * 
 * @author weigel
 */
public class SourceParser implements TypeParser {

	public TypeBuilder getTypeBuilder() {
		return new StandardTypeBuilder();
	}

	public void parseType(ConcreteType concreteType, VTDNav vn, JCas jcas, byte[] identifier, DocumentTextData docText) throws Exception {
		ConcreteFeature c = (ConcreteFeature)(concreteType);
		c.setValue(new String(identifier));
	}
}

