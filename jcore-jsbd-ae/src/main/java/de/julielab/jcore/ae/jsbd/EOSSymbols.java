/** 
 * EOSSymbols.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 1.6	
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * A list of end-of-sentence symbols.
 **/

package de.julielab.jcore.ae.jsbd;

import java.util.TreeSet;

class EOSSymbols {

	private TreeSet<String> symbols;

	public EOSSymbols() {
		init();
	}

	private void init() {
		symbols = new TreeSet<String>();
		symbols.add(".");
		symbols.add(":");
		symbols.add("!");
		symbols.add("?");
		symbols.add("]");
		symbols.add(")");
		symbols.add("\"");
	}
	
	public TreeSet<String> getSymbols() {
		return symbols;
	}
	
	public boolean contains(String c) {
		return symbols.contains(c);
	}
	
	public boolean tokenEndsWithEOSSymbol(String token) {
		if (token.length()>0) {
			String lastChar = token.substring(token.length() - 1, token.length());
			if (symbols.contains(lastChar)) {
				return true;
			}
		} 
		return false;
	}

}
