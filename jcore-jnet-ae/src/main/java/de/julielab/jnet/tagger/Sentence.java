/** 
 * Sentence.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: faessler
 * 
 * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: Nov 3, 2006 
 * 
 * This class is a kind of typedef for "ArrayList<Unit>". It is used for improved readability
 * of the source code that is, to avoid things like ArrayList<ArrayList<Unit>>.
 **/

package de.julielab.jnet.tagger;

import java.util.ArrayList;

public class Sentence {

	ArrayList<Unit> sentence;

	public Sentence(final ArrayList<Unit> sentence) {
		this.sentence = new ArrayList<Unit>(sentence);
	}

	public Sentence() {
		sentence = new ArrayList<Unit>();
	}

	public void add(final Unit unit) {
		sentence.add(unit);
	}

	public Unit get(final int i) {
		return sentence.get(i);
	}

	public ArrayList<Unit> getUnits() {
		return sentence;
	}

	public int size() {
		return sentence.size();
	}

	@Override
	public String toString() {
		return sentence.toString();
	}
}
