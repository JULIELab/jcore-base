/** 
 * IOUtils.java
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
 * Creation date: Nov 1, 2006 
 * 
 * Some more utils for JNET.
 * 
 **/
package de.julielab.coordination.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

public class Utils {

	/**
	 * shuffles the contents of a file on a sentence level
	 * 
	 * @param inputFile
	 * @param outputFile
	 */
	public static void ShuffleFileContents(File inputFile, File outputFile) {
		ArrayList<String> lines = readFile(inputFile);
		Collections.shuffle(lines);
		writeFile(outputFile, lines);
	}

	/**
	 * reads file into ArrayList. Each line is one element (String).
	 * 
	 * @param filename
	 *            full path
	 */
	public static ArrayList<String> readFile(File filename) {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			try {
				String line = "";
				while ((line = br.readLine()) != null) {
					lines.add(line);
				}
				br.close();
			} catch (IOException e) {
				System.err.println("Read error " + e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lines;
	}

	/**
	 * writes ArrayList into file. Here, we assume that each element of the ArrayList is a String,
	 * which we write as new line into the file.
	 * 
	 * @param filename
	 *            full path
	 */
	public static void writeFile(File filename, ArrayList lines) {
		try {
			FileWriter fw = new FileWriter(filename);
			for (int i = 0; i < lines.size(); i++)
				fw.write((String) lines.get(i) + "\n");
			fw.close();
		} catch (IOException e) {
			System.err.println("error writing file");
			e.printStackTrace();
		}
	}

	public static void writeFile(File filename, String myString) {
		try {
			FileWriter fw = new FileWriter(filename);
			fw.write(myString + "\n");
			fw.close();
		} catch (IOException e) {
			System.err.println("error writing file");
			e.printStackTrace();
		}
	}

	/**
	 * The Properties Object featureConfig contains key-value pairs. Some of these pairs correspond
	 * to information about meta datas. This method returns a list of the used meta datas by
	 * searching for pairs of the form "xxx_feat_enabled = true". It then adds the "xxx" to the
	 * list. For "pos_feat_enabled = true" it will add pos to the list. For "chunk_feat_enabled =
	 * false" it will do nothing.
	 */
	public static String[] getTrueMetas(Properties featureConfig) {
		ArrayList<String> trueMetas = new ArrayList<String>();
		Enumeration keys = featureConfig.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.matches("[a-zA-Z]+_feat_enabled") && featureConfig.getProperty(key).equals("true")) {
				trueMetas.add(key.substring(0, key.indexOf("_feat_enabled")));
			}
		}
		String[] ret = new String[trueMetas.size()];
		trueMetas.toArray(ret);
		return ret;
	}
}
