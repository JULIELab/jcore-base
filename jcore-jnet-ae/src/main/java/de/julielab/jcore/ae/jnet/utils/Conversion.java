/** 
 * Conversion.java
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
 * Creation date: Aug 01, 2006 
 * 
 * Some conversion utilities.
 * 
 **/

package de.julielab.jcore.ae.jnet.utils;

import java.util.ArrayList;

public class Conversion {

	/**
	 * converts PipedFormat into IOB format (POS tags are omitted)
	 * 
	 * @param sentences
	 *            ArrayList in PipedFormat
	 * @return ArrayList in IOB Format
	 */
	public static ArrayList<String> convertIOB(final ArrayList<?> sentences) {
		final ArrayList<String> iob = new ArrayList<String>();

		for (int i = 0; i < sentences.size(); i++) {
			final String sentence = (String) sentences.get(i);
			final String[] tokens = sentence.trim().split("[\t ]+");

			for (final String token : tokens) {
				final String[] features = token.split("\\|\\|");
				if (features.length != 2) {
					System.err.println("err: mal-formatted data");
					System.exit(0);
				}
				final String word = features[0].split("\\|")[0];
				final String label = features[1];

				iob.add(word + "\t" + label);
			}

			iob.add("O" + "\t" + "O"); // empty line
		}

		// for (int i = 0; i < iob.size(); i++)
		// System.out.println (iob.get(i));

		return iob;
	}

}
