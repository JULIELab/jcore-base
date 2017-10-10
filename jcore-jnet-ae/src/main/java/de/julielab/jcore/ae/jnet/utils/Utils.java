/** 
 * IOUtils.java
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
 * Creation date: Nov 1, 2006 
 * 
 * Some more utils for JNET.
 * 
 **/

package de.julielab.jcore.ae.jnet.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;

import de.julielab.java.utilities.FileUtilities;

public class Utils {

	/**
	 * shuffles the contents of a file on a sentence level
	 * 
	 * @param inputFile
	 * @param outputFile
	 */
	public static void ShuffleFileContents(final File inputFile,
			final File outputFile) {
		final ArrayList<String> lines = readFile(inputFile);
		Collections.shuffle(lines);
		writeFile(outputFile, lines);
	}

	/**
	 * reads file into ArrayList. Each line is one element (String).
	 * 
	 * @param filename
	 *            full path
	 */
	public static ArrayList<String> readFile(final File filename) {
		final ArrayList<String> lines = new ArrayList<String>();
		try {
			final BufferedReader br = FileUtilities.getReaderFromFile(filename);
			try {
				String line = "";
				while ((line = br.readLine()) != null)
					lines.add(line);
				br.close();
			} catch (final IOException e) {
				System.err.println("Read error " + e);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return lines;
	}

	/**
	 * writes ArrayList into file. Here, we assume that each element of the
	 * ArrayList is a String, which we write as new line into the file.
	 * 
	 * @param filename
	 *            full path
	 */
	public static void writeFile(final File filename, final ArrayList<String> lines) {
		try {
			final Writer fw = FileUtilities.getWriterToFile(filename);
			for (int i = 0; i < lines.size(); i++)
				fw.write(lines.get(i) + "\n");
			fw.close();
		} catch (final IOException e) {
			System.err.println("error writing file");
			e.printStackTrace();
		}
	}

	public static void writeFile(final File filename, final String myString) {
		try {
			final Writer fw = FileUtilities.getWriterToFile(filename);
			fw.write(myString + "\n");
			fw.close();
		} catch (final IOException e) {
			System.err.println("error writing file");
			e.printStackTrace();
		}
	}

}
