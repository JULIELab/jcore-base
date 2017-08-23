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
package de.julielab.jcore.ae.jpos.tagger;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureConfiguration {

	/**
	 * check whether a boolean feature exists and is set to "true"
	 *
	 * @param propDef
	 * @param propName
	 * @return
	 */
	public boolean featureActive(final Properties propDef, final String propName) {
		if ((propDef.getProperty(propName) != null)
				&& propDef.getProperty(propName).equals("true"))
			return true;
		else
			return false;
	}

	public int[] getIntArray(final Properties propDef, final String propName) {
		int[] intArray = null;

		if (propDef.getProperty(propName) != null) {
			final String[] stringArray = propDef.getProperty(propName).trim()
					.split(",");

			intArray = new int[stringArray.length];
			for (int i = 0; i < stringArray.length; i++)
				intArray[i] = (new Integer(stringArray[i])).intValue();
		}
		return intArray;
	}

	/**
	 * extracts the offset conjunction information (feature creation horizon)
	 * from a String of the form (-1) (0) (1), (-1) (0) (1,2) or (-1) (0) (1 2)
	 */
	public int[][] offsetConjFromConfig(final String offset_conjunctions) {
		if (offset_conjunctions == null)
			return null;

		int[][] conjunctions;

		// to find round brackets with digits sperated with whitespaces
		final Pattern inBrackets = Pattern.compile("\\([-\\d\\s,]+\\)");
		final Matcher bracketMatcher = inBrackets.matcher(offset_conjunctions);

		final ArrayList<String> bracketContents = new ArrayList<String>();
		int bracketStart, bracketEnd;
		int pos = 0;

		// get all brackets and write their content in the ArrayList
		while (bracketMatcher.find(pos)) {
			bracketStart = bracketMatcher.start() + 1;
			bracketEnd = bracketMatcher.end() - 1;
			bracketContents.add(offset_conjunctions.substring(bracketStart,
					bracketEnd));
			pos = bracketMatcher.end();
		}
		// create an array for each bracket and fill it with the digits in the
		// bracket
		conjunctions = new int[bracketContents.size()][];
		for (int i = 0; i < bracketContents.size(); i++) {
			final String[] digits = bracketContents.get(i).split(",");
			conjunctions[i] = new int[digits.length]; // array creation
			for (int j = 0; j < digits.length; j++)
				conjunctions[i][j] = Integer.parseInt(digits[j].trim()); // filling
		}
		return conjunctions;
	}

	public ArrayList<String> getLexiconKeys(final Properties propDef) {
		final ArrayList<String> lexiconKeys = new ArrayList<String>();

		final Enumeration<?> keys = propDef.propertyNames();
		while (keys.hasMoreElements()) {
			final String key = (String) keys.nextElement();
			if (key.matches("[a-zA-Z]+_lexicon"))
				lexiconKeys.add(key);
		}
		return lexiconKeys;
	}

	/**
	 * Gets the value of a property, splitting it into Strings
	 *
	 * @param propDef
	 *            Property file to parse
	 * @param propName
	 *            Property key to extract values for
	 * @return String[], containing the value split at "," or null, if not found
	 */
	public String[] getStringArray(final Properties propDef,
			final String propName) {
		if (propDef.getProperty(propName) != null)
			return propDef.getProperty(propName).trim().split(", *");
		return null;
	}
}
