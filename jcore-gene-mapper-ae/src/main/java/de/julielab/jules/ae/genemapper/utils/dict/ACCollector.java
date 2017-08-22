/**
 * ACCollector.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: kampe
 * 
 * Current version: 1.3.1
 * Since version:   1.3.1
 *
 * Creation date: Oct 09, 2007 
 * 
 * Extracts all AC entries from the complete UNIProtKB/TrEMBL data set in flat 
 * file format (uniprot_tremble.dat). Only the first ID of every entries gets 
 * stored for later usage. This will help cleaning up the BioThesaurus.
 * 
 * 
 **/

package de.julielab.jules.ae.genemapper.utils.dict;

import java.io.File;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.TreeSet;
/**
 * @deprecated Check if this class is used anywhere (resource scripts?)
 * @author faessler
 *
 */
@Deprecated
public class ACCollector {
	
	TreeSet<String> acSet;
	
	ACCollector(){
		this.acSet = new TreeSet<String>();
	}
	
	/**
	 * Collects all AC lines in a set and saves them for later usage.
	 * @param sprotFile UniProtKB data set.
	 * @param dumpFile Set gets stored in this file.
	 */
	private void findACs(File sprotFile, String dumpFile) {
		BufferedReader bis = null;
		try {
			bis = new BufferedReader(new FileReader(sprotFile));
			String line;
			System.out.println("------------------ collecting AC entries ----------");
			int counter = 1;
			while ((line = bis.readLine()) != null) {
				//AC entry looks like this:
				//AC   id1; id2;...
				if (line.startsWith("AC")){
					String[] values = line.split("   ");
					int limiterPos = values[1].indexOf(';');
					String id = values[1].substring(0, limiterPos);
					acSet.add(id);
					
					if (counter % 10000 == 0){
						System.out.println("Read entries: " + counter);
					}
					line = bis.readLine();
					line = bis.readLine();
					line = bis.readLine();line = bis.readLine();
					line = bis.readLine();
					line = bis.readLine();
					
					++counter;
				}
			}
			System.out.println("Total entries: " + counter);
			System.out.println("------------------ writing set of AC IDs -----------------------\n");
			
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dumpFile));
			oos.writeObject(acSet);
			oos.flush();
			oos.close();
			bis.close();
			
			System.out.println("------------------ All done! -----------------------\n");
			
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			try {if (bis != null) {bis.close();}} catch (IOException e) {};
		}
	}

	/**
	 * @param args
	 * This class expects 2 parameters:
	 * sprotFile: location of UniProt data set
	 * outFile: storage location for results
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: ACCollector <UniProtKB data set> <output file>");
			System.exit(-1);
		}
		
		//uniprot_sprot.dat
		File sprotFile = new File(args[0]);
		String outFile = args[1];
		
		if (!sprotFile.isFile()) {
			System.err.println("Could not find specified UniProtKB file: " + sprotFile.toString());
			System.exit(-1);
		}
		
	   ACCollector collector = new ACCollector();
	   collector.findACs(sprotFile, outFile);

	}

}