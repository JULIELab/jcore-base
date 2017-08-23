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
package de.julielab.jnet.tagger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import cc.mallet.types.LabelAlphabet;

public class Tags {

	/**
	 * use IO or IOB format i.e. there must be an O label and for each class
	 * either an I label (IO) of an I and an B label !!
	 */

	private final ArrayList<String> tags;

	public String type = "IO"; // IO or IOB

	/**
	 * the constructor.
	 * 
	 * @param filename
	 *            full path to file in which the tags are stored. One tag each
	 *            line. Make sure, there is also an outside tag "O". Further you
	 *            should check, that either there is always a begin tag "B-" or
	 *            never, as this is not counterchecked by this module.
	 */
	public Tags(final String filename) {
		tags = new ArrayList<String>();
		try {
			final BufferedReader br = new BufferedReader(new FileReader(
					filename));
			String line = "";
			while ((line = br.readLine()) != null)
				if (line.length() > 0) {
					final String tag = line.substring(0, 1);
					// set type to IOB if there is at least on B-tag
					// furthermore assume the format to be consistent in its
					// type (IO vs. IOB)
					if (tag.equals("B"))
						type = "IOB";
					tags.add(line);
				}
			br.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * gets you the LabelAlphabet representation of the tags.
	 */
	public LabelAlphabet getAlphabet() {
		final LabelAlphabet dict = new LabelAlphabet();
		for (int i = 0; i < tags.size(); i++) {
			final String label = tags.get(i);
			dict.lookupLabel(label, true);
		}
		return dict;
	}

	/**
	 * gets you the all the labels as a string array.
	 */
	public String[] getTags() {
		final String taglist[] = tags.toArray(new String[tags.size()]);
		return taglist;
	}

	public int nrTags() {
		return tags.size();
	}

	public boolean contains(final String label) {
		if (tags.contains(label))
			return true;
		else
			return false;
	}
}
