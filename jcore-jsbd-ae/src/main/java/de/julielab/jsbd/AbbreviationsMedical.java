package de.julielab.jsbd;

import java.util.TreeSet;

public class AbbreviationsMedical {
	public TreeSet<String> abbr;

	public AbbreviationsMedical() {
		init();
	}

	private void init() {
		abbr = new TreeSet<String>();

		abbr.add("Dr.");
		abbr.add("i.e.");
		abbr.add("I.E.");
		abbr.add("vs.");
		abbr.add("etc.");
		abbr.add("mol.");
		abbr.add("ca.");
		abbr.add("s.c.");
		abbr.add("i.v.");
		abbr.add("o.g.");
		abbr.add("gtt.");
		abbr.add("restl.");
		abbr.add("tgl.");
		abbr.add("bzgl.");
	}

	public TreeSet<String> getSet() {
		return abbr;
	}
}
