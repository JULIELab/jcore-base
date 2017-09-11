/**
 * AcronymEntry.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tusche
 *
 * Current version: 1.3.3
 *
 * Creation date: 14.01.2007
 *
 * a part of an Acronym: an Acronym contains shortform + AcronymEntry
 * an AcronymEntry contains all fullforms to the shortform
 * plus information about them (information is saved in a FullformEntry)
 * 
 * 
 **/

package de.julielab.jcore.ae.acronymtagger.entries;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;


/**
 * a part of an Acronym: an Acronym contains shortform + AcronymEntry
 * an AcronymEntry contains all fullforms to the shortform
 * plus information about them (information is saved in a FullformEntry)
 * 
 * @author tusche
 * 
 */
public class AcronymEntry implements Serializable {

	static final long serialVersionUID = 2L;

	private TreeMap<String, FullformEntry> fullformList = new TreeMap<String, FullformEntry>();


	// ----------CONSTRUCTORS---------------------------------

	/**
	 * adds only a fullform (no further information) to the internal
	 * fullformList
	 * 
	 * @param full
	 *            the (normalized) fullform
	 */
	protected AcronymEntry(String full) {
		FullformEntry x = new FullformEntry();
		this.addFullform(full, x);
	}

	/**
	 * adds a fullform with all information to the internal fullformList
	 * 
	 * @param full
	 *            the (normalized) fullform
	 * @param year
	 *            the year of the document
	 * @param mesh
	 *            all MeshTerms of the document
	 * @param unnormalizedFulls
	 *            all unnormalized fullforms
	 */
	protected AcronymEntry(String full, int year, Set<String> mesh,
			Set<String> unnormalizedFulls) {

		FullformEntry x = new FullformEntry(year, mesh, unnormalizedFulls);
		this.addFullform(full, x);
	}

	/**
	 * adds a fullform with all information in the FullformEntry to the internal
	 * fullformList
	 * 
	 * @param fullName
	 *            the (normalized) fullform
	 * @param ffE
	 *            the FullformEntry containg document information
	 */
	protected AcronymEntry(String fullName, FullformEntry ffE) {
		this.addFullform(fullName, ffE);
	}

	// --------GIVE & GET-----------------------------------

	public boolean hasfullform(String full) {
		return fullformList.containsKey(full);
	}

	public Set<Entry<String, FullformEntry>> getAllFullforms() {
		return fullformList.entrySet();
	}

	public void addFullform(String fullName, FullformEntry f) {
		if (!fullformList.containsKey(fullName)) {
			fullformList.put(fullName, f);
		} else {
			FullformEntry old = fullformList.get(fullName);
			old.meshTerms.addAll(f.meshTerms);
			old.unnormalizedForms.addAll(f.unnormalizedForms);

			FullformEntry newForm = new FullformEntry(Math.min(old.year, f.year),
					old.count + f.count, old.meshTerms, old.unnormalizedForms);
			fullformList.remove(fullName);
			fullformList.put(fullName, newForm);
		}
	}
	
	/**
	 * This method treads the second parameter as a variation of
	 * of the FullformEntry connected to the first parameter.
	 * In general, the information included in the ffEntryToInclude
	 * is merged with the FullformEntry connected to ffToKeep.
	 * That means that year, count and meshtermlist of ffToKeep
	 * are merged with those of ffEntryToInclude in the currently defined way
	 * (see AcronymEntry.addFullform() for more information)
	 * while the name of ffEntryToInclude is added to ffToKeep
	 * as a spelling variation.
	 * Warning1: the FullformEntry connected to ffEntryToInclude
	 * is deleted from this AcronymEntry afterwards
	 * Warning2: a NullPointerException is thrown, if at least one
	 * of the two parameters are not found in this AcronymEntry.
	 * @param ffToKeep the name of the FullformEntry that is supposed
	 * to be the representative
	 * @param ffEntryToInclude the name of the FullformEntry that contains
	 * the information which shall be added to the represantative. Will be deleted
	 * afterwards
	 */
	public void merge(String ffToKeep, String ffEntryToInclude) throws NullPointerException {
		
		FullformEntry representative = new FullformEntry();
		FullformEntry additional = new FullformEntry();

		try {
			representative = fullformList.get(ffToKeep);
			additional 	= fullformList.get(ffEntryToInclude);
			
			if (representative == null || additional == null) {
				throw new Exception();
			}
			
			
			representative.meshTerms.addAll(additional.meshTerms);
			representative.unnormalizedForms.addAll(additional.unnormalizedForms);
			representative.unnormalizedForms.add(ffEntryToInclude);
			
			Set<String> newUnnormFull= new HashSet<String>(representative.unnormalizedForms);

			FullformEntry newForm = new FullformEntry(Math.min(representative.year, additional.year),
					representative.count + additional.count, representative.meshTerms, representative.unnormalizedForms);

			fullformList.remove(ffToKeep);
			fullformList.remove(ffEntryToInclude);

			fullformList.put(ffToKeep, newForm);

		}
		catch (Exception e) {
			throw new NullPointerException("This AcronymEntry does not contain at least one of the specified fullforms");
		}
	}
	
	
	/**
	 * removes the fullform entry defined by the String
	 * @param fullform
	 */
	public void remove(String fullform) {
		fullformList.remove(fullform);
	}

	/**
	 * returns a formatted String containing all information of the AcronymEntry
	 * format is Short Full_1 Year_1 MeshList_1 # Unnormalized Fullforms_1 Short
	 * Full_2 Year_2 MeshList_2 # Unnormalized Fullforms_2 ...
	 * 
	 * @param shortform the current short form
	 * @return formatted String
	 */
	public String getString(String shortform) {
		FullformEntry f;
		String fName;
		StringBuffer s = new StringBuffer("");

		for (Map.Entry<String, FullformEntry> entry : fullformList.entrySet()) {
			f = entry.getValue();
			fName = entry.getKey();
			s.append(shortform + "\t" + fName + f.getString());
		}
		return s.toString();
	}
	
	/**
	 * returns a html representation of this entry for
	 * including it in a table.
	 * @param shortform the current short form
	 * @paream includeMeshs decide whether the table contains
	 * a column listing all meshterms to the short-full-pair
	 * @return a string containing a formatted html table row
	 */
	public String getHTMLString(String shortform, boolean includeMeshs) {
	
		Set<Map.Entry<String, FullformEntry>> flEntries = fullformList.entrySet();
		int entryNumber = flEntries.size();
		Iterator<Map.Entry<String, FullformEntry>> fullformIter = flEntries.iterator();
		Map.Entry<String, FullformEntry> entry;

		FullformEntry f;
		String fName;
		StringBuffer s = new StringBuffer("\t<tr><th rowspan=\"" + entryNumber + "\">" +shortform + "</th>\n");
		
		entry = fullformIter.next();
		f = entry.getValue();
		fName = entry.getKey();
		
		s.append("<td>" + fName + "</td>" + f.getHTMLString(includeMeshs) + "</tr>\n");
		
		/*
		  <tr>
		    <th rowspan="2">Die Eselheit besteht aus</th>
		    <td>echten Eseln</td>
		  </tr>
		  <tr>
		    <td>verkappten Eseln (Menschen)</td>
		  </tr>
		 */
		  

		while (fullformIter.hasNext()) {
			entry = fullformIter.next();
			f = entry.getValue();
			fName = entry.getKey();
			s.append("<tr><td>" + fName + "</td>" + f.getHTMLString(includeMeshs) + "</tr>\n");
		}
		return s.toString();
	}
	
	public int size() {
		return this.fullformList.size();
	}

}
