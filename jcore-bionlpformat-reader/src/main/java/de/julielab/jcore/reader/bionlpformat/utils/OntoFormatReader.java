/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
