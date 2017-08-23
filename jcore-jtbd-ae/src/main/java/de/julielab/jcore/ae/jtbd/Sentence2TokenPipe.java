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
package de.julielab.jcore.ae.jtbd;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

class Sentence2TokenPipe extends Pipe {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(Sentence2TokenPipe.class);

	//TODO proper unicode!!!

	// all upper case letters (consider different languages, too)
	private static final String CAPS = "A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÄÖÜ";

	// all lower case letters (consider different languages, too)
	private static final String LOW = "a-zàèìòùáéíóúçñïäöü";

	private final Set<String> tbSymbols;

	private final Pattern splitPattern = Pattern.compile("[^\\s]+");

	/**
	 * default constructor
	 */
	public Sentence2TokenPipe() {
		super(new Alphabet(), new LabelAlphabet());
		tbSymbols = TokenBoundarySymbols.getSymbols();
	}

	/**
	 * returns a string array of all superunits (i.e. white space split)
	 *
	 * @param line
	 * @return
	 */
	private ArrayList<String> getSuperUnits(final String line) {

		final Matcher m = splitPattern.matcher(line);
		final ArrayList<String> superUnits = new ArrayList<String>();

		while (m.find())
			superUnits.add(m.group());
		return superUnits;

	}

	/**
	 * make the label sequence for the corresponding unit sequence
	 *
	 * @param orgSentence
	 * @param units
	 *            the single units of a sentence (w/o white space)
	 * @param wSpaces
	 *            info about white spaces from the testing data
	 */
	public ArrayList<String> makeLabels(final String tokSentence) {

		LOGGER.trace("makeLabels()");

		final ArrayList<String> labels = new ArrayList<String>();
		/*
		 * read in the units and the white space info
		 */
		final StringBuffer sentence = new StringBuffer(tokSentence);
		final StringBuffer currUnit = new StringBuffer();

		while (sentence.length() > 0) {
			final String c = String.valueOf(sentence.charAt(0));
			LOGGER.trace("makeLabels() - " + c);

			if (Pattern.matches("\\s", c)) { // at any whitespace
				LOGGER.trace("makeLabels() - found WS");
				if (currUnit.length() > 0) {
					currUnit.delete(0, currUnit.length());
					LOGGER.trace("makeLabels() - adding label P");
					labels.add("P");
				}
				sentence.deleteCharAt(0);
			} else if (tbSymbols.contains(c)) {
				LOGGER.trace("makeLabels() - found TB");

				// add current unit
				if (currUnit.length() > 0) {
					currUnit.delete(0, currUnit.length());
					LOGGER.trace("makeLabels() - adding label N");
					labels.add("N");
				}

				// add tb-character unit
				currUnit.append(c);

				if (sentence.length() > 1) { // check whether next token is a
					// white space
					final String c1 = String.valueOf(sentence.charAt(1));
					if (Pattern.matches("\\s", c1)) {
						LOGGER.trace("makeLabels() - label P");
						labels.add("P");
					} else {
						LOGGER.trace("makeLabels() - label N");
						labels.add("N");
					}
				} else {
					// no whitespace as last character
					LOGGER.trace("makeLabels() - label N");
					labels.add("N");
				}

				if (currUnit.length() > 0)
					currUnit.delete(0, currUnit.length());
				sentence.deleteCharAt(0);

			} else {
				LOGGER.trace("makeLabels() - token");
				currUnit.append(c);
				sentence.deleteCharAt(0);
			}

		}

		// add last unit
		LOGGER.trace("makeLabels() -  " + tokSentence);
		if (currUnit.length() > 0)
			labels.add("N");
		LOGGER.trace("makeLabels() - " + labels.toString());

		return labels;
	}

	/**
	 *
	 * makes all units for a sentence (i.e. white space and special character
	 * splits)
	 *
	 * @param orgSentence
	 * @param units
	 *            the single units of a sentence (w/o white space)
	 * @param wSpaces
	 *            info about white spaces from the testing data
	 */
	public void makeUnits(final String orgSentence,
			final ArrayList<Unit> units, final ArrayList<String> wSpaces) {

		LOGGER.trace("makeUnits() - making units...");

		/*
		 * read in the units and the white space info
		 */
		final StringBuffer sentence = new StringBuffer(orgSentence);

		final ArrayList<String> superUnitAlphabet = getSuperUnits(orgSentence);

		int superUnitIterator = 0;

		final StringBuffer currUnit = new StringBuffer();
		int start = 0;
		int end = 0;

		while (sentence.length() > 0) {
			final String c = String.valueOf(sentence.charAt(0));
			LOGGER.trace("makeUnits() - " + c);

			if (Pattern.matches("\\s", c)) {
				// at any whitespace position
				// store the unit found till this position

				LOGGER.trace("makeUnits() - WS");

				if (currUnit.length() > 0) {
					final Unit unit = new Unit(start, end, currUnit.toString(),
							superUnitAlphabet.get(superUnitIterator));
					// units.add(currUnit.toString());
					units.add(unit);
					LOGGER.trace("makeUnits() -adding unit:" + currUnit + "!");
					currUnit.delete(0, currUnit.length());
					wSpaces.add("WS");
					// labels.add("P");

					// super units
					superUnitIterator++;
				}

				sentence.deleteCharAt(0);
				LOGGER.trace("makeUnits() - " + units.toString() + " -- "
						+ wSpaces.toString());

				// set unit offset
				end++;
				start = end;

			} else if (tbSymbols.contains(c)) {
				LOGGER.trace("makeUnits() - TB");

				// add current unit
				if (currUnit.length() > 0) {
					final Unit unit = new Unit(start, end, currUnit.toString(),
							superUnitAlphabet.get(superUnitIterator));
					// units.add(currUnit.toString());
					units.add(unit);
					LOGGER.trace("makeUnits() - Adding unit:" + currUnit + "!");
					currUnit.delete(0, currUnit.length());
					wSpaces.add("noWS");

					// set unit offset
					start = end;
					LOGGER.trace("makeUnits() - SE:" + start + "." + end);
				}

				// add tb-character unit
				currUnit.append(c);
				LOGGER.trace("makeUnits() - adding unit:" + currUnit + "!!");

				if (sentence.length() > 1) { // check whether next token is a
					// white space
					final String c1 = String.valueOf(sentence.charAt(1));
					if (Pattern.matches("\\s", c1))
						wSpaces.add("WS");
					else
						wSpaces.add("noWS");
				} else
					// no whitespace as last character
					wSpaces.add("noWS");

				if (currUnit.length() > 0) {
					end++;
					LOGGER.trace("makeUnits() - SE:" + start + "." + end);
					final Unit unit = new Unit(start, end, currUnit.toString(),
							superUnitAlphabet.get(superUnitIterator));
					units.add(unit);
					currUnit.delete(0, currUnit.length());
				}
				sentence.deleteCharAt(0);

				// set unit offset
				start = end;

				LOGGER.trace("makeUnits() - " + units.toString() + " -- "
						+ wSpaces.toString());

			} else {
				LOGGER.trace("makeUnits() - token");
				currUnit.append(c);
				sentence.deleteCharAt(0);

				// set unit offset
				end++;
			}

		}

		// add last unit
		LOGGER.trace("makeUnits() - " + orgSentence);
		if (currUnit.length() > 0) {
			final Unit unit = new Unit(start, end, currUnit.toString(),
					superUnitAlphabet.get(superUnitIterator));
			units.add(unit);
			wSpaces.add("noWS");
		}

		String sent = "";
		for (int j = 0; j < units.size(); j++) {
			LOGGER.trace("makeUnits() - " + units.get(j) + "\t"
					+ wSpaces.get(j));
			final String sp = (wSpaces.get(j).equals("WS")) ? " " : "";
			sent += units.get(j).rep + sp;
		}

		LOGGER.trace("makeUnits() -org: " + orgSentence);
		LOGGER.trace("makeUnits() -new: " + sent);

		LOGGER.trace("makeUnits() - " + units.toString());

	}

	/**
	 *
	 *
	 * main pipe method to to the feature extraction
	 *
	 * @param carrier
	 *            an Instance object where the date field is the input sequence
	 *            (String), the source field is the tokenized sentence (String),
	 *            the name field is an arraylist of Unit objects
	 */
	@Override
	public Instance pipe(final Instance carrier) {

		// the input sentence (not tokenized)
		final String orgSentence = (String) carrier.getData();

		// the tokenized sentence (for training, empty for prediction)
		final String tokSentence = (String) carrier.getSource();

		// the features for each token
		final TokenSequence data = new TokenSequence();

		// the labels (P,N), P (positive) = end of token
		final LabelSequence target = new LabelSequence(getTargetAlphabet());

		final ArrayList<Unit> units = new ArrayList<Unit>();
		final ArrayList<String> wSpaces = new ArrayList<String>();
		ArrayList<String> labels;

		// make the units from the input sentence
		makeUnits(orgSentence, units, wSpaces);

		if (tokSentence.length() > 0)
			// in evaluation mode get the labels from tokSentence
			labels = makeLabels(tokSentence);
		else {
			// in prediction mode tokSentence is just an empty string
			labels = new ArrayList<String>();
			for (int i = 0; i < units.size(); i++)
				labels.add("N");
		}

		// check integrity !
		if ((units.size() != labels.size())
				|| (labels.size() != wSpaces.size())) {
			int pos = -1;
			if (null != carrier.getName())
				pos = ((Integer) carrier.getName()).intValue() + 1;
			LOGGER.error(
					"Something's wrong with unit creation. Number of units: {}; number of labels: {}; number of whitespaces: {}",
					new Object[] { units.size(), labels.size(), wSpaces.size() });
			LOGGER.error("pipe() - Unit and label extraction produced failure (at position "
					+ (pos == -1 ? "unknown" : pos)
					+ "). Omitting sentences for feature generation...\n"
					+ orgSentence + "\n" + tokSentence);

			// just omit this sentence for tokenization, but throw the error
			// from above
			carrier.setData(data);
			carrier.setTarget(target);
			carrier.setName(units);
			return carrier;
		}

		// make the features for each unit
		for (int i = 0; i < units.size(); i++) {

			final String unitRep = units.get(i).rep;
			final String superUnitRep = units.get(i).superUnitRep;
			final String label = labels.get(i);

			final Token token = new Token(unitRep);

			/*
			 * features based on information on unit only
			 */

			// the unit itself
			token.setFeatureValue("U_lex=" + unitRep, 1);

			// white space to the right
			if (wSpaces.get(i).equals("WS"))
				token.setFeatureValue("U_HasRightWhiteSpace", 1);

			// is one of the token boundary symbols
			if (tbSymbols.contains(unitRep))
				token.setFeatureValue("U_isTokenBoundarySymbol", 1);

			// word class -- currently, we use brief word class only!
			// String wc = unitRep;
			// wc = wc.replaceAll("[A-Z]", "A");
			// wc = wc.replaceAll("[a-z]", "a");
			// wc = wc.replaceAll("[0-9]", "0");
			// wc = wc.replaceAll("[^A-Za-z0-9]", "x");
			// token.setFeatureValue("U_WC=" + wc, 1);

			// brief word class
			String bwc = unitRep;
			bwc = bwc.replaceAll("[A-Z]+", "A");
			bwc = bwc.replaceAll("[a-z]+", "a");
			bwc = bwc.replaceAll("[0-9]+", "0");
			bwc = bwc.replaceAll("[^A-Za-z0-9]+", "x");
			token.setFeatureValue("U_BWC=" + bwc, 1);

			// length of the token
			if (unitRep.length() <= 3)
				token.setFeatureValue("U_SIZE1", 1);
			else if (unitRep.length() <= 6)
				token.setFeatureValue("U_SIZE2", 1);
			else
				token.setFeatureValue("U_SIZE3", 1);

			// abbreviation classes
			if (unitRep.matches("[" + CAPS + "]\\."))
				token.setFeatureValue("U_ABBR1", 1);

			if (unitRep.matches("([" + CAPS + LOW + "]\\.)+"))
				token.setFeatureValue("U_ABBR2", 1);

			if (unitRep.matches("[" + LOW + "]+\\."))
				token.setFeatureValue("U_ABBR3", 1);

			// some of our default regexp features
			if (unitRep.matches("[" + CAPS + "].*"))
				token.setFeatureValue("U_INITCAPS", 1);

			if (unitRep.matches("[" + CAPS + "]"))
				token.setFeatureValue("U_ONECAPS", 1);

			if (unitRep.matches("[" + CAPS + "]+"))
				token.setFeatureValue("U_ALLCAPS", 1);

			if (unitRep.matches("(.*[" + CAPS + LOW + "].*[0-9].*|.*[0-9].*["
					+ CAPS + LOW + "].*)"))
				token.setFeatureValue("U_ALPHANUMERIC", 1);

			if (unitRep.matches("[IVXDLCM]+"))
				token.setFeatureValue("U_ROMAN", 1);

			if (unitRep.matches(".*\\b[IVXDLCM]+\\b.*"))
				token.setFeatureValue("U_HASROMAN", 1);

			if (unitRep.matches("[0-9]+"))
				token.setFeatureValue("U_NATURALNUMBER", 1);

			if (unitRep.matches("[-0-9]+[.,]+[0-9.,]+"))
				token.setFeatureValue("U_REALNUMBER", 1);

			if (unitRep.matches(".*[0-9]+.*"))
				token.setFeatureValue("U_HASDIGITS", 1);

			if (unitRep.matches("(\\(.*|\\[.*)"))
				token.setFeatureValue("U_BEGINBRACKETS", 1);

			/*
			 * features based on super-unit information
			 */

			// add superunit as a feature
			token.setFeatureValue("SU_lex=" + superUnitRep, 1);

			// check some simple regexp
			if (superUnitRep.matches(".*[\\w]]+.*"))
				token.setFeatureValue("SU_isAlphanumeric", 1);

			// some bracket checks
			if (superUnitRep.matches("\\(.*\\)|\\[.*\\]"))
				// check whether superunit is completely bracketed
				token.setFeatureValue("SU_inBrackets", 1);
			else if (superUnitRep.matches(".*\\(.*\\).*|.*\\[.*\\].*"))
				// check whether superunit contains closed brackets
				token.setFeatureValue("SU_hasClosedBrackets", 1);
			else if (superUnitRep.matches(".*\\(.*|.*\\[.*"))
				// check whether superunit contains has left bracket only
				token.setFeatureValue("SU_hasLeftBracketOnly", 1);
			else if (superUnitRep.matches(".*\\).*|.*\\].*"))
				// check whether superunit contains has right bracket only
				token.setFeatureValue("SU_hasRightBracketOnly", 1);

			// check whether superunit is or contains an arrow
			// and unit is part of that arrow
			if (superUnitRep.matches(".*-->.*")
					&& (unitRep.equals("-") || unitRep.equals(">")))
				token.setFeatureValue("SU_isPartOfArrow", 1);

			// check for a (double) dash
			if (superUnitRep.matches("----"))
				token.setFeatureValue("SU_isDoubleDash", 1);
			else if (superUnitRep.matches(".*----.*"))
				token.setFeatureValue("SU_hasDoubleDash", 1);
			else if (superUnitRep.matches("--"))
				token.setFeatureValue("SU_isDash", 1);
			else if (superUnitRep.matches(".*--.*"))
				token.setFeatureValue("SU_hasDash", 1);

			// check for -/- or +/- etc.
			if (superUnitRep.matches(".*[+-]/[+-].*"))
				token.setFeatureValue("SU_hasPlusMinus", 1);

			// check for + and - in brackets
			if (superUnitRep.matches(".*\\([+-]\\).*"))
				token.setFeatureValue("SU_PMwithBrackets", 1);

			// check for a possible enumeration
			if (superUnitRep.matches("\\(([0-9]|[a-h]|i|ii|iii|iv|v)\\)"))
				token.setFeatureValue("SU_isEnumeration", 1);

			// check for plural s in brackets
			if (superUnitRep.matches(".*\\(s\\)"))
				token.setFeatureValue("SU_hasBracketedPlural", 1);

			// check for genitive with apostrophe
			if (superUnitRep.matches(".*'s"))
				token.setFeatureValue("SU_hasGenitive", 1);

			// check length of superunit
			if (superUnitRep.length() <= 4)
				token.setFeatureValue("SU_SIZE1", 1);
			else if (superUnitRep.length() <= 8)
				token.setFeatureValue("SU_SIZE2", 1);
			else
				token.setFeatureValue("SU_SIZE3", 1);

			// check whether superunit might be a chemical
			// therefor we check the number typical special characters contained
			if ((superUnitRep.length() > 6)
					&& superUnitRep.matches("(.*[\\W].*){5,}")
					&& !superUnitRep.contains("-->"))
				token.setFeatureValue("SU_isChemical", 1);

			// brief word class
			String su_bwc = superUnitRep;
			su_bwc = su_bwc.replaceAll("[" + CAPS + "]+", "A");
			su_bwc = su_bwc.replaceAll("[" + LOW + "]+", "a");
			su_bwc = su_bwc.replaceAll("[0-9]+", "0");
			su_bwc = su_bwc.replaceAll("[^" + CAPS + LOW + "0-9]+", "x");
			token.setFeatureValue("SU_BWC=" + su_bwc, 1);

			// check for url
			if (superUnitRep.matches("\\(?www\\..*?\\)?"))
				// LOGGER.trace("pipe() - "su: wwwURL: " + superunit);
				token.setFeatureValue("SU_wwwURL", 1);
			else if (superUnitRep.matches("\\(?http:.*?\\)?")
					|| superUnitRep.matches("\\(?ftp:.*?\\)?"))
				// LOGGER.trace("pipe() - su: httpURL: " + superunit);
				token.setFeatureValue("SU_httpURL", 1);

			/*
			 * now add the features and the label
			 */
			data.add(token);
			target.add(label);
		}

		carrier.setData(data); // the features per token
		carrier.setTarget(target); // the labels per token
		carrier.setName(units);

		carrier.setSource(wSpaces);

		return carrier;
	}

}
