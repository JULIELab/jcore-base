/** 
 * MeshtermOrganismFinder.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.7	
 * Since version:   1.5
 *
 * Creation date: Nov 19, 2007 
 * 
 * A helper class that provides information 
 * on the organisms contained in a document based on the MeSH terms.
 **/

package de.julielab.jules.ae.genemapper.disambig.org;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.IndexSearcher;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.MeshHeading;
import de.julielab.jcore.types.MeshMention;
import de.julielab.jcore.types.Organism;
import de.julielab.jcore.types.ResourceEntry;

public class MeshtermOrganismFinder {

	public final static String COMBINER = "_";
	private HashMap<Pattern, String> meshOrganismMapping;
	private static final Logger LOGGER = LoggerFactory.getLogger(MeshtermOrganismFinder.class);
	private TreeSet<String> organisms;
	private IndexSearcher organismIndexSearcher;

	public MeshtermOrganismFinder(String mappingDefFile) throws IOException {
		LOGGER.debug("initialization: loading organism mapping from {}...", mappingDefFile);
		initOrganismMapping(mappingDefFile);
	}

	/**
	 * reads the mesh terms from the cas, check in the mapping file.
	 * 
	 * @param jcas
	 *            the document containing MeSH terms
	 * @return a list with all organisms found
	 */
	public TreeSet<String> getOrganismsForDocument(JCas jcas) {
		TreeSet<String> organismList = new TreeSet<String>();
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator meshIterator = indexes.getAnnotationIndex(MeshHeading.type).iterator();
		while (meshIterator.hasNext()) {
			MeshHeading mesh = (MeshHeading) meshIterator.next();
			String term = mesh.getDescriptorName();
			String[] organisms = findOrganism(term);
			if (organisms != null) {
				for (String organism : organisms) {
					organismList.add(organism);
					LOGGER.debug("getOrganismsFromCAS() - mesh descriptor >" + term + "< found in mapping. Organism: "
							+ organism);
				}
			} else {
				LOGGER.debug("getOrganismsFromCAS() - mesh descriptor >" + term + "< NOT found in mapping");
			}
		}

		Iterator organismIterator = indexes.getAnnotationIndex(Organism.type).iterator();
		while (organismIterator.hasNext()) {
			Organism org = (Organism) organismIterator.next();
			String term = org.getSpecificType();
			// For legacy reasons we continue to perform the lookup by name for the moment.
			// But we will use the ResourceEntries as well.
			String[] organisms = findOrganism(term);
			if (organisms != null) {
				for (String organism : organisms) {
					organismList.add(organism);
					LOGGER.debug("getOrganismsFromCAS() - Organism specificType >" + term
							+ "< found in mapping. Organism: " + organism);
				}
			} else {
				LOGGER.debug("getOrganismsFromCAS() - Organism specificType >" + term + "< NOT found in mapping");
			}
			// EF CHANGE
			// Now get the annotations from the actual organism tagger.
			FSArray resourceEntryList = org.getResourceEntryList();
			if (null != resourceEntryList) {
				for (int i = 0; i < resourceEntryList.size(); i++) {
					ResourceEntry resourceEntry = (ResourceEntry) resourceEntryList.get(i);
					if (null == resourceEntry)
						continue;
					String source = resourceEntry.getSource();
					// We only work with the NCBI Taxonomy.
					if (!source.equals("NCBI Taxonomy"))
						continue;
					String taxId = resourceEntry.getEntryId();
					organismList.add(taxId);
				}
			}
		}

		// TODO Evaluate: Is this even necessary?
		Iterator meshMentionIterator = indexes.getAnnotationIndex(MeshMention.type).iterator();
		while (meshMentionIterator.hasNext()) {
			MeshMention org = (MeshMention) meshMentionIterator.next();
			String term = org.getSpecificType();
			String[] organisms = findOrganism(term);
			if (organisms != null) {
				for (String organism : organisms) {
					organismList.add(organism);
					LOGGER.debug("getOrganismsFromCAS() - Organism specificType >" + term
							+ "< found in mapping. Organism: " + organism);
				}
			} else {
				LOGGER.debug("getOrganismsFromCAS() - Organism specificType >" + term + "< NOT found in mapping");
			}
		}
		
		LOGGER.debug("Found the following organisms:");
		if (organismList.size() > 0) {
			for (String taxId : organismList)
				LOGGER.debug("TaxId: {}", taxId);
		} else {
			LOGGER.debug("No organisms were found in the UIMA repository or in MeSH terms.");
		}

		indexes = null;
		return organismList;
	}

	/**
	 * get organism from mesh2uniprot mapping definition
	 * 
	 * @param term
	 *            the mesh descriptor name to search in this mapping definition
	 * @return null if nothing found
	 */
	private String[] findOrganism(String term) {
		if (!StringUtils.isBlank(term)) {
			// ArrayList<String> organism = new ArrayList<String>();
			String[] organisms = null;
			// check against mapping file
			for (Iterator iter = meshOrganismMapping.keySet().iterator(); iter.hasNext();) {
				Pattern pat = (Pattern) iter.next();
				if (pat.matcher(term).matches()) {
					organisms = meshOrganismMapping.get(pat).split("\\|");
					return organisms;
				}
			}
		}
		return null;
	}

	/**
	 * reads the mapping definition the first column of each line is interpreted as regular expression
	 */
	private void initOrganismMapping(String mappingDefFile) throws IOException {
		meshOrganismMapping = new HashMap<Pattern, String>();
		if (null == mappingDefFile) {
			LOGGER.warn("Mapping definition file is null, MeSH organisms are not used.");
			return;
		}
			
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader((mappingDefFile)));
			String line = "";
			while ((line = br.readLine()) != null) {
				String[] values = line.trim().split("\t");
				if (values.length != 2) {
					LOGGER.error("initOrganismMapping() - organism mapping file malformed!");
					throw new IOException();
				}
				String mesh = values[0];
				String org = values[1];
				Pattern pat = Pattern.compile(mesh);
				meshOrganismMapping.put(pat, org);
			}
			br.close();
		} catch (IOException e) {
			LOGGER.error("initOrganismMapping() - error reading mesh2uniptor mapping file");
			throw e;
		} finally {
			try {if (br != null) {br.close();}} catch (IOException e) {};
		}
	}

	/**
	 * @return the meshOrganismMapping
	 */
	public HashMap<Pattern, String> getMeshOrganismMapping() {
		return meshOrganismMapping;
	}
}
