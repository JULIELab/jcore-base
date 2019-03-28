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

package de.julielab.jcore.ae.jpos.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class Utils {

	/**
	 * shuffles the contents of a file on a sentence level
	 *
	 * @param inputFile
	 * @param outputFile
	 * @throws IOException
	 */
	public static void ShuffleFileContents(final File inputFile,
			final File outputFile) throws IOException {
		final List<String> lines = Files.readLines(inputFile, Charsets.UTF_8);
		Collections.shuffle(lines);
		writeFile(outputFile, lines);
	}

	/**
	 * writes ArrayList into file. Here, we assume that each element of the
	 * ArrayList is a String, which we write as new line into the file.
	 *
	 * @param filename
	 *            full path
	 */
	public static void writeFile(final File filename, final List<String> lines) {
		try {
			final FileWriter fw = new FileWriter(filename);
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
			final FileWriter fw = new FileWriter(filename);
			fw.write(myString + "\n");
			fw.close();
		} catch (final IOException e) {
			System.err.println("error writing file");
			e.printStackTrace();
		}
	}

}
