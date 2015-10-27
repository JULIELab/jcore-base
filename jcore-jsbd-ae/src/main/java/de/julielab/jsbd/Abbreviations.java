/** 
 * Abbreviations.java
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
 * List of abbreviations.
 **/

package de.julielab.jsbd;

import java.util.TreeSet;

class Abbreviations {

	private TreeSet<String> abbr;

	Abbreviations() {
		init();
	}

	private void init() {
		abbr = new TreeSet<String>();

		abbr.add("cv.");
		abbr.add("(approx.");
		abbr.add("approx.");
		abbr.add("Dr.");
		abbr.add("(e.g.");
		abbr.add("e.g.");
		abbr.add("(i.e.");
		abbr.add("i.e.");
		abbr.add("sp.");
		abbr.add("spp.");
		abbr.add("pmol.");
		abbr.add("Biol.");
		abbr.add("Biosci.");
		abbr.add("Biotech.");
		abbr.add("Rev.");
		abbr.add("Heynh.");
		abbr.add("vs.");
		abbr.add("subsp.");
		abbr.add("Ltd.");
		abbr.add("etc.");
		abbr.add("mol.");
		abbr.add("viz.");
		abbr.add("St.");
		abbr.add("wt.");
		abbr.add("ca.");
		abbr.add("s.c.");
		abbr.add("i.v.");
		abbr.add("Molec.");
		abbr.add("Ed.");
		abbr.add("Eds.");

	}

	TreeSet<String> getSet() {
		return abbr;
	}
}
