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
package de.julielab.jcore.ae.jnet.uima;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeSet;

public class NegativeList {

	private final static String DELIM = "@";

	private TreeSet<String> negativeList;

	public NegativeList(final File myFile) throws IOException {
		this(new FileInputStream(myFile));
	}
	
	public NegativeList(final InputStream is) throws IOException {
		init(is);
	}

	/**
	 * reads the negative list from a file and stored the entries in a set
	 * 
	 * @param myFile
	 */
	private void init(final InputStream is) throws IOException {
		negativeList = new TreeSet<String>();
		BufferedReader br;
		br = new BufferedReader(new InputStreamReader(is));
		String line = "";
		while ((line = br.readLine()) != null)
			negativeList.add(line);
	}

	/**
	 * checks whether an entity mention is contained in the negative list.
	 * 
	 * @param mentionText
	 *            the text covered by the entity annotation
	 * @param label
	 *            the label assigned to this entity annotation
	 * @return true if negative list contains mentionText (with label)
	 */
	public boolean contains(final String mentionText, final String label) {

		// check with label
		String searchString = mentionText + DELIM + label;
		if (negativeList.contains(searchString))
			return true;

		// check without label
		searchString = mentionText;
		if (negativeList.contains(searchString))
			return true;
		return false;
	}

}
