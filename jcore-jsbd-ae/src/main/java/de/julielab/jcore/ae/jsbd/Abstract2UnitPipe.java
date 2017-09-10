/** 
 * Abstract2UnitPipe.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 * 
 * Author: tomanek
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: Aug 01, 2006 
 * 
 * The base pipe used converting an abstract into a sequence of Unit objects.
 **/

package de.julielab.jcore.ae.jsbd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

class Abstract2UnitPipe extends Pipe {

	private static final long serialVersionUID = 1L;

	private static final Pattern splitPattern = Pattern.compile("[^\\s]+");

	TreeSet<String> eosSymbols;

	TreeSet<String> abbrList;

	Abstract2UnitPipe() {
		super (new Alphabet(), new LabelAlphabet());

		// initialize the list of end-of-sentence symbols and abbreviations
		eosSymbols = new EOSSymbols().getSymbols();
		abbrList = (new AbbreviationsMedical()).getSet();
	}

	/**
	 * This pipe gets an Instance object, where the variable source is assumed to be the abstract
	 * filename, data is the abstract file read in.
	 * 
	 * Unit objects are created for this abstract (see MedInfo 2007 paper). For each Unit a label is
	 * predicted: "IS" means "inside sentence", "EOS" means "end-of-sentence", i.e., such a Unit is
	 * at the end of a sentence.
	 */
	public Instance pipe(Instance carrier) {

		String abstractFileName = (String) carrier.getSource();
		@SuppressWarnings("unchecked")
		ArrayList<String> lines = (ArrayList<String>) carrier.getData();
		HashMap<String, Integer> unitFreq = getUnitFrequency(lines);

		// the features for each token
		TokenSequence data = new TokenSequence();

		// the labels (IS/EOS) for each token of the text
		LabelSequence target = new LabelSequence((LabelAlphabet) getTargetAlphabet());

		ArrayList<Unit> unitInfo = new ArrayList<Unit>();

		// now go through lines and add a Token object for each token
		for (int i = 0; i < lines.size(); i++) {

			String line = (String) lines.get(i);
			if (line.length() == 0) {
				// ignore empty lines
				continue;
			}

			ArrayList<Unit> units = getUnits(line);

			if (units.size() == 0)
				continue;

			for (int j = 0; j < units.size(); j++) {

				String currUnitRep = units.get(j).rep;
				String plainUnitRep = getPlainUnit(currUnitRep);// getPlainToken(curr_token);
				String label = "IS";
				Token token = new Token(currUnitRep);

				// --- add features here ---

				// on EOSSymbols
				if (containsEOSSymbol(currUnitRep))
					token.setFeatureValue("endwithEOSSymb=" + getEOSSymbol(currUnitRep), 1);

				if ((j + 1) == units.size()) {
					label = "EOS";
				}

				int count = nrEOSSymbolsContained(plainUnitRep);
				if (count > 0)
					token.setFeatureValue("hasinnerEOSSymb=" + count, 1);

				// the token itself
				token.setFeatureValue("TOKEN=" + currUnitRep, 1);

				// some regexp features
				if (currUnitRep.matches("[\\p{Lu}\\p{M}].*"))
					token.setFeatureValue("INITCAPS", 1);

				if (currUnitRep.matches("[\\p{Lu}\\p{M}]"))
					token.setFeatureValue("ONECAPS", 1);

				if (currUnitRep.matches("[\\p{Lu}\\p{M}]+"))
					token.setFeatureValue("ALLCAPS", 1);

				if (currUnitRep.matches("(.*[\\p{L}\\p{M}].*[0-9].*|.*[0-9].*[\\p{L}\\p{M}].*)"))
					token.setFeatureValue("ALPHANUMERIC", 1);

				if (currUnitRep.matches("[IVXDLCM]+"))
					token.setFeatureValue("ROMAN", 1);

				if (currUnitRep.matches(".*\\b[IVXDLCM]+\\b.*"))
					token.setFeatureValue("HASROMAN", 1);

				if (currUnitRep.matches("[0-9]+"))
					token.setFeatureValue("NATURALNUMBER", 1);

				if (currUnitRep.matches("[-0-9]+[.,]+[0-9.,]+"))
					token.setFeatureValue("REALNUMBER", 1);

				if (currUnitRep.matches(".*[0-9]+.*"))
					token.setFeatureValue("HASDIGITS", 1);

				if (currUnitRep.matches("(\\(.*|\\[.*)"))
					token.setFeatureValue("BEGINBRACKETS", 1);

				if (currUnitRep.matches("(\\(.*\\)|\\[.*\\])"))
					token.setFeatureValue("INSIDEBRACKETS", 1);

				if (currUnitRep.matches("(\".*|'.*)"))
					token.setFeatureValue("BEGINQUOTES", 1);

				if (currUnitRep.matches("(\".*\"|'.*')"))
					token.setFeatureValue("INSIDEBQUOTES", 1);

				// length of the token
				if (currUnitRep.length() <= 3)
					token.setFeatureValue("SIZE1", 1);
				else if (currUnitRep.length() <= 6)
					token.setFeatureValue("SIZE2", 1);
				else
					token.setFeatureValue("SIZE3", 1);

				// abbreviation classes
				if (currUnitRep.matches("[A-Z]\\."))
					token.setFeatureValue("ABBR1", 1);

				if (currUnitRep.matches("([A-Za-z]\\.)+"))
					token.setFeatureValue("ABBR2", 1);

				if (currUnitRep.matches("[abcdfghjklmnpqrstvwxyz]+\\."))
					token.setFeatureValue("ABBR3", 1);

				// word class
				String bwc = plainUnitRep;

				bwc = bwc.replaceAll("[\\p{Lu}\\p{M}]+", "A");
				bwc = bwc.replaceAll("[\\p{Ll}\\p{M}]+", "a");
				bwc = bwc.replaceAll("[0-9]+", "0");
				bwc = bwc.replaceAll("[^\\p{L}\\p{M}0-9]+", "x");

				token.setFeatureValue("BWC=" + bwc, 1);

				// check whether token with EOSsymbol occurs more than once in
				// abstract
				if (containsEOSSymbol(currUnitRep)) {
					int freq = ((Integer) unitFreq.get(currUnitRep)).intValue();
					if (freq > 1)
						token.setFeatureValue("FreqTokenEOSSymbol", 1);
				}

				// abbreviation
				if (abbrList.contains(currUnitRep))
					token.setFeatureValue("KNOWNABBR", 1);

				// --- add all to the instance ---
				data.add(token);
				target.add(label);
			}
			unitInfo.addAll(units);
		}

		carrier.setData(data); // the features per token
		carrier.setTarget(target); // the labels per token

		carrier.setName(unitInfo); // the units of the abstract
		carrier.setSource(abstractFileName); // the filename of the piece of text to be split

		return carrier;
	}

	/**
	 * counts the number of EOS symbols contained in the token
	 * 
	 * @param token
	 * @return int
	 */
	private int nrEOSSymbolsContained(String token) {
		int count = 0;
		char[] c = token.toCharArray();
		for (int i = 0; i < c.length; i++) {
			char[] cc = { c[i] };
			if (eosSymbols.contains(new String(cc)))
				count++;
		}
		return count;
	}

	/**
	 * checks whether the token ends with a EOSSymbol
	 * 
	 * @param token
	 * @return true if containes EOS symbol
	 */
	private boolean containsEOSSymbol(String token) {
		if (token.length() > 0) {
			String lastChar = token.substring(token.length() - 1, token.length());
			if (eosSymbols.contains(lastChar))
				return true;
		}
		return false;
	}

	/**
	 * returns the last char of a token, if this char is a EOSSymbol. Otherwise an empty string is
	 * returned.
	 * 
	 * @param token
	 * @return
	 */
	private String getEOSSymbol(String token) {
		if (token.length() > 0) {
			String lastChar = token.substring(token.length() - 1, token.length());
			if (eosSymbols.contains(lastChar))
				return lastChar;
		}
		return "";
	}

	/**
	 * remove the EOSSymbol from the string token representation. If token does not end with
	 * EOSsymbol, the original token is returned.
	 * 
	 * @return
	 */
	private String getPlainUnit(String unitRep) {
		if (containsEOSSymbol(unitRep))
			return unitRep.substring(0, unitRep.length() - 1);
		else
			return unitRep;
	}

	/**
	 * get the frequence of occurrence of this unit in the abstract
	 * 
	 * @param lines
	 *            the input file split into single lines
	 * @return
	 */
	private HashMap<String, Integer> getUnitFrequency(ArrayList<String> lines) {
		HashMap<String, Integer> freq = new HashMap<String, Integer>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			ArrayList<Unit> units = getUnits(line);

			for (int j = 0; j < units.size(); j++) {
				Unit u = units.get(j);

				int count = 0;
				if (freq.containsKey(u.rep)) {
					count = freq.get(u.rep);
				}
				count++;
				freq.put(u.rep, count);
			}
		}
		return freq;
	}

	/**
	 * returns a string array containing all units for one line this is done using a regexp matcher
	 * the line is split it all whitespace characters
	 * 
	 * @param line
	 * @return
	 */
	private ArrayList<Unit> getUnits(String line) {

		Matcher m = splitPattern.matcher(line);
		ArrayList<Unit> units = new ArrayList<Unit>();

		while (m.find()) {
			int begin = m.start();
			int end = m.end();
			String rep = m.group();
			units.add(new Unit(begin, end, rep));
		}
		return units;

	}
}