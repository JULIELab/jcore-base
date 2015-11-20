/** 
 * NABaseline.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 1.0	
 * Since version:   0.1
 *
 * Creation date: Jul 8, 2008 
 * 
 * This is the Baselines to be used in the Coordination Resolution 
 * 
 **/
package de.julielab.coordination.tagger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;

public class NABaseline {

	public static String CONJ = "CONJ";
	public static String CC = "CC";
	public static String COMMA = ",";
	public static String OUTSIDE_LABEL = "O";
	final static int BIO = 1;
	final static int WSJ = 0;
	final static int MODUS = WSJ;
	final static String NOUN = "#n";
	static boolean FOUND = false;
	static boolean heuristic = false;

	public NABaseline() {
	}

	/**
	 * Normalises the word for the request of WordNet
	 * 
	 * @param word
	 * @return word: normalised version
	 */
	public String normalize(String word) {
		String normalized;
		if (MODUS == WSJ) {
			if (word.contains("-")) {
				word = word.substring(word.lastIndexOf("-") + 1, word.length());
			}
		}
		if (MODUS == BIO) {
			word = word.replaceAll("-", "_");
		}
		normalized = word;
		return normalized;
	}

	/**
	 * @param wordnetTokens
	 * @param measureType
	 * @throws IOException
	 */
	public boolean predictConjuncts(ArrayList<CoordinationToken> wordnetTokens) throws IOException {
		try {
			FOUND = false;
			CoordinationToken[] tokens;
			String[] words;
			CoordinationToken n1 = null, n2 = null, n3 = null;
			String n1Word = null, n2Word = null, n3Word = null;
			String expression = "";
			int pattern = getPatternType(wordnetTokens);
			//System.out.println("Pattern " + pattern);
			if (pattern != -1) {
				tokens = setPatternOnExpression(pattern, wordnetTokens);
				n1 = tokens[0];
				n2 = tokens[1];
				n3 = tokens[2];
				words = setWords(tokens);
				n1Word = words[0];
				n2Word = words[1];
				n3Word = words[2];
				if (n1 != null && n2 != null && n3 != null) {
					setConjuncts(pattern, wordnetTokens, n1, n2, n3, n1Word, n2Word, n3Word);
					writeConjuncts(wordnetTokens);
				}
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return FOUND;
	}

	public void writeConjuncts(ArrayList<CoordinationToken> wordnetTokens) throws IOException {
		String expression = "";
		String labels = "";
		for (Iterator iter = wordnetTokens.iterator(); iter.hasNext();) {
			CoordinationToken element = (CoordinationToken) iter.next();
			String word = element.getWord();
			String label = element.getCoordinationLabel();
			labels = labels + label + " ";
			expression = expression + word + " ";
		}
		/**
		 * if (expression.contains(",")) { multipleCoordinatiosnWriter.write(expression + "\n");
		 * multipleCoordinatiosnWriter.close(); }
		 */
		/**
		 * if (!labels.contains(CONJ)) { multipleCoordinatiosnWriter.write(expression + "\n");
		 * multipleCoordinatiosnWriter.close(); }
		 */
		for (Iterator iter = wordnetTokens.iterator(); iter.hasNext();) {
			CoordinationToken element = (CoordinationToken) iter.next();
			String word = element.getWord();
			String label = element.getCoordinationLabel();
			String line = word + "\t";
			if (!label.equals(CONJ)) {
				if (element.getPosTag().equals(CC) || element.getPosTag().equals(COMMA)) {
					label = CC;
				} else {
					label = OUTSIDE_LABEL;
				}
			}
		}
	}

	private String[] setWords(CoordinationToken[] tokens) {
		String[] words = new String[3];
		CoordinationToken n1 = tokens[0];
		CoordinationToken n2 = tokens[1];
		CoordinationToken n3 = tokens[2];
		String n1Word = null, n2Word = null, n3Word = null;
		if (n1 != null && n2 != null && n3 != null) {
			n1Word = n1.getWord().toLowerCase() + NOUN;
			n2Word = n2.getWord().toLowerCase() + NOUN;
			n3Word = n3.getWord().toLowerCase() + NOUN;
			n1Word = normalize(n1Word);
			n2Word = normalize(n2Word);
			n3Word = normalize(n3Word);
		}
		//System.out.println("Words: " + n1Word + " " + n2Word + " " + n3Word);
		words[0] = n1Word;
		words[1] = n2Word;
		words[2] = n3Word;
		return words;
	}

	private void setConjuncts(int pattern, ArrayList<CoordinationToken> wordnetTokens, CoordinationToken n1,
					CoordinationToken n2, CoordinationToken n3, String n1Word, String n2Word, String n3Word) {
		double valueMeasure1 = 0, valueMeasure2 = 0;
		try {
			String pos1 = n1.getPosTag();
			String pos2 = n2.getPosTag();
			String pos3 = n3.getPosTag();
			String number1 = "sing", number2 = "sing", number3 = "sing";
			if (pos1.endsWith("S"))
				number1 = "plural";
			if (pos2.endsWith("S"))
				number2 = "plural";
			if (pos3.endsWith("S"))
				number3 = "plural";
			// Resnik Baseline
			if (number1.equals(number2) && !(number1.equals(number3))) {
				valueMeasure1 = 1;
				valueMeasure2 = 0;
			} else if (number1.equals(number3) && !(number1.equals(number2))) {
				valueMeasure2 = 1;
				valueMeasure1 = 0;
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (pattern == 0) {
			if (valueMeasure1 > valueMeasure2) {
				n1.setCoordinationLabel(CONJ);
				n2.setCoordinationLabel(CONJ);
				if (heuristic) {
					for (Iterator iter = wordnetTokens.iterator(); iter.hasNext();) {
						CoordinationToken coodToken = (CoordinationToken) iter.next();
						String posTag = coodToken.getPosTag();
						if (!posTag.equals(CC) && !posTag.equals(COMMA)) {
							coodToken.setCoordinationLabel(CONJ);
						}
					}
				}
			} else if (valueMeasure2 > valueMeasure1) {
				n2.setCoordinationLabel(CONJ);
				n3.setCoordinationLabel(CONJ);
			} else if (valueMeasure2 == valueMeasure1 && valueMeasure1 != -1) {
				n2.setCoordinationLabel(CONJ);
				n3.setCoordinationLabel(CONJ);
			}
		} else if (pattern == 1 || pattern == 2) {
			if (valueMeasure1 > valueMeasure2) {
				n1.setCoordinationLabel(CONJ);
				n2.setCoordinationLabel(CONJ);
				if (heuristic) {
					int n1Position = getPosition(n1, wordnetTokens);
					int n2Position = getPosition(n2, wordnetTokens);
					int n3Position = getPosition(n3, wordnetTokens);
					int ccPosition = getCCPosition(wordnetTokens);
					for (int i = 0; i < wordnetTokens.size(); i++) {
						CoordinationToken tok = wordnetTokens.get(i);
						if (i < n2Position && i > ccPosition) {
							tok.setCoordinationLabel(CONJ);
						}
					}
				}
			} else if (valueMeasure2 > valueMeasure1) {
				n1.setCoordinationLabel(CONJ);
				n3.setCoordinationLabel(CONJ);
				if (heuristic) {
					// setze alle auf Conj
					for (Iterator iter = wordnetTokens.iterator(); iter.hasNext();) {
						CoordinationToken coodToken = (CoordinationToken) iter.next();
						String posTag = coodToken.getPosTag();
						if (!posTag.equals(CC) && !posTag.equals(COMMA)) {
							coodToken.setCoordinationLabel(CONJ);
						}
					}
				}
			} else if (valueMeasure2 == valueMeasure1 && valueMeasure1 != -1) {
				try {
					n1.setCoordinationLabel(OUTSIDE_LABEL);
					n2.setCoordinationLabel(OUTSIDE_LABEL);
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private int getCCPosition(ArrayList<CoordinationToken> wordnetTokens) {
		int position = 0;
		for (int i = 0; i < wordnetTokens.size(); i++) {
			CoordinationToken element = (CoordinationToken) wordnetTokens.get(i);
			if (element.getPosTag().equals(CC) || element.getPosTag().equals(COMMA)) {
				return i;
			}
		}
		return position;
	}

	private int getPosition(CoordinationToken n, ArrayList<CoordinationToken> wordnetTokens) {
		int pos = 0;
		for (int i = 0; i < wordnetTokens.size(); i++) {
			CoordinationToken element = (CoordinationToken) wordnetTokens.get(i);
			if (element.equals(n)) {
				pos = i;
			}
		}
		return pos;
	}

	/**
	 * Identifies n1, n2 and n3 in expressions such as IMB and Microsoft sales (n1, n2, n3)
	 * 
	 * @param pattern
	 * @param wordnetTokens
	 * @param n1
	 * @param n2
	 * @param n3
	 */
	public CoordinationToken[] setPatternOnExpression(int pattern, ArrayList<CoordinationToken> wordnetTokens) {
		CoordinationToken[] tokens = new CoordinationToken[3];
		CoordinationToken n1 = null, n2 = null, n3 = null;
		String expression = "";
		for (Iterator iter = wordnetTokens.iterator(); iter.hasNext();) {
			CoordinationToken element = (CoordinationToken) iter.next();
			String word = element.getWord();
			expression = expression + word + " ";
		}
		if (pattern == 0) {
			//System.out.println("Pattern 0");
			for (int i = 0; i < wordnetTokens.size(); i++) {
				CoordinationToken token = wordnetTokens.get(i);
				if (n1 != null & n2 != null && n3 == null & token.getPosTag().contains("NN")) {
					n3 = token;
				}
				if (n1 != null && n2 == null & token.getPosTag().contains("NN")) {
					n2 = token;
				}
				if (n1 == null & token.getPosTag().contains("NN")) {
					n1 = token;
				}
			}
		}
		if (pattern == 2) {
			//System.out.println("Expression Pattern is Pattern 2 ");
			for (int i = 0; i < wordnetTokens.size(); i++) {
				CoordinationToken token = wordnetTokens.get(i);
				//System.out.println(wordnetTokens.get(i));
				if (token.getPosTag().equals(CC) || token.getPosTag().equals(COMMA)) {
					if (n1 == null) {
						for (int g = i - 1; g > 0; g--) {
							CoordinationToken token2 = (CoordinationToken) wordnetTokens.get(g);
							if (token2.getPosTag().contains("NN") && n1 == null) {
								n1 = token2;
							}
						}
					}
					for (int j = i + 1; j < wordnetTokens.size(); j++) {
						CoordinationToken token3 = wordnetTokens.get(j);
						if (n2 == null & token3.getPosTag().contains("NN") & (j + 2) == wordnetTokens.size()) {
							n2 = token3;
							//System.out.println("n2 is " + n2.getWord());
						}
					}
				}
				if (n2 != null & i + 1 == wordnetTokens.size()) {
					n3 = token;
					//System.out.println("n3 is " + n3.getWord());
				}
			}
		}
		if (pattern == 1) {
			//System.out.println("Pattern 1");
			for (int i = 0; i < wordnetTokens.size(); i++) {
				CoordinationToken token = wordnetTokens.get(i);
				if (n1 == null & token.getPosTag().contains("NN")) {
					n1 = token;
					//System.out.println("n1 is " + n1.getWord());
				}
				if (token.getPosTag().equals(CC) || token.getPosTag().equals(COMMA)) {
					for (int j = i + 1; j < wordnetTokens.size(); j++) {
						CoordinationToken token2 = wordnetTokens.get(j);
						if (n2 == null && token2.getPosTag().contains("NN")) {
							n2 = token2;
							//System.out.println("n2 is " + n2.getWord());
						}
					}
				}
				if (n2 != null & i + 1 == wordnetTokens.size()) {
					n3 = token;
					//System.out.println("n3 is " + token.getWord());
				}
			}
		}
		tokens[0] = n1;
		tokens[1] = n2;
		tokens[2] = n3;
		return tokens;
	}

	/**
	 * returns a pattern
	 * 
	 * @param wordnetTokens
	 * @return
	 * @throws IOException
	 */
	public int getPatternType(ArrayList<CoordinationToken> wordnetTokens) throws IOException {
		String np = "";
		String pattern = "";
		for (Iterator iter = wordnetTokens.iterator(); iter.hasNext();) {
			CoordinationToken token = (CoordinationToken) iter.next();
			String posTag = token.getPosTag();
			String word = token.getWord();
			np = np + word + " ";
			if (posTag.contains("NN")) {
				pattern = pattern + "n ";
			} else if (posTag.equals(CC)) {
				pattern = pattern + CC + " ";
			}
		}
		if (pattern.contains("n n CC n n"))
			return 2;
		else if (pattern.contains("n CC n n"))
			return 1;
		else if (pattern.contains("n n CC n"))
			return 0;
		return -1;
	}
}
