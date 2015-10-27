/** 
 * TypeParser.java
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
 * Creation date: 02.11.2008 
 **/
package de.julielab.jcore.reader.xmlmapper.typeParser;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;

import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;

/**
 * standard Interface to define an external Parser
 * 
 * @author weigel
 */
public interface TypeParser {

	/**
	 * @return an instance of the TypeBuilder class. Wheter the Type Need special handling or not.
	 *         if not just return a new instance of the StandardTypeBuilder
	 */
	public TypeBuilder getTypeBuilder();

	/**
	 * Parses a Type. Gather all necessary Infomations from the vdtnav, and fill the concrete Type.
	 * The corresponding TypeTemplate is part of the ConcreteType
	 * 
	 * @param concreteType
	 * @param xpath
	 * @param vn
	 * @param String identifier
	 * @param DocumentTextData docText
	 * @throws Exception 
	 * @throws CollectionException
	 */
	public void parseType(ConcreteType concreteType, VTDNav vn, JCas jcas, byte[] identifier, DocumentTextData docText) throws Exception;

}
