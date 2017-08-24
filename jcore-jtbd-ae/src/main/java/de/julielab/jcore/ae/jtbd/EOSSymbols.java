/**
 * EOSSymbols.java
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
 * A list of end-of-sentence symbols.
 **/

package de.julielab.jcore.ae.jtbd;

import java.util.HashSet;

public class EOSSymbols {

	public static boolean contains(final Character c) {
		return symbols.contains(c);
	}

	private static void init() {
		symbols.add('.');
		symbols.add(':');
		symbols.add('!');
		symbols.add('?');
		symbols.add(']');
		symbols.add(')');
		symbols.add('"');
	}

	private static final HashSet<Character> symbols = new HashSet<Character>();

	//static initialization block, executed while class is loaded
	static {
		init();
	}
}
