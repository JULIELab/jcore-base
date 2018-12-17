/**
 * TokenBoundarySymbols.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 *
 * Current version: 1.6 Since version: 1.0
 *
 * Creation date: Aug 01, 2006
 *
 * This class holds a list of symbols where the tokenizer checks for possible
 * token boundaries.
 *
 **/

package de.julielab.jcore.ae.jtbd;

import java.util.HashSet;
import java.util.Set;

class TokenBoundarySymbols {

	static Set<String> getSymbols() {
		return tbSymbols;
	}

	private static void init() {
		tbSymbols.add("-");
		tbSymbols.add("+");
		tbSymbols.add("x");

		tbSymbols.add("?");
		tbSymbols.add("!");

		tbSymbols.add(">");
		tbSymbols.add("<");

		tbSymbols.add(".");
		tbSymbols.add(",");
		tbSymbols.add(";");
		tbSymbols.add(":");
		tbSymbols.add("=");

		tbSymbols.add("/");
		tbSymbols.add("\\");

		tbSymbols.add("\"");
		tbSymbols.add("'");
		tbSymbols.add("%");
		tbSymbols.add("&");

		tbSymbols.add("(");
		tbSymbols.add(")");
		tbSymbols.add("[");
		tbSymbols.add("]");
		tbSymbols.add("{");
		tbSymbols.add("}");
		
		// lower German quotation mark
		tbSymbols.add("\u201E");
		// upper German quotation mark 
		tbSymbols.add("\u201D");
	}

	private final static Set<String> tbSymbols = new HashSet<String>();

	//static initialization block, executed while class is loaded
	static {
		init();
	}
}
