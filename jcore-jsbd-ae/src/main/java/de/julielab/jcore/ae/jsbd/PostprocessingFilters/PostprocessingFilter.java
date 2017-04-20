package de.julielab.jcore.ae.jsbd.PostprocessingFilters;

import java.util.ArrayList;
import java.util.TreeSet;

import de.julielab.jsbd.AbbreviationsBiomed;
import de.julielab.jsbd.AbbreviationsMedical;
import de.julielab.jsbd.Unit;

public class PostprocessingFilter {

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
