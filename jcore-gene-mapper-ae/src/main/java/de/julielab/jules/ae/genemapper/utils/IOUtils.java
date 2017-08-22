/** 
 * IOUtils.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: //TODO insert current version number 	
 * Since version:   //TODO insert version number of first appearance of this class
 *
 * Creation date: Jan 28, 2008 
 * 
 * //TODO insert short description
 **/

package de.julielab.jules.ae.genemapper.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class IOUtils {

	public static ArrayList<String> readFile2ArrayList(File myFile)
			throws FileNotFoundException, IOException {
		ArrayList<String> tmp = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(myFile));
		String line = "";
		while ((line = br.readLine()) != null) {
			tmp.add(line);
		}
		br.close();
		return tmp;
	}

	public static String readFile2String(File myFile)
			throws FileNotFoundException, IOException {
		StringBuffer buffer = new StringBuffer();
		BufferedReader br = new BufferedReader(new FileReader(myFile));
		String line = "";
		while ((line = br.readLine()) != null) {
			buffer.append(line + " ");
		}
		br.close();
		return buffer.toString().trim();
	}

}
