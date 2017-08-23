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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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
			final BufferedReader br = new BufferedReader(new FileReader(
					filename));
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
