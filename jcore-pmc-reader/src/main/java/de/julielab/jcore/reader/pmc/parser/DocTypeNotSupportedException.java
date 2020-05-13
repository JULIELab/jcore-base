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
package de.julielab.jcore.reader.pmc.parser;

public class DocTypeNotSupportedException extends DocumentParsingException {

	/**
	 *
	 */
	private static final long serialVersionUID = -6650686582333820503L;

	public DocTypeNotSupportedException() {
		super();
	}

	public DocTypeNotSupportedException(String message, Throwable cause, boolean enableSuppression,
										boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DocTypeNotSupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DocTypeNotSupportedException(String message) {
		super(message);
	}

	public DocTypeNotSupportedException(Throwable cause) {
		super(cause);
	}

}
