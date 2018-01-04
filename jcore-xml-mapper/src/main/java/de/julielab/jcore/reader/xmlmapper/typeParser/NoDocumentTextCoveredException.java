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
package de.julielab.jcore.reader.xmlmapper.typeParser;

/**
 * This exception expresses the case where an annotation should cover a part of
 * the document text, but the referenced part is empty. The exception is used to
 * communicate this fact to the calling method rather then actually reporting an
 * error.
 * 
 * @author faessler
 * 
 */
public class NoDocumentTextCoveredException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -656035132655571153L;

}
