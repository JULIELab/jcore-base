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

package de.julielab.jcore.ae.jnet.tagger;

import java.util.HashMap;
import java.util.Set;

public class Unit {

	public int begin;

	public int end;

	private String rep;

	private String label = null;

	private HashMap<String, String> metaInfo = null;

	private double confidence = -1;

	public Unit(final int begin, final int end, final String rep,
			final String label, final HashMap<String, String> metas) {
		this(begin, end, rep, label);
		metaInfo.putAll(metas);
	}

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
		metaInfo = new HashMap<String, String>();
	}

	@Override
	public String toString() {
		String ret = rep + ": " + begin + "-" + end + "(" + label + ")";
		final Set<String> keySet = metaInfo.keySet();
		for (final Object key : keySet)
			ret += ", " + (String) key + ": " + metaInfo.get(key);
		return ret;
	}

	public String getMetaInfo(final String key) {
		return metaInfo.get(key);
	}

	public String getRep() {
		return rep;
	}

	public String getLabel() {
		return label;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setRep(final String rep) {
		this.rep = rep;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public void setConfidence(final double conf) {
		confidence = conf;
	}
}
