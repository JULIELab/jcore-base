/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.bionlpformat.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OntoFormatReader {

	final String sectionRegEx = "\\s*\\[\\s*(\\w+)\\s*\\]\\s*"; 
	final String commentRegEx = "\\s*#\\s*.*";
	
	final Pattern sectionPattern = Pattern.compile(sectionRegEx);
	final Pattern commentPattern = Pattern.compile(commentRegEx, Pattern.DOTALL);

	BufferedReader ontoReader;
	
	public void readFile(File ontoFile) throws IOException {
		ontoReader = new BufferedReader(new FileReader(ontoFile));

		String section = "";
		String line = null;
		while ( (line = ontoReader.readLine()) != null ) {
			if ( commentPattern.matcher(line).matches() ) {
				continue;
			}
			else if ( line.trim().isEmpty() ) {
			    continue;
			}
			else {
				Matcher sectionMatcher =  sectionPattern.matcher(line);
				if ( sectionMatcher.matches() ) {
					section = sectionMatcher.group(1);
				}
				else {
					if ( section.equals("entities") ) {
						System.out.println(line);
					}
					else if ( section == "relations" ) {
						
					}
					else if ( section == "events" ) {
						
					}
					else if ( section == "attributes" ) {
						
					}
				}
			}
		}
	}
}
