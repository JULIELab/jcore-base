/** 
 * SynHitUtils.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.7 	
 * Since version:   1.7
 *
 * Creation date: Mar 10, 2008 
 * 
 * some helper functions for displaying the found hits
 **/

package de.julielab.jules.ae.genemapper.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.julielab.jules.ae.genemapper.SynHit;

public class SynHitUtils {

	
	/**
	 * shows all hits in an arraylist of SynHits objects
	 * 
	 * @param hitList
	 */
	public static void showHits(HashMap<String, SynHit> hitList) {
		DecimalFormat scoreFormat = new DecimalFormat("0.000");
		System.out.println("\n\n\nfinal hit list:");
		for (Iterator<String> iter = hitList.keySet().iterator(); iter.hasNext();) {
			String key = iter.next();
			SynHit hit = hitList.get(key);
			System.out.println(key + " --> \t"
					+ scoreFormat.format(hit.getMentionScore()) + "\t"
					+ hit.getSynonym());
		}
	}

	public static void showHits(ArrayList<SynHit> hitList) {
		DecimalFormat scoreFormat = new DecimalFormat("0.000");
		System.out.println("\n\n\nfinal hit list:");
		for (SynHit hit : hitList) {
			String id = hit.getId();
			System.out.println(id + " --> \t"
					+ scoreFormat.format(hit.getMentionScore()) + "\t"
					+ hit.getSynonym());
		}
	}

	public static String showHitIDs(ArrayList<SynHit> filteredHits) {
		StringBuffer show = new StringBuffer();
		for (SynHit hit : filteredHits) {
			show.append(hit.getId() + " ");
		}
		return show.toString();
	}

	public static String showHitIDs(HashMap<String, SynHit> filteredHits) {
		StringBuffer show = new StringBuffer();
		for (Iterator<String> iter = filteredHits.keySet().iterator(); iter.hasNext();) {
			String id = iter.next();
			show.append(id + " ");
		}
		return show.toString();
	}

}
