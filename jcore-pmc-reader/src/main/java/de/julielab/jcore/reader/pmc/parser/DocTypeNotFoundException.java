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

public class DocTypeNotFoundException extends DocumentParsingException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6650686582333820503L;

	public DocTypeNotFoundException() {
		super();
	}

	public DocTypeNotFoundException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DocTypeNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public DocTypeNotFoundException(String message) {
		super(message);
	}

	public DocTypeNotFoundException(Throwable cause) {
		super(cause);
	}

}
