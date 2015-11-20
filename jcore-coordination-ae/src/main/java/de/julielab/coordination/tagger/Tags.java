/** 
 * Tags.java
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
 * Creation date: Dec 1, 2006 
 * 
 **/

package de.julielab.coordination.tagger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import edu.umass.cs.mallet.base.types.LabelAlphabet;

public class Tags {

	/**
	 * use IO or IOB format i.e. there must be an O label and for each class
	 * either an I label (IO) of an I and an B label !!
	 */

	private static ArrayList<String> tags;

	public String type = "IO"; // IO or IOB

	/**
	 * the constructor.
	 * 
	 * @param filename
	 *            full path to file in which the tags are stored. One tag each
	 *            line. Make sure, there is also an outside tag "O". Further you
	 *            should check, that either there is always a begin tag "B-" or
	 *            never, as this is not counterchecked by this module.
	 */
	public Tags(String filename) {
		tags = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				if (line.length() > 0) {
					String tag = line.substring(0, 1);
					// set type to IOB if there is at least on B-tag
					// furthermore assume the format to be consistent in its
					// type (IO vs. IOB)
					if (tag.equals("B"))
						type = "IOB";
					tags.add(line);
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * gets you the LabelAlphabet representation of the tags.
	 */
	public LabelAlphabet getAlphabet() {
		LabelAlphabet dict = new LabelAlphabet();
		for (int i = 0; i < tags.size(); i++) {
			String label = (String) tags.get(i);
			dict.lookupLabel(label, true);
		}
		return dict;
	}

	/**
	 * gets you the all the labels as a string array.
	 */
	public String[] getTags() {
		String taglist[] = (String[]) tags.toArray(new String[tags.size()]);
		return taglist;
	}

	public int nrTags() {
		return tags.size();
	}

	public boolean contains(String label) {
		if (tags.contains(label))
			return true;
		else
			return false;
	}
}
