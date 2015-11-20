/** 
 * Unit.java
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
 * Creation date: Dec 7, 2006 
 * 
 * 
 **/

package de.julielab.coordination.tagger;

import java.util.HashMap;
import java.util.Set;

public class Unit {

	public int begin;

	public int end;

	private String rep;

	private String label = null;

	private HashMap<String, String> metaInfo = null;

	private double confidence = -1;
	
	public Unit(int begin, int end, String rep, String label,
			HashMap<String, String> metas) {
		this(begin, end, rep, label);
		metaInfo.putAll(metas);
	}

	public Unit(int begin, int end, String rep, String label) {
		this(begin, end, rep);
		this.label = label;
	}

	public Unit(int begin, int end, String rep) {
		this.begin = begin;
		this.end = end;
		this.rep = rep;
		this.label = "";
		metaInfo = new HashMap<String, String>();
	}

	public String toString() {
		String ret = rep + ": " + begin + "-" + end + "(" + label + ")";
		Set keySet = metaInfo.keySet();
		for (Object key : keySet) {
			ret += ", " + (String) key + ": " + metaInfo.get((String) key);
		}
		return ret;
	}

	public String getMetaInfo(String key) {
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
	
	public void setRep(String rep) {
		this.rep = rep;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public void setConfidence(double conf) {
		this.confidence = conf;
	}
}
