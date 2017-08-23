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
package de.julielab.jcore.ae.jsbd.PostprocessingFilters;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.stream.Stream;

import de.julielab.jcore.ae.jsbd.AbbreviationsBiomed;
import de.julielab.jcore.ae.jsbd.AbbreviationsMedical;
import de.julielab.jcore.ae.jsbd.Unit;

public class PostprocessingFilter {
	public static final String BIOMED_POSTPROC = "biomed";
	public static final String MEDICAL_POSTPROC = "medical";
	
	public static final Stream<String> POSTPROC_STREAM = Stream.of("biomed", "medical");
	
	public enum Mode {
		BIOMED {
	        @Override
	        public ArrayList<String> process(ArrayList<String> predLabels, ArrayList<Unit> units) {
	        	return biomedPostprocessingFilter(predLabels, units);
			}
	    },
	    MEDICAL {
	        @Override
	        public ArrayList<String> process(ArrayList<String> predLabels, ArrayList<Unit> units) {
	        	return medicalPostprocessingFilter(predLabels, units);
			}
	    };

	    public abstract ArrayList<String> process(ArrayList<String> predLabels, ArrayList<Unit> units);
	}

	/**
	 * a postprocessing filter (to be used after prediction) which can correct known errors
	 * 
	 * @param predLabels
	 * @param units
	 * @param abbrList
	 * @return
	 */
	public static ArrayList<String> medicalPostprocessingFilter(ArrayList<String> predLabels, ArrayList<Unit> units) {
		AbbreviationsMedical abr = new AbbreviationsMedical();
		TreeSet<String> abrSet = abr.getSet();

		String[] labels = (String[]) predLabels.toArray(new String[predLabels.size()]);
		ArrayList<String> newPred = new ArrayList<String>();

		// do not set an EOS after opening bracket until bracket is closed again
		int openNormalBrackets = 0;
		int openSquareBrackets = 0;
		int count = 0;

		for (int i = 0; i < labels.length; i++) {
			String unitRep = units.get(i).rep;

			char[] c = unitRep.toCharArray();
			for (int j = 0; j < c.length; j++) {
				switch (c[j]) {
				case '(':
					openNormalBrackets++;
					break;
				case '[':
					openSquareBrackets++;
					break;
				case ')':
					openNormalBrackets--;
					break;
				case ']':
					openSquareBrackets--;
					break;
				}
			}

			if (openSquareBrackets > 0 || openNormalBrackets > 0) {
				labels[i] = "IS";
				count++;
			}

			// close all brackets after 50 tokens inside brackets
			if (count >= 50) {
				openSquareBrackets = 0;
				openNormalBrackets = 0;
			}

			if (openSquareBrackets < 0)
				openSquareBrackets = 0;

			if (openNormalBrackets < 0)
				openNormalBrackets = 0;

		}

		for (int i = 0; i < labels.length; i++) {
			String unitRep = units.get(i).rep;

			// remove EOS from known abbreviations
			if (abrSet.contains(unitRep))
				labels[i] = "IS";

			// set EOS if ends with ? or ! ."
			if (unitRep.endsWith(".\"") || unitRep.endsWith("?") || unitRep.endsWith("!"))
				labels[i] = "EOS";

			// add to final arrayList
			newPred.add(labels[i]);
		}

		return newPred;
	}
	
	/**
	 * a postprocessing filter (to be used after prediction) which can correct known errors
	 * 
	 * @param predLabels
	 * @param units
	 * @param abbrList
	 * @return
	 */
	public static ArrayList<String> biomedPostprocessingFilter(ArrayList<String> predLabels, ArrayList<Unit> units) {

		AbbreviationsBiomed abr = new AbbreviationsBiomed();
		TreeSet<String> abrSet = abr.getSet();

		String[] labels = (String[]) predLabels.toArray(new String[predLabels.size()]);
		ArrayList<String> newPred = new ArrayList<String>();

		// do not set an EOS after opening bracket until bracket is closed again
		int openNormalBrackets = 0;
		int openSquareBrackets = 0;
		int count = 0;

		for (int i = 0; i < labels.length; i++) {
			String unitRep = units.get(i).rep;

			char[] c = unitRep.toCharArray();
			for (int j = 0; j < c.length; j++) {
				switch (c[j]) {
				case '(':
					openNormalBrackets++;
					break;
				case '[':
					openSquareBrackets++;
					break;
				case ')':
					openNormalBrackets--;
					break;
				case ']':
					openSquareBrackets--;
					break;
				}
			}

			if (openSquareBrackets > 0 || openNormalBrackets > 0) {
				labels[i] = "IS";
				count++;
			}

			// close all brackets after 50 tokens inside brackets
			if (count >= 50) {
				openSquareBrackets = 0;
				openNormalBrackets = 0;
			}

			if (openSquareBrackets < 0)
				openSquareBrackets = 0;

			if (openNormalBrackets < 0)
				openNormalBrackets = 0;

		}

		for (int i = 0; i < labels.length; i++) {
			String unitRep = units.get(i).rep;

			// remove EOS from known abbreviations
			if (abrSet.contains(unitRep))
				labels[i] = "IS";

			// set EOS if ends with ? or ! ."
			if (unitRep.endsWith(".\"") || unitRep.endsWith("?") || unitRep.endsWith("!"))
				labels[i] = "EOS";

			// add to final arrayList
			newPred.add(labels[i]);
		}

		return newPred;
	}
}
