/** 
 * ContextGenerator.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: wermter
 * 
 * Current version: 1.5.1
 * Since version:   1.0
 *
 * Creation date: Feb 04, 2008
 * 
 * This class builds the semantic context for a given gene dictionary in
 * a bag-of-words fashion. It is used by the ContextIndexGenerator.java
 * 
 * 
 **/

package de.julielab.jules.ae.genemapper.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.julielab.jules.ae.genemapper.CandidateFilter;


public class ContextGenerator {

	/*
	 * Best contexts are:
	 * - GENE2SUMMARY_FILE
	 * - GENE2GO_FILE
	 * - GO2SYNS_FILE
	 * - GENE2UP_CONTEXT_FILE
	 * - GENE2DESIGNATIONS_FILE
	 * 
	 * for BC2, GENE2GENERIF_FILE cause performance loss of 1% F1!
	 * 
	 * for (real-world) StemNet: GENE2INTERACTION_FILE is necessary (see CD25)
	 * although in BC2, you lose 0.2% F1
	 */
	
	// BC2 new:
	//private static final File GENE2SUMMARY_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/eg_index_resources_human/gene2summary");
	//private static final File GENE2GO_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/eg_index_resources_human/gene2go");
	//private static final File GO2SYNS_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/eg_index_resources_human/go_all");
	//private static final File GENE2UP_CONTEXT_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/eg_index_resources_human/eg2freetext_chromo.context");
	//private static final File GENE2DESIGNATIONS_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/eg_index_resources_human/eg2designation");
	//private static final File GENE2GENERIF_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/eg_index_resources/eg2generif");
	
	// BC2 old:
	//private static final File GENE2SUMMARY_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/gene2summary.human");
	//private static final File GENE2GO_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/gene2go.human");
	//private static final File GO2SYNS_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/go_all");
	//private static final File GENE2UP_CONTEXT_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/entrez2freetext_chromosome");
	//private static final File GENE2DESIGNATIONS_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/entrezGeneDesignationsForSemanticContext");
	//private static final File GENE2GENERIF_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/eg2generifs");
	//private static final File GENE2GENERIF_FILE = new File("/home/jwermter/uniprot.dict.eg");
	
	// Stemnet:
	
	//private static final File GENE2SUMMARY_FILE = new File("/data/data_resources/biology/up_index_resources/up2summary");
	//private static final File GENE2GO_FILE = new File("/data/data_resources/biology/up_index_resources/up2go");
	//private static final File GO2SYNS_FILE = new File("/data/data_resources/biology/up_index_resources/go_all");
	//private static final File GENE2UP_CONTEXT_FILE = new File("/data/data_resources/biology/up_index_resources/up2freetext_chromo.context");
	//private static final File GENE2DESIGNATIONS_FILE = new File("/data/data_resources/biology/up_index_resources/up2designation");
	//private static final File GENE2GENERIF_FILE = new File("/data/data_resources/biology/up_index_resources/up2generif");
	//private static final File GENE2INTERACTION_FILE = new File("/data/data_resources/biology/up_index_resources/up2interaction");
	// Update Engelmann:
//	private static final File GENE2SUMMARY_FILE = new File("/home/engelmann/geno/Semantic_Context_new/up_index_resources/up2summary");
//	private static final File GENE2GO_FILE = new File("/home/engelmann/geno/Semantic_Context_new/up_index_resources/up2go");
//	private static final File GO2SYNS_FILE = new File("/home/engelmann/geno/Semantic_Context_new/up_index_resources/go_all");
//	private static final File GENE2UP_CONTEXT_FILE = new File("/home/engelmann/geno/Semantic_Context_new/up_index_resources/up2freetext_chromo.context");
//	private static final File GENE2DESIGNATIONS_FILE = new File("/home/engelmann/geno/Semantic_Context_new/up_index_resources/up2designation");
//	private static final File GENE2GENERIF_FILE = new File("/home/engelmann/geno/Semantic_Context_new/up_index_resources/up2generif");
//	private static final File GENE2INTERACTION_FILE = new File("/home/engelmann/geno/Semantic_Context_new/up_index_resources/up2interaction");
	
	// Original lines used by Wermter / Tomanek
//	private static final File GENE2SUMMARY_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/up_index_resources/up2summary");
//	private static final File GENE2GO_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/up_index_resources/up2go");
//	private static final File GO2SYNS_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/up_index_resources/go_all");
//	private static final File GENE2UP_CONTEXT_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/up_index_resources/up2freetext_chromo.context");
//	private static final File GENE2DESIGNATIONS_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/up_index_resources/up2designation");
//	//private static final File GENE2GENERIF_FILE = new File("/data/data_resources/biology/up_index_resources/up2generif");
//	private static final File GENE2INTERACTION_FILE = new File("/mnt/data_stemnet/resources/dictionaries/gene_dictionaries/stemnet/up_index_resources/up2interaction");
	
	//private static final File GENE2SYNS_FILE = new File("/data/data_resources/biology/up_index_resources/gene.dict.up");
	
	// BC2
	//private static final File GENE2UP_CONTEXT_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/entrez2freetext_chromosome.test");
	// Stemnet
	
	
	//private static final File GENE2INTERACTION_FILE = new File("/home/jwermter/biocreative2_data/semantic_context/eg2interaction");

	
	
	private HashMap<String, String> id2go = new HashMap<String, String>();
	private HashMap<String, String> go2syns = new HashMap<String, String>();
	private HashMap<String, String> id2summary = new HashMap<String, String>();
	private HashMap<String, String> id2designations = new HashMap<String, String>();
	private HashMap<String, String> id2upContext = new HashMap<String, String>();
	private HashMap<String, String> id2intact = new HashMap<String, String>();
	private HashMap<String, String> id2generif = new HashMap<String, String>();
	private HashMap<String, String> gene2syns = new HashMap<String, String>();
	
	private static final boolean debug = false;

	

	/**
	 * constructor which creates semantic context index in the specified directory on the disk
	 * file paths are hard-coded due to size issues
	 * 
	 * @param resourcesDir path to resources directory
	 * @param db type of id (i.e. either "gene" or "protein")
	 * 
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
	public ContextGenerator(String resourcesDir, String db) throws FileNotFoundException, IOException {
		
//		readId2summaryFile(GENE2SUMMARY_FILE);
//		readId2GoFile(GENE2GO_FILE);
//		readGo2SynFile(GO2SYNS_FILE);
//		readId2UniprotContextFile(GENE2UP_CONTEXT_FILE);
//		readId2IntactFile(GENE2INTERACTION_FILE); // needed for StemNet
//		//readId2GeneRifFile(GENE2GENERIF_FILE);
//		//readGene2SynsFile(GENE2SYNS_FILE);
//		readId2egDesignationFile(GENE2DESIGNATIONS_FILE);
		
		String idType = "";
		
		if (db.equals("gene")) {
			idType = "eg";
		} else if (db.equals("protein")) {
			idType = "up";
		}
		
		File id2SummaryFile = (new File(resourcesDir + idType + "2summary"));
		if (!id2SummaryFile.isFile()) {
			System.err.println("Could not find file " + id2SummaryFile.getAbsolutePath());
			System.exit(-1);
		}
		File id2GoFile = (new File(resourcesDir + idType + "2go"));
		if (!id2GoFile.isFile()) {
			System.err.println("Could not find id2GoFile (" + idType + "2go)");
			System.exit(-1);
		}
		File go2SynFile = (new File(resourcesDir + "go_all"));
		if (!go2SynFile.isFile()) {
			System.err.println("Could not find go2SynFile (go_all)");
			System.exit(-1);
		}		
		File id2UniprotContextFile = (new File(resourcesDir + idType + "2freetext_chromo.context"));
		if (!id2UniprotContextFile.isFile()) {
			System.err.println("Could not find id2uniprotContextFile (" + idType + "2freetext_chromo.context)");
			System.exit(-1);
		}
		File id2InteractFile = (new File(resourcesDir + idType + "2interaction"));
		if (!id2InteractFile.isFile()) {
			System.err.println("Could not find id2InteractFile (" + idType + "2interaction)");
			System.exit(-1);
		}
		File id2EgDesignationFile = (new File(resourcesDir + idType + "2designation"));
		if (!id2EgDesignationFile.isFile()) {
			System.err.println("Could not find id2DesignationFile (" + idType + "2designation)");
			System.exit(-1);
		}
		
		readId2summaryFile(id2SummaryFile);
		readId2GoFile(id2GoFile);
		readGo2SynFile(go2SynFile);
		readId2UniprotContextFile(id2UniprotContextFile);
		readId2IntactFile(id2InteractFile);
		readId2EgDesignationFile(id2EgDesignationFile);
	}

	/**
	 * Builds the semantic context for a given Gene ID
	 * @param id - the gene id for which the semantic context is to be returned
	 * @return the semantic context
	 */

	public String getContext(String id) {
		
		String context = " ";
		
		if(id2upContext.get(id) != null) {
			context += id2upContext.get(id);
		}
				
		if(id2designations.get(id) != null) {
			context += " " + id2designations.get(id);
		}
				
		if(id2summary.get(id) != null) {
			context += " " + id2summary.get(id);
		}
		
		
		if(id2generif.get(id) != null) {
			context += " " + id2generif.get(id);
		}
		
		if(id2intact.get(id) != null) {
			context += " " + id2intact.get(id);
		}

				
		if(id2go.get(id) != null) {
			String goCodes = id2go.get(id);
		
			String[] codes = goCodes.split("\\|");
			String code = "";
			String syn = "";
			
			for (int i=0; i < codes.length; i++) {
				code = codes[i];
				//System.out.println(code);
				if(go2syns.get(code) != null) {
					syn = go2syns.get(code);
					syn = syn.replaceAll("\\|", " ");
					//System.out.println(code + ": " + syn);
					context += " " + syn;
				}
			}
		}
		
		
		
		if(gene2syns.get(id) != null) {
			String syn = gene2syns.get(id);
			syn = syn.replaceAll("\\|", " ");
			context += " " + syn;
		}
			
		
		//System.out.println(id + ": " + context);
		return context.trim();
	}
	
	
	private void readId2GoFile(File goFile) throws IOException{
		
		//id2go = new HashMap<String, String>();
		BufferedReader goReader = new BufferedReader(new FileReader(goFile));
		String line = "";
		
		while ((line = goReader.readLine()) != null) {
			
			if(line.startsWith("#"))
				continue;
			String[] values = line.split("\t");

			// check whether format is OK
			if (values.length != 2) {
				System.err
						.println("ERR: goFile not in expected format. \ncritical line: "
					+ line);
				System.exit(-1);
			}
			
			String id = values[0];
			String go = values[1].trim();
			
			if(id2go.containsKey(id)) {
				String goCodes = id2go.get(id);
				goCodes += "|" + go;
				id2go.put(id, goCodes.trim());
			} else {
				id2go.put(id, go);
			}
			
			
		}
		goReader.close();
		System.out.println("Done processing id2go file.");
	}
	
	private void readGo2SynFile(File goFile) throws IOException{
		
		//go2syns = new HashMap<String, String>();
		BufferedReader goReader = new BufferedReader(new FileReader(goFile));
		String line = "";
		
		while ((line = goReader.readLine()) != null) {
			
			if(line.startsWith("#"))
				continue;
			String[] values = line.split("\t");

			// check whether format is OK
			if (values.length != 2) {
				System.err
						.println("ERR: go2syns not in expected format. \ncritical line: "
								+ line);
				System.exit(-1);
			}
			
			String go = values[0];
			String syns = values[1].trim();
			
			go2syns.put(go, syns);
			
		}
		goReader.close();
		System.out.println("Done processing go2syns file.");
	}
	
	
	
	private void readGene2SynsFile(File geneFile) throws IOException{
		
		//go2syns = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new FileReader(geneFile));
		String line = "";
		
		while ((line = reader.readLine()) != null) {
			if(line.startsWith("#"))
				continue;
			
			String[] values = line.split("\t");

			// check whether format is OK
			if (values.length != 2) {
				System.err
						.println("ERR: gene2syns not in expected format. \ncritical line: "
								+ line);
				System.exit(-1);
			}
			
			String gene = values[0];
			String syns = values[1].trim();
			
			gene2syns.put(gene, syns);
			
		}
		reader.close();
		System.out.println("Done processing gene2syns file.");
	}
	
	
	
	
	
	
	
	private void readId2summaryFile(File id2sumFile) throws IOException{
		
		//id2summary = new HashMap<String, String>();
		BufferedReader id2sumReader = new BufferedReader(new FileReader(id2sumFile));
		String line = "";
		
		while ((line = id2sumReader.readLine()) != null) {
			if(line.startsWith("#"))
				continue;
			String[] values = line.split("\t");
			// check whether format is OK
			if (values.length != 2) {
				System.err
						.println("ERR: id2summary not in expected format. \ncritical line: "
								+ line);
				System.exit(-1);
			}
			
			String id = values[0];
			String summary = values[1].trim();
			
			id2summary.put(id, summary);
			
		}
		id2sumReader.close();
		System.out.println("Done processing id2summary file.");
	}
	
	
	private void readId2UniprotContextFile(File id2upContextFile) throws IOException{
		
		//id2upContext = new HashMap<String, String>();
		BufferedReader id2upContextReader = new BufferedReader(new FileReader(id2upContextFile));
		String line = "";
		
		while ((line = id2upContextReader.readLine()) != null) {
			String[] values = line.split("\t");
			if(line.startsWith("#"))
				continue;
			// check whether format is OK
			if (values.length != 2) {
				System.err
						.println("ERR: id2upContext not in expected format. \ncritical line: "
								+ line);
				System.exit(-1);
			}
			
			String id = values[0];
			String upContext = values[1].trim();
			
			if(id2upContext.containsKey(id)) {
				upContext += " " + id2upContext.get(id);
				id2upContext.put(id, upContext);
			} else {
				id2upContext.put(id, upContext);
			}

			
		}
		id2upContextReader.close();
		System.out.println("Done processing id2upContext file.");
	}
	
	
	private void readId2EgDesignationFile(File id2DesignFile) throws IOException{
		
		//id2upContext = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new FileReader(id2DesignFile));
		String line = "";
		CandidateFilter cf = new CandidateFilter();
		while ((line = reader.readLine()) != null) {
			
			if(line.startsWith("#"))
				continue;
			
			String[] values = line.split("\t");

			// check whether format is OK
			if (values.length != 2) {
				System.err
						.println("ERR: id2designate not in expected format. \ncritical line: "
								+ line);
				System.exit(-1);
			}
			
			String id = values[0];
			String designation = values[1].trim();
			
			Pattern p = cf.patternDomainFamilies;
			Matcher m = p.matcher(designation);
			if (m.matches()) {
				//System.out.println("DOMAIN/FAMILY REMOVED: |" + designation + "|");
				continue;
			}
			
			
			p = cf.patternUnspecifieds;
			m = p.matcher(designation);
			if (m.matches()) {
				//System.out.println("UNSPECIFIED REMOVED: |" + designation + "|");
				continue;
			}
			
			
			if(id2designations.containsKey(id)) {
				designation += " " + id2designations.get(id);
				id2designations.put(id, designation);
			} else {
				id2designations.put(id, designation);
			}

			
		}
		reader.close();
		System.out.println("Done processing id2designations file.");
	}
	
	
	
	private void readId2GeneRifFile(File geneRifFile) throws IOException {
		
		BufferedReader id2geneRifReader = new BufferedReader(new FileReader(geneRifFile));
		String line = "";
		
		while ((line = id2geneRifReader.readLine()) != null) {
			String[] values = line.split("\t");

			// check whether format is OK
			if (values.length != 2) {
				System.err
						.println("ERR: id2upContext not in expected format. \ncritical line: "
								+ line);
				System.exit(-1);
			}
			
			String id = values[0];
			String geneRifContext = values[1].trim();
			
			if(id2generif.containsKey(id)) {
				geneRifContext += " " + id2generif.get(id);
				id2generif.put(id, geneRifContext);
			} else {
				id2generif.put(id, geneRifContext);
			}

			
		}
		id2geneRifReader.close();
		System.out.println("Done processing id2GeneRif file.");
		
	}

	private void readId2IntactFile(File geneInteractFile) throws IOException {
		
		BufferedReader id2IntactReader = new BufferedReader(new FileReader(geneInteractFile));
		String line = "";
		
		while ((line = id2IntactReader.readLine()) != null) {
			String[] values = line.split("\t");

			// check whether format is OK
			if (values.length != 2) {
				System.err
						.println("ERR: id2upContext not in expected format. \ncritical line: "
								+ line);
				System.exit(-1);
			}
			
			String id = values[0];
			String geneIntactContext = values[1].trim();
			
			
			if(id2intact.containsKey(id)) {
				geneIntactContext += " " + id2intact.get(id);
				id2intact.put(id, geneIntactContext);
							
			} else {
				id2intact.put(id, geneIntactContext);
			}

			
		}
		id2IntactReader.close();
		System.out.println("Done processing id2Intact file.");
		
		
	}
	

	private void showDebug(String s) {
		if (debug) {
			System.out.println(s);
		}
	}

}
