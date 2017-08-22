/** 
 * DictionaryClusterCleaner.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.6 	
 * Since version:   1.6
 *
 * Creation date: Feb 18, 2008 
 * 
 * reads in a whole dictionary (format: synonym<tab>id), 
 * makes clusters for each id (hashmap<String, ArrayList<String>>) 
 * and removes some entries from clusters if:
 * 
 * Rule 1: underspecified synonym: another synonym is more specific than current one,
 * i.e. an extension by a number, a single letter, or a greek letter (alpha, beta, gamma)...
 *  
 **/

package de.julielab.jules.ae.genemapper.utils.dict;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.julielab.jules.ae.genemapper.utils.IOUtils;

public class DictionaryClusterCleaner {

	static String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";

	HashMap<String, TreeSet<String>> clusters = new HashMap<String, TreeSet<String>>();

	DictionaryClusterCleaner(File dict) throws FileNotFoundException,
			IOException {
		System.out.println("reading from file: " + dict);
		ArrayList<String> dictList = IOUtils.readFile2ArrayList(dict);
		System.out.println("building clusters...");
		for (String entry : dictList) {
			String[] values = entry.split("\t");
			if (values.length != 2) {
				System.err.println("unexpected line in dict: " + entry);
				System.exit(-1);
			}
			add2Clusters(values[1], values[0]);
		}
		
		
	}

	private void applyRule1() {
		
		System.out.println("cleaning clusters by rule1...");
		for (Iterator<String> iter = clusters.keySet().iterator(); iter.hasNext();) {
			// System.out.println("\n------ ");
			String id = (String) iter.next();
			TreeSet<String> syns = clusters.get(id);
			TreeSet<String> newSyns = new TreeSet<String>(syns);

			// loop over all ids
			for (String syn1 : syns) {
				// define a pattern
				//Pattern pat = Pattern.compile("(" + syn1 + ")"
				//		+ " ([a-z]|[A-Z]|[0-9]|" + GREEK + ")");
				
				String[] v = syn1.split("\\s+");
				if (v.length>1)
					continue;
				Pattern pat = Pattern.compile("(" + syn1 + ")"
						+ " [0-9]");
				
				// System.out.println("syn1: " + syn1 + " --> " +
				// pat.pattern());
				for (String syn2 : syns) {
					if (syn1.equals(syn2))
						continue;
					// System.out.println(" --> syn2: " + syn2);
					Matcher m = pat.matcher(syn2);
					if (m.matches()) {
						// don't add to dict
						// System.out.println(" => remove syn 1: " + syn1);
						newSyns.remove(syn1);
					} else {
						// keep in dict
						// System.out.println(" => keep syn 1: " + syn1);
					}
				}
			}

			// System.out.println("\n syn: " + newSyns);
			clusters.put(id, newSyns);
		}

	}

	private void add2Clusters(String id, String syn) {
		TreeSet<String> syns = new TreeSet<String>();
		if (clusters.containsKey(id)) {
			syns = clusters.get(id);
		}
		syns.add(syn);
		clusters.put(id, syns);
	}

	private void writeClusters2File(File outFile) throws IOException {
		FileWriter fw = new FileWriter(outFile);

		for (Iterator<String> iter = clusters.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			TreeSet<String> syns = clusters.get(id);
			for (String syn : syns) {
				fw.write(syn + "\t" + id + "\n");
			}

		}
		fw.close();
		System.out.println("written to file: " + outFile);
	}

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		DictionaryClusterCleaner d = new DictionaryClusterCleaner(new File(
				"/tmp/entrezGeneUniprotHGNC_Lexicon.unique"));
		//System.out.println(d.clusters);

		d.applyRule1();

		//System.out.println(d.clusters);
		d.writeClusters2File(new File("/tmp/newdict"));
	}

}
