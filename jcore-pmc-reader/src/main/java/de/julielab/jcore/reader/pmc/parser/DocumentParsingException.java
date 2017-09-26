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

import de.julielab.jcore.reader.pmc.PmcReaderException;

public class DocumentParsingException extends PmcReaderException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4981222376109384961L;

	public DocumentParsingException() {
		super();
	}

	public DocumentParsingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DocumentParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public DocumentParsingException(String message) {
		super(message);
	}

	public DocumentParsingException(Throwable cause) {
		super(cause);
	}

}
