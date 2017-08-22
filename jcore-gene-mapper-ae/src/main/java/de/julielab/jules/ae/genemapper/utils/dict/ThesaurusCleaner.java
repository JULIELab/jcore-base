/**
 * ThesaurusCleaner.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 1.3
 * Since version:   1.3
 *
 * Creation date: Aug 02, 2007 
 * 
 * Selects lines from BioThesaurus according to column content.
 * 
 * cleaning definition
 * - negative entries: all lines removed that contain ANY of the negative entries
 * - positive entries: all lines removed that contain ANY of the positive entries
 * - column 3 (sources) extra handled: kept only when non of the sources is part of the negative list
 * - specifiy a positive entry on column 1 if no other positive entries are given
 * 
 * example:
 * - to select only lines for a specific organism add a positive entry on column 13 with each organism id to be kept
 * - to select only lines where the synonym doesn't contain the word "hypothetical" add a negative entry on column
 * 2 and a positive on column 1 (means that all taken except when negative rules are applied)
 **/

package de.julielab.jules.ae.genemapper.utils.dict;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.TreeSet;
import java.io.FileWriter;


public class ThesaurusCleaner {

	private boolean debug = false;
	
	ArrayList<Column> positive;

	ArrayList<Column> negative;

	ArrayList<String> negSources;
	
	TreeSet<String> acSet;

	public ThesaurusCleaner() {
		this.positive = new ArrayList<Column>();
		this.negative = new ArrayList<Column>();
		this.negSources = new ArrayList<String>();
		this.acSet = new TreeSet<String>();
	}

	/**
	 * Parses a text files with the following format: colNumber \t
	 * [+|-]ruleString
	 * 
	 * @param selection
	 *            file containing the rules
	 */
	private void readSelection(File selection) {
		BufferedReader bis = null;
		try {
			bis = new BufferedReader(new FileReader(selection));
			String line;
			System.out.println("------------------ cleaning definition ----------");
			while ((line = bis.readLine()) != null) {
				// ignore comments
				if(line.startsWith("##")) {
					continue;
				}
				String[] values = line.split("\t");
				if (values[1].charAt(0) == '+') {
					System.out.println("cleaning def: positive entry: " + line);
					positive.add(new Column(Integer.parseInt(values[0]), values[1]
							.substring(1), Boolean.parseBoolean(values[2])));
				} else if (values[1].charAt(0) == '-') {
					System.out.println("cleaning def: negative entry: " + line);
					int col = (int) Integer.parseInt(values[0]);
					// special treatment for all BT sources
					if (col == 3) {
						negSources.add(values[1].substring(1));
					} else {
						System.out.println(col);
						negative.add(new Column(col, (String) values[1].substring(1), Boolean.parseBoolean(values[2])));
					}
				} else {
					System.err.println("Undefined sequence!");
					break;
				}
			}
			System.out.println("-----------------------------------------\n");
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			try {if (bis != null) {bis.close();}} catch (IOException e) {};
		}
	}

	/**
	 * checks, if all sources are not trustworthy as stated by the rules
	 * (selection)
	 * 
	 * @param sources
	 *            the content of column 3
	 * @return
	 */
	private boolean trustSources(String sources) {
		boolean trust = true;
		int neg = 0;
		String[] split = sources.split("\\+\\+\\+");

		for (int i = 0; i < split.length; ++i) {
			for (int j = 0; j < negSources.size(); ++j) {
				if (split[i].contains(negSources.get(j))) {
					++neg;
					break;
				}
			}
		}
		// all sources were found by the rules
		if (neg == split.length) {
			trust = false;
		}

		return trust;
	}

	/**
	 * Performs a check on every line of the BioThesaurus.
	 * 
	 * @param from
	 *            source file
	 * @param to
	 *            clean version
	 */
	void pickLines(File from, String to) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(from));
			FileWriter writer = new FileWriter(to);
			boolean removeLine;
			String line;
			int counter = 0;
			while ((line = reader.readLine()) != null) {
				removeLine = false;
				
				String[] fields = line.split("\t");
				
				//entry is skipped, if it is not reviewed
				if (!acSet.contains(fields[0])){
					debug("removing because of review status: " + fields[0]);
					continue;
				}
				if(acSet.contains("Q8N2L1")){
					debug("Q8N2L1 is in AC set!!!!!");
				}
				
				/*
				 * negative list
				 */
				// if the column does contain an element from this list,
				// the line is skipped
				for (Iterator iter = negative.iterator(); iter.hasNext();) {
					Column column = (Column) iter.next();
					if(!column.getMatchStyle()){
						if (fields[column.getColumn() - 1].contains(column
								.getFilter())) {
							removeLine = true;
							debug("removing because of negative filter: " + column.getFilter() 
									+ "\t" + column.getColumn());						
							break;
						}
					} else {
						if (fields[column.getColumn() - 1].equals(column
								.getFilter())) {
							removeLine = true;
							debug("removing because of negative filter: " + column.getFilter() 
									+ "\t" + column.getColumn());						
							break;
						}
					}
				}
				
				/*
				 * check column 3: sources
				 */
				
				if (negSources.size() > 0) {
					// columns 3 contains all BT sources.
					if (!trustSources(fields[2])) {
						removeLine = true;
						debug("removing because of source: " + fields[2]);
					}
				}
	
				if (!removeLine){
					//check for char entries and 'all digits' token
					String[] tokens = fields[1].split("\\s");
					if (tokens.length >= 2){
					} else {
						if (tokens[0].length() == 1){
							removeLine = true;
							continue;
						}
						if (tokens[0].matches("[0-9]+")){
							removeLine = true;
							continue;
						}
					}
				}
				
				/*
				 * positive list
				 */
				if (!removeLine) {
					// if the column doesn't contain all element from the positive list,
					// the line is skipped
					removeLine = true;
					for (Iterator iter = positive.iterator(); iter.hasNext();) {
						Column column = (Column) iter.next();
						if (!column.getMatchStyle()){
							if (fields[column.getColumn() - 1].contains(column
									.getFilter())) {
								removeLine = false;
								debug("keeping because of positive filter: " + column.getFilter() 
										+ "\t" + column.getColumn());
								break;
							}
						} else{
							debug("THERE: " + fields[0]);
							if (fields[column.getColumn() - 1].equals(column
									.getFilter())) {
								removeLine = false;
								debug("keeping because of positive filter: " + column.getFilter() 
										+ "\t" + column.getColumn());
								break;
							}
						}
					}
				}
				
				/*
				 * write line if not to be removed
				 */
				if (!removeLine) {
					++counter;
					if (counter % 10000 == 0){
						System.out.println("# cleaned entries: " + counter);
					}
					writer.write(line + "\n");
					// Without this, each file will be 8kB or less.
					writer.flush();
				}
			}
			reader.close();
			writer.close();
			System.out.println(counter);
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	/**
	 * to execute the thesaurus cleaner start it with the following command-line
	 * arguments: arg0: selectionFile arg1: thesaurusFile arg2: outputFile
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ThesaurusCleaner select = new ThesaurusCleaner();
		if (args.length != 4) {
			System.err
					.println("Usage: ThesaurusCleaner <cleaning definition> <BT file> " +
							"<UniProt AC set> <outout file>");
			System.exit(-1);
		}
		File selection = new File(args[0]);
		File thesaurus = new File(args[1]);
		File uniprotAC = new File(args[2]);
		String outFile = args[3];
		if (!selection.isFile()) {
			System.err.println("Could not find specified cleaning definition file: " + selection.toString());
			System.exit(-1);
		} else if (!thesaurus.isFile()) {
			System.err.println("Could not find specified thesaurus file: " + thesaurus.toString());
			System.exit(-1);
		} else if (!thesaurus.isFile()) {
			System.err.println("Could not find specified thesaurus file: " + thesaurus.toString());
			System.exit(-1);
		}
		ObjectInputStream ois;
		try {
			System.out.println("reading set of AC entries...");
			ois = new ObjectInputStream(new FileInputStream(uniprotAC));
			select.acSet = (TreeSet<String>) ois.readObject();	
			select.readSelection(selection);
			System.out.println("cleaning thesaurus...");
			long t1 = System.currentTimeMillis();
			select.pickLines(thesaurus, outFile);
			long t2 = System.currentTimeMillis();
			System.out.println("done");
			System.out.println("time needed for cleaning: " + (t2-t1)/1000 + " sec");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}
	
	private void debug(String text) {
		if(debug) {
			System.err.println("[DBG]\t" + text);
		}
	}
}