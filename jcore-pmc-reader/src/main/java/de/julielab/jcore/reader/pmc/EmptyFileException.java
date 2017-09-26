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
package de.julielab.jcore.reader.pmc;

public class EmptyFileException extends PmcReaderException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5873712917804969811L;

	public EmptyFileException() {
		super();
	}

	public EmptyFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EmptyFileException(String message, Throwable cause) {
		super(message, cause);
	}

	public EmptyFileException(String message) {
		super(message);
	}

	public EmptyFileException(Throwable cause) {
		super(cause);
	}

}
