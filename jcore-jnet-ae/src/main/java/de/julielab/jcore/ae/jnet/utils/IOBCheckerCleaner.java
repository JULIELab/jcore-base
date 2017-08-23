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
