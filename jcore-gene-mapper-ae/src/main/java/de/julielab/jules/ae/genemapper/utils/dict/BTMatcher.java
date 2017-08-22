package de.julielab.jules.ae.genemapper.utils.dict;

/** 
 * BTMatcher.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 1.3.1
 * Since version:   1.3
 *
 * Creation date: Sep 15, 2007 
 * 
 * Finds synonyms in collections of text and returns
 * their frquencies. Will find only synonyms that are not subtoken.
 * Intended to be used for BioThesaurus cleanup.
 **/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BTMatcher {

	private int SYN_FIELD = 1;
	private int ID_FIELD = 0;
	// <Synonyms, <matching UniRef50 IDs, counter>>
	private HashMap<String, RefCounter> btEntities;

	BTMatcher() {
		this.btEntities = new HashMap<String, RefCounter>();
	}

	/**
	 * Collects all synonyms and corresponding UniRef50 IDs. (Counter is
	 * initialized for later usage.)
	 * 
	 * @param btFile
	 *            BioThesaurus
	 */
	private void readBT(File btFile) {
		try {
			BufferedReader btReader = new BufferedReader(new FileReader(btFile));
			String line;
			System.out.println("Importing BioThesaurus: ");
			int counter = 1;
			while ((line = btReader.readLine()) != null) {
				String[] split = line.split("\t");
				// Synonym does not have an UniRef ID entry
				if (split[ID_FIELD].equals(" ") || split[ID_FIELD].equalsIgnoreCase("null")) {
					continue;
				}
				String escape = cleanEntry(split[SYN_FIELD]);

				// Synonym is already listed.
				if (btEntities.containsKey(escape)) {
					TreeSet<String> check = btEntities.get(escape).getUniRefs();
					// add UniRef, if it is new
					if (!check.contains(split[ID_FIELD])) {
						check.add(split[ID_FIELD]);
						btEntities.put(escape, new RefCounter(check));
					}

				} else { // new entry
					btEntities.put(escape, new RefCounter(split[ID_FIELD]));
				}

				if (counter % 10000 == 0) {
					System.out.println("Read: " + counter);
				}

				++counter;
			}
			btReader.close();

			System.out.println("Stored synonyms: " + btEntities.size());
			System.out.println("Read entries: " + --counter);
		} catch (FileNotFoundException file) {
			file.printStackTrace();
		} catch (IOException io) {
			io.printStackTrace();
		}
	}

	private String cleanEntry(String escape) {
		//delete commas and hyphens (token)
		escape = escape.replaceAll("-|,", "");
		//TODO: Change 'replace' lines into a compact form (groups!)
		//Change string, so it will work as pattern
		escape = escape.replace(")", "\\)");	//('unmatched closing' bug)
		escape = escape.replace("(", "\\(");	//same here
		escape = escape.replace("}", "\\}");	//'illegal repetition' fix
		escape = escape.replace("{", "\\{");	//same here
		escape = escape.replace("]", "\\]");	//false recognition of tokens
		escape = escape.replace("[", "\\[");	//	->[g]c recognizes gc
		escape = escape.replace("*", "\\*");	//won't be recognized without
		escape = escape.replace("+", "\\+");	//same here
		escape = escape.replace("|", "\\|");	//lead to 'SubToken'
		return escape;
	}

	/**
	 * Directs file content into a string.
	 */
	private String readText(File text) {
		StringBuffer textBuffer = new StringBuffer("");
		BufferedReader textReader = null;
		try {
			textReader = new BufferedReader(new FileReader(text));
			String line;
			while ((line = textReader.readLine()) != null) {
				textBuffer.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {if (textReader != null) {textReader.close();}} catch (IOException e) {};
		}
		//delete commas and hyphens (text)
		return textBuffer.toString().replaceAll("-|,", "");
	}

	/**
	 * Mines given text for all listed synonyms and counts their occurrences.
	 */
	public HashMap<String, RefCounter> getFrequencies(String myText) {
		for (Iterator<String> iter = btEntities.keySet().iterator(); iter.hasNext();) {
			String btEntry = iter.next();
			RefCounter ref = btEntities.get(btEntry);
			Pattern btPattern = Pattern.compile("\\b" +  //word boundary
					btEntry + "\\b", Pattern.CASE_INSENSITIVE);
			Matcher btMatcher = btPattern.matcher(myText);
			while (btMatcher.find()) {
				ref.increment();
			}
			btEntities.put(btEntry, ref);
		}
		return btEntities;
	}

	/**
	 * to execute the BTMatcher start it with the following command-line
	 * arguments: arg0: thesaurusFile 
	 * arg1: directory of text files or single file
	 * arg2: output file
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
	
		if (args.length != 3) {
			System.err.println("Usage: BTMatcher <BT file> <text/dir> <outfile>");
			System.exit(-1);
		}
		File bt = new File(args[0]);
		File textSource = new File(args[1]);
		if (!bt.isFile()) {
			System.err.println("Could not find bt file");
			System.exit(-1);
		}
		File outFile = new File(args[2]);

		BTMatcher btMatcher = new BTMatcher();
		HashMap<String, RefCounter> freqs = new HashMap<String, RefCounter>();
		
		if (textSource.isFile()) {
			btMatcher.readBT(bt);
			String text = btMatcher.readText(textSource);
			freqs = btMatcher.getFrequencies(text);
		} else if (textSource.isDirectory()) {
			File[] content = textSource.listFiles();
			btMatcher.readBT(bt);
			for (int i = 0; i < content.length; ++i) {
				System.out.println("processing file: " + content[i]);
				String text = btMatcher.readText(content[i]);
				freqs = btMatcher.getFrequencies(text);
			}
		} else {
			System.err.println("Could not find file: " + args[1]);
		}

		// now write output to file
		writeFreqs(freqs, outFile);
		
		System.out.println("\n\n matching finished. Output written to: " + outFile.toString());
		
	}

	
	private static void writeFreqs(HashMap<String, RefCounter> freqs, File outFile) throws IOException {
		System.out.println("writing results to file...");
		FileWriter fw = new FileWriter(outFile);
		for (Iterator<String> iter = freqs.keySet().iterator(); iter.hasNext();) {
			String key = iter.next();
			RefCounter refC = freqs.get(key);
			int counter = refC.getCounter();
			TreeSet<String> unirefs = refC.getUniRefs();
			for (Iterator<String> iterator = unirefs.iterator(); iterator.hasNext();) {
				String unirefID = iterator.next();
				String info = counter + "\t" + key + "\t" + unirefID;
				//System.out.println(info);
				fw.write(info + "\n");
			}
		}
		fw.close();
	}
}
