/** 
 * PairExtractor.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 1.3	
 * Since version:   1.2
 *
 * Creation date: Jun 09, 2007 
 * 
 * Extracts, stores & preprocesses 'true & 'false' pairs
 * TODO: explain OVERLAP_RATIO, MAXSYN_LENGTH, RELATED_PAIRS
 **/

package de.julielab.jules.ae.genemapper.scoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.ae.genemapper.utils.Utils;

public class MaxEntScorerPairExtractor {

	private static final Logger LOGGER = LoggerFactory.getLogger(MaxEntScorerPairExtractor.class);

	private static final int RELATED_PAIRS = 10;

	private static final double OVERLAP_RATIO = 0.5;

	private static final int MAXSYN_LENGTH = 5;
	
	
	/**
	 * TODO: comment!
	 * 
	 * @param trueList
	 * @param completeList
	 * @param amountTrue
	 * @param ratioFalse
	 * @return
	 */
	ArrayList<String[]> getPairs(File trueList, File completeList,
			int amountTrue, float ratioFalse) {

		ArrayList<String[]> pairs = new ArrayList<String[]>();

		try {
			// first get positive training examples (first n entries in
			// true-examples-file)
			BufferedReader fileIn = new BufferedReader(new FileReader(trueList));
			String text;
			int counter = 1;

			LOGGER.debug("getPairs() - getting positive training examples ...");
			while ((text = fileIn.readLine()) != null && counter <= amountTrue) {
				pairs.add(text.split("\t"));
				++counter;
			}
			fileIn.close();
			LOGGER
					.debug("getPairs() - number of positive training examples read: "
							+ pairs.size());

			if (amountTrue != pairs.size()) {
				LOGGER
						.debug("getPairs() - number of positive training examples: "
								+ amountTrue);
				amountTrue = pairs.size();
				LOGGER.warn("getPairs() - Only " + amountTrue
						+ " entries available!");
			}

			// then add negative ones accordings to ratio
			int fillUp = (int) (amountTrue * ratioFalse);
			ArrayList<String[]> restPairs = findFalse(completeList, fillUp);
			for (int i = 0; i < restPairs.size(); ++i) {
				pairs.add(restPairs.get(i));
			}

			// it might be more efficient, if the data structure
			// would be unsorted by design. (HashSet or something else.)

			// Collections.shuffle(pairs);
			LOGGER.debug("getPairs() - overall number of training examples: "
					+ pairs.size());

		} catch (IOException io) {
			io.printStackTrace();
		}
		return pairs;
	}

	public void showPairs(ArrayList<String[]> pairs) {
		LOGGER.debug("all pairs: ");
		for (int i = 0; i < pairs.size(); i++) {
			StringBuffer pair = new StringBuffer();
			String[] l = (String[]) pairs.get(i);
			for (int j = l.length - 1; j > -1; j--) {
				pair.append(l[j] + " \t");
			}
			System.out.println(pair);
		}
	}

	public void storePairs(ArrayList<String[]> pairs, File filename)
			throws IOException {
		LOGGER.debug("storePairs()");
		FileWriter fw = new FileWriter(filename);

		for (int i = 0; i < pairs.size(); i++) {
			StringBuffer pair = new StringBuffer();
			String[] l = (String[]) pairs.get(i);
			for (int j = 0; j < l.length; j++) {
				pair.append(l[j] + "\t");
			}
			fw.write(pair.toString() + "\n");
		}

		fw.close();
	}

	/**
	 * TODO: comment!
	 * 
	 * @param entities
	 * @param amount
	 * @return
	 */
	ArrayList<String[]> findFalse(File entities, int amount) {
		ArrayList<String[]> pairs = new ArrayList<String[]>();
		LOGGER.debug("findFalse() - getting negative training examples ...");
		try {
			BufferedReader fileIn = new BufferedReader(new FileReader(entities));
			ArrayList<String[]> rows = new ArrayList<String[]>();
			String text;

			while ((text = fileIn.readLine()) != null) {
				rows.add(text.split("\t"));
			}
			
			LOGGER.debug("rows: " + rows.size());
			fileIn.close();

			LOGGER.debug("findFalse() - number of negative training examples: "
					+ amount);
			// decides for every pair of String, if it should be
			// added to the learning process (at least one word in common
			// [normalized!]).
			int counter = 0;
			long time = System.currentTimeMillis();
			// TermNormalizer normal = new TermNormalizer();
			for (int i = 0; i < rows.size() && counter < amount; ++i) {
				String first = rows.get(i)[0];
				int internalCounter = 0;
				for (int j = i + 1; j < rows.size() && counter < amount; ++j) {
					if (!(rows.get(i)[1].equals(rows.get(j)[1]))) { // make sure
						// this is a
						// negative
						// example (if xid not same)
						String second = rows.get(j)[0];
						if (addPair(first, second, OVERLAP_RATIO, MAXSYN_LENGTH)) {
							pairs.add(new String[] { first, second, "FALSE" });
							++counter;
							++internalCounter;
							// show status
							if (counter % 50 == 0) {
								System.out.println("made " + counter + "/"
										+ amount);

							}
						}
					}
					if (internalCounter >= RELATED_PAIRS) {
						// LOGGER.debug("findFalse() - term used in too many
						// pairs...: " + first);
						break;
					}
				}
			}
			long time2 = System.currentTimeMillis();
			LOGGER.debug("findFalse() - result: duration: " + (time2 - time)
					+ " ms; pairs: " + pairs.size() + "; counter: " + counter);
		} catch (IOException io) {
			io.printStackTrace();
		}
		return pairs;
	}


	
	void makeFalseList(File entities, File storeList) {
		ArrayList<String[]> pairs = new ArrayList<String[]>();
		LOGGER.debug("findFalse() - getting negative training examples ...");
		
		
		try {
			LOGGER.debug("reading complete list...");
			BufferedReader fileIn = new BufferedReader(new FileReader(entities));
			ArrayList<String[]> rows = new ArrayList<String[]>();
			String text;

			while ((text = fileIn.readLine()) != null) {
				String values[] = text.split("\t");
				if (values[1].split(" ").length<=MAXSYN_LENGTH)
					rows.add(text.split("\t"));
			}
			
			if (storeList.isFile()) {
				storeList.delete();
			}
			
			LOGGER.debug("starting to make pairs for rows: " + rows.size());
			fileIn.close();

			// decides for every pair of String, if it should be
			// added to the learning process (at least one word in common
			// [normalized!]).
			long time = System.currentTimeMillis();
			// TermNormalizer normal = new TermNormalizer();
			for (int i = 0; i < rows.size(); ++i) {
				FileWriter out = new FileWriter(storeList, true);
				if (i%10==0) {
					System.out.println(i + "/" + rows.size());
				}
				
				String first = rows.get(i)[1];
				for (int j = i + 1; j < rows.size(); ++j) {

					if (!(rows.get(i)[0].equals(rows.get(j)[0]))) { // make sure
						// this is a
						// negative
						// example (if xid not same)
						String second = rows.get(j)[1];
						if (addPairSpecialRules(first, second, OVERLAP_RATIO, MAXSYN_LENGTH)) {
							out.write(first + "\t" + second + "\t" + "FALSE" + "\n");
							//System.out.println(rows.get(i)[0] + " : " + rows.get(j)[0] + "\t" + first + " : " + second);
						}
					}
				}
				out.close();
			}
			long time2 = System.currentTimeMillis();
			LOGGER.debug("findFalse() - result: duration: " + (time2 - time)
					+ " ms");
		} catch (IOException io) {
			io.printStackTrace();
		}
		
	}
	


	/**
	 * simple pair add rule: terms must not be the same and must have at least
	 * one token in common. Works on normalized terms.
	 * 
	 * @param first
	 *            normalized term
	 * @param second
	 *            normalized term
	 * @return
	 */
	public boolean addPair(String first, String second) {
		if (first.equals(second)) {
			return false;
		}
		String[] firstArray = first.split(" ");
		String[] secondArray = second.split(" ");
		TreeSet<String> intersection = Utils.getCommonWords(firstArray, secondArray);
		if (intersection.size() >= 1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * overlap must be at least overlapRatio in both terms and both terms must
	 * not be longer than a maximal synonym length
	 * 
	 * @param first
	 *            normalized term
	 * @param second
	 *            normalized term
	 * @param overlapRatio
	 *            intersection-size / term-length
	 * @param maxSynLenghth
	 *            length in tokens
	 * @return
	 */
	public boolean addPair(String first, String second, double overlapRatio,
			int maxSynLength) {
		double termOverlap = Utils.getOverlapRatio(first, second);
	
	
		String[] firstArray = first.split(" ");
		String[] secondArray = second.split(" ");
		int firstLength = firstArray.length;
		int secondLength = secondArray.length;

		if (termOverlap >= overlapRatio && firstLength <= maxSynLength && secondLength <= maxSynLength) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * as addPair but pair is only allowed if
	 *  - difference is not only a number or a single character
	 *  - overlap is not only a number or a single character
	 * @param first
	 * @param second
	 * @param overlapRatio
	 * @param maxSynLength
	 * @return
	 */
	public boolean addPairSpecialRules(String first, String second, double overlapRatio,
			int maxSynLength) {
		double termOverlap = Utils.getOverlapRatio(first, second);
	

	
		String[] firstArray = first.split(" ");
		String[] secondArray = second.split(" ");
		int firstLength = firstArray.length;
		int secondLength = secondArray.length;

		if (termOverlap >= overlapRatio) {
			
			TreeSet<String> common = Utils.getCommonWords(firstArray, secondArray);			
			boolean onlyNumCharCommon = true;
			for (Iterator<String> iter = common.iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				if (!element.matches("([0-9]+|[a-z])")) {
					onlyNumCharCommon=false;
				}
			}
			
			TreeSet<String> different = Utils.getDifferentWords(firstArray, secondArray);	
			boolean onlyNumCharDifferent = true;
			for (Iterator<String> iter = different.iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				if (!element.matches("([0-9]+|[a-z])")) {
					onlyNumCharDifferent=false;
				}
			}

//			System.out.println(first + "\t " + second);
//			System.out.println("diff:" + different);
//			System.out.println("comm: " + common);

			
			if (onlyNumCharCommon || onlyNumCharDifferent) {
				//System.out.println("not adding");
				return false;
			} else {
				//System.out.println("adding");
				return true;
			}

		} else {
			return false;
		}
	}

	/**
	 * Stores normalized 'TRUE 'entities into data file so we won't have to do
	 * this over and over again.
	 * 
	 * Expected format of complete list:
	 * col1: synonym (normalized)
	 * col2: ID
	 * tab betweeb both columns
	 * 
	 * 
	 * @param entities
	 *            complete list of all entities
	 */
	void makeTrueList(File completeList, File storeList) {
		LOGGER.debug("makeTrueList() - started ...");
		try {
			BufferedReader fileIn = new BufferedReader(new FileReader(
					completeList));
			ArrayList<String[]> rows = new ArrayList<String[]>();
			String text;
			while ((text = fileIn.readLine()) != null) {
				rows.add(text.split("\t"));
			}
			fileIn.close();

			// decides for every pair of String, if it should be
			// added to the learning process (at least one word in common
			// [normalized!]).
			FileWriter outTrue = new FileWriter(storeList);

			long time = System.currentTimeMillis();
			for (int i = 0; i < rows.size(); ++i) {
				String first = rows.get(i)[0];
				for (int j = i + 1; j < rows.size(); ++j) {
					if (rows.get(i)[1].equals(rows.get(j)[1])) {
						String second = rows.get(j)[0];
						if (addPair(first, second, OVERLAP_RATIO, MAXSYN_LENGTH)) {
							outTrue.write(first + "\t" + second + "\tTRUE\n");
						}
					} else {
						break;
					}
				}
			}
			long time2 = System.currentTimeMillis();

			outTrue.flush();
			outTrue.close();
			LOGGER.debug("makeTrueList() - took: " + (time2 - time));
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	/**
	 * Reads first String, second String & class ('TRUE' or 'FALSE') out of
	 * file.
	 * 
	 * @param listFile
	 *            File containing pairs of Strings + boolean
	 * @return
	 */
	ArrayList<String[]> readList(File listFile) {
		ArrayList<String[]> list = new ArrayList<String[]>();
		try {
			BufferedReader fileIn = new BufferedReader(new FileReader(listFile));
			String text;
			while ((text = fileIn.readLine()) != null) {
				list.add(text.split("\t"));
			}
			fileIn.close();
		} catch (IOException io) {
			io.printStackTrace();
		}
		return list;
	}

	/**
	 * TODO: comment!
	 * 
	 * @param S1
	 * @param S2
	 * @return
	 */
	public String[][] compareStrings(String S1, String S2) {

		String[] split1 = S1.split(" ");
		String[] split2 = S2.split(" ");
		TreeSet<String> sameWords = new TreeSet<String>();
		TreeSet<String> diffWords = new TreeSet<String>();
		
		
		boolean equal;
		// First loop finds all words in common &
		// some of the different words.
		
		for (int i = 0; i < split1.length; ++i) {
			equal = false;
			for (int j = 0; j < split2.length; ++j) {
				if (split1[i].equals(split2[j])) {
					sameWords.add(split1[i]);
					equal = true;
				}
			}
			if (!equal) {
				diffWords.add(split1[i]);
			}
		}
		
		// Now looping through second String.
		// Finds all differences that are left over from first loop.
		
		for (int i = 0; i < split2.length; ++i) {
			equal = false;
			for (int j = 0; j < split1.length; ++j) {
				if (split2[i].equals(split1[j])) {
					equal = true;
				}
			}
			if (!equal) {
				diffWords.add(split2[i]);
			}
		}
		/*
		System.out.println(S1 + " :: " + S2);
		System.out.print("SAMES = ");
		for(String same : sameWords)
			System.out.print(same + "; ");
		
		System.out.print("\nDIFFS = ");
		
		for(String diff : diffWords)
			System.out.print(diff + "; ");
		
		System.out.println("\n");
		*/
		return (new String[][] { sameWords.toArray(new String[] {}),
				diffWords.toArray(new String[] {}) });
	}
}