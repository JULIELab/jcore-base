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
