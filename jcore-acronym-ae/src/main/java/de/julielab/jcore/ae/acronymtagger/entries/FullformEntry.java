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
package de.julielab.jcore.ae.acronymtagger.entries;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *  a FullformEntry contains earliest year and count of the fullform 
 * plus meshTerms of the document where it was seen in
 * last change: now the fullform-string is completely normalized and the
 * FullformEntry contains an additional list (a java Set) of all unnormalized
 * fullforms
 * 
 * @author tusche
 *
 */
public class FullformEntry implements Serializable {

	static final long serialVersionUID = 3L;
	
	public int year; // the earliest year of all documents where the form was
						// seen in

	public int count; // number of times the fullform was seen

	public HashSet<String> meshTerms; // all meshTerms of all documents

	public HashSet<String> unnormalizedForms; // all unnormalized forms seen to
											// this fullform

	/**
	 * trivial constructor
	 */
	protected FullformEntry() {
		year = 10000;
		count = 1;
	}

	/**
	 * constructor for creating a complete new FullformEntry seen ONCE now
	 * (count = 1)
	 * 
	 * @param y
	 *            the year of the document it was seen in
	 * @param m
	 *            the MeshTerms of the document it was seen in
	 * @param f
	 *            the unnormalized fullforms seen so far
	 */
	protected FullformEntry(int y, Set<String> m, Set<String> f) {
		year = y;
		meshTerms = new HashSet<String>(m);
		count = 1;
		unnormalizedForms = new HashSet<String>(f);
	}

	/**
	 * constructor for creating a FullformEntry, specifying all information
	 * 
	 * @param y
	 *            the year of the document it was seen in
	 * @param m
	 *            the MeshTerms of the document it was seen in
	 * @param f
	 *            the unnormalized fullforms seen so far
	 * @param c
	 *            the number of times the form was seen
	 */
	protected FullformEntry(int y, int c, Set<String> m, Set<String> f) {
		year = y;
		count = c;
		meshTerms = new HashSet<String>(m);
		unnormalizedForms = new HashSet<String>(f);
	}

	/**
	 * @return a String representation of this FullformEntry
	 */
	protected String getString() {
		StringBuffer s = new StringBuffer("\t" + this.year + "\t" + this.count);

		for (String mesh : meshTerms) {
			s.append("\t");
			s.append(mesh);
		}
		s.append("\t#");

		for (String form : unnormalizedForms) {
			s.append("\t");
			s.append(form);
		}

		return s + "\n";
	}
	
	/**
	 * @return a html representation of this FullformEntry
	 * for including it within a html table row
	 */
	protected String getHTMLString(boolean includeMeshs) {
		
		StringBuffer s = new StringBuffer("<td>" + this.year + "</td><td>" + this.count + "</td><td>");

		if (includeMeshs) {
			Iterator<String> meshIter = meshTerms.iterator();
			String m;
			if (meshIter.hasNext()) {
				s.append(meshIter.next());
			}
			
			while (meshIter.hasNext()) {
				m = meshIter.next();
				s.append("<br>" + m);
			}

			s.append("</td><td>");
		}

		Iterator<String> formIter = unnormalizedForms.iterator();
		String f;
		if (formIter.hasNext()) {
			s.append(formIter.next());
		}
		
		while (formIter.hasNext()) {
			f = formIter.next();
			s.append("<br>" + f);
		}
		s.append("</td>");

		return s.toString();
	}

}
