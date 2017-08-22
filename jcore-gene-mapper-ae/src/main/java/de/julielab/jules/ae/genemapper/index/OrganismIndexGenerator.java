/** 
 * OrganismIndexGenerator.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.5.1
 * Since version:   1.5.1
 *
 * Creation date: Nov 19, 2007 
 * 
 * This class generates the Lucene index for Uniprot IDs for the purpose
 * to check whether Uniprot protein exists for a particular organism
 * 
 **/

package de.julielab.jules.ae.genemapper.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

public class OrganismIndexGenerator {

	private static final File BIOTHESAURUS_FILE = new File("/home/jwermter/Biothesaurus/bt_iproclass.uniprot_ids.unique");
	//private static final File BIOTHESAURUS_FILE = new File("/home/jwermter/Biothesaurus/t");

			
	private static final File INDEX_FILE = new File("src/main/resources/organism_index");
	//"/home/tomanek/tmp/li/bt_ipro_ilruleXX");
	//"src/test/resources/lucene_test_index");

		
	/*
	 * define some fields in the index:
	 * SYN_FIELD: this field is to be searched
	 * ID_FIELD: there the id is stored
	 * LOOKUP_SYN_FIELD: the synonym is stored again here (needed for calculating the score)
	 */
	public static final String ORGANISM_FIELD = "uniprot_id";
	
	private File biothesaurusFile;

	Directory indexDirectory;

	private static final boolean debug = false;

	/**
	 * run me here !
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		long s1 = System.currentTimeMillis();
		OrganismIndexGenerator indexGenerator;
		try {
			indexGenerator = new OrganismIndexGenerator(BIOTHESAURUS_FILE, INDEX_FILE);

			indexGenerator.createIndex();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long s2 = System.currentTimeMillis();

		System.out.println("Index created successfully! (" + (s2 - s1) / 1000
				+ " sec)");
	}

	/**
	 * constructor which creates index in the specified directory on the disk
	 */
	public OrganismIndexGenerator(File biothesaurusFile,
			File indexFile) throws FileNotFoundException, IOException {
		this.biothesaurusFile = biothesaurusFile;
		indexDirectory = createIndexDirectory(indexFile);
	}

	/**
	 * constructor which creates index in temporarily in memory
	 */
	
	public OrganismIndexGenerator(File biothesaurusFile)
			throws FileNotFoundException, IOException {
		this.biothesaurusFile = biothesaurusFile;
		indexDirectory = new RAMDirectory();
		
	}

	/**
	 * create the index, i.e. read from the biothesaurus file (which
	 * is expected only to contain the uniprot ids!) and then write it to the index.
	 * 
	 * @throws IOException
	 */
	public void createIndex() throws IOException {

		WhitespaceAnalyzer analyser = new WhitespaceAnalyzer();

		IndexWriter iw = new IndexWriter(indexDirectory, analyser, true);

		//TermNormalizer normalizer = new TermNormalizer();

		BufferedReader biothesaurusReader = new BufferedReader(new FileReader(
				biothesaurusFile));

		// counter for file entries processed
		int counter = 0;
		
		System.out.println("Generating index now. This may take quite a while (up to several hours when file is large) ...");
		// now loop thourgh biothesaurus and add entries to the index
		try {

			String line = "";
			while ((line = biothesaurusReader.readLine()) != null) {

				String id = line.trim();
				String[] values = id.split("_");

				// check whether format is OK
				if (values.length != 2) {
					System.err
							.println("ERR: File not in expected format. \ncritical line: "
									+ line);
					System.exit(-1);
				}

				
				showDebug(id);

				// make fields
				Field idField = new Field(OrganismIndexGenerator.ORGANISM_FIELD, new StringReader(id));
				//Field lookupIdField = new Field(ORGANISM_FIELD, id,
					//	Field.Store.YES, Field.Index.UN_TOKENIZED);

				// make document and add to index
				Document d = new Document();
				d.add(idField);
				iw.addDocument(d);
				
				++counter;
				if (counter % 10000 == 0){
					System.out.println("# entries processed: " + counter);
				}
			}

			// finally optimize the index and close it
			iw.optimize();
			iw.close();

			biothesaurusReader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * create the directory object where to put the lucene index...
	 */
	private FSDirectory createIndexDirectory(File indexFile) {
		FSDirectory fdir = null;
		try {
			fdir = FSDirectory.getDirectory(indexFile, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fdir;
	}

	private void showDebug(String s) {
		if (debug) {
			System.out.println(s);
		}
	}

}
