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

public class ElementParsingException extends PmcReaderException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5579915605927261987L;

	public ElementParsingException() {
		super();
	}

	public ElementParsingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ElementParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ElementParsingException(String message) {
		super(message);
	}

	public ElementParsingException(Throwable cause) {
		super(cause);
	}

}
