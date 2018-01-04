/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.xmlmapper.mapper;

import java.util.List;

import org.apache.uima.jcas.JCas;

import com.ximpleware.VTDException;
import com.ximpleware.VTDNav;

public interface DocumentTextPartParser {
	List<String> parseDocumentPart(VTDNav vn, PartOfDocument docTextPart, int offset, JCas jCas, byte[] identifier) throws VTDException;
}
