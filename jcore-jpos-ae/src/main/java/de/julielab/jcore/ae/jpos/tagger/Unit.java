/**
 * Unit.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 *
 * Current version: 2.3
 * Since version:   2.2
 *
 * Creation date: Dec 7, 2006
 *
 *
 **/

package de.julielab.jcore.ae.jpos.tagger;

public class Unit {

	public int begin;

	public int end;

	private String rep;

	private String label = null;

	public Unit(final int begin, final int end, final String rep,
			final String label) {
		this(begin, end, rep);
		this.label = label;
	}

	public Unit(final int begin, final int end, final String rep) {
		this.begin = begin;
		this.end = end;
		this.rep = rep;
		label = "";
	}

	@Override
	public String toString() {
		final String ret = rep + ": " + begin + "-" + end + "(" + label + ")";
		return ret;
	}

	public String getRep() {
		return rep;
	}

	public String getLabel() {
		return label;
	}

	public void setRep(final String rep) {
		this.rep = rep;
	}

	public void setLabel(final String label) {
		this.label = label;
	}
}
