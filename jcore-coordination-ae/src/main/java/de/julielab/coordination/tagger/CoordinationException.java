/** 
 * JNETExcpeption.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: Nov 1, 2006 
 * 
 * Default Exception for JNET.
 **/

package de.julielab.coordination.tagger;

public class CoordinationException extends Exception {

	final static long serialVersionUID = 23;

	public CoordinationException() {
		super();
	}

	public CoordinationException(String s) {
		super(s);

	}
}
