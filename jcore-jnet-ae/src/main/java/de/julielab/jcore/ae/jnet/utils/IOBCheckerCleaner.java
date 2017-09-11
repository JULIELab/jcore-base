/** 
 * IOBCheckerCleaner.java
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
 * Creation date: Nov 12, 2008 
 * 
 * 
 **/

package de.julielab.jcore.ae.jnet.utils;

import java.io.File;
import java.util.ArrayList;

/**
 * 
 * checks and cleans IOB files: makes sure that there aren't multiple empty
 * lines, several spaces are replaces by tabs, and it is checked whether every
 * token has its label
 * 
 * @author tomanek
 */
public class IOBCheckerCleaner {

	public static void main(final String[] args) {

		if (args.length != 2) {
			System.out
					.println("usage: java IOBCheckerCleaner <iobFile in> <iobFile out>");
			System.exit(-1);
		}

		final File inFile = new File(args[0]);
		final File outFile = new File(args[1]);

		final ArrayList<String> iobData = Utils.readFile(inFile);
		final ArrayList<String> outData = new ArrayList<String>();

		String lastLine = "";
		int pos = 0;
		for (String line : iobData) {
			line = line.trim();
			line = line.replaceAll("[\\s]+", "\t");

			// if we have several empty lines in a row
			if ((pos > 0) && lastLine.equals("") && line.equals("")) {
				System.err.println("consecutive empty lines in line: " + pos);
				System.exit(-1);
			}

			// check for missing tags
			if (!line.equals("") && (line.split("\t").length != 2)) {
				System.err.println("incorrect line: " + line);
				System.exit(-1);
			}

			lastLine = line;
			pos++;
			outData.add(line);
		}

		System.out.println("file OK... writing to: " + outFile);
		Utils.writeFile(outFile, outData);

	}

}
