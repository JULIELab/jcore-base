/** 
 * IOUtils.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: buyko
 * 
 * Current version: 1.0	
 * Since version:   1.0
 *
 * Creation date: 27.07.2007 
 * 
 * IO Utils and Conversion
 **/

package de.julielab.jcore.consumer.txt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

public class IOUtils {
	

	/**
	 * Generate ArrayList from File
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<String> file_to_arraylist(File file) throws IOException {
		ArrayList<String> output = new ArrayList<String>();
		BufferedReader fr = new BufferedReader(new FileReader(file));
		String line = fr.readLine();
		while (line != null) {
			output.add(line);
			line = fr.readLine();
		}
		return output;

	}
	
	/**
	 * Generate File form ArrayList
	 * @param array
	 * @param file
	 * @throws IOException
	 */
	public static void arraylist_to_file(ArrayList array, File file)
			throws IOException {
		FileWriter fw = new FileWriter(file);
		for (Iterator iter = array.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (element != null && element != "")
			fw.write(element + "\n");

		}
		fw.close();

	}
	
	
	

	/**
	 * Generate File form ArrayList
	 * @param array
	 * @param file
	 * @throws IOException
	 */
	public static void hashmapKeys_to_file(HashMap array, File file)
			throws IOException {
		FileWriter fw = new FileWriter(file);
		Set keys = array.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String word = (String) iter.next();
			fw.write (word + "\n");
		}
		
		fw.close();

	}
	
	public static HashMap file_to_hashmapKeys(File file) throws IOException {
		HashMap output = new HashMap();
		BufferedReader fr = new BufferedReader(new FileReader(file));
		String line = fr.readLine();
		while (line != null) {
			output.put(line, line);
			line = fr.readLine();
		}
		return output;

	}
	
	
	   /** Michael Scor implementation
	    * Finds the covering annotation of the specified coverFSType for the given
	    * annotation.
	    * 
	    * @param aCAS
	    *           a CAS to search in
	    * @param annot
	    *           current annotation
	    * @param coverFsType
	    *           covering annotation type to search for
	    * 
	    * @return returns the covering annotation FS or null if the covering
	    *         annotation was not found.
	    * 
	    */
	   public static AnnotationFS findCoverFS(CAS aCAS, AnnotationFS annot,
	         Type coverFsType) {

	      // covering annotation
	      AnnotationFS coverFs = null;

	      // create a searchFS of the coverFsType with the annot boundaries to
	      // search for it.
	      FeatureStructure searchFs = aCAS.createAnnotation(coverFsType, annot
	            .getBegin(), aCAS.getDocumentText().length());

	      // get the coverFSType iterator from the CAS and move it "near" to the
	      // position of the created searchFS.
	      FSIterator iterator = aCAS.getAnnotationIndex(coverFsType).iterator();
	      iterator.moveTo(searchFs);

	      // now the iterator can either point directly to the FS we are searching
	      // or it points to the next higher FS in the list. So we either have
	      // already found the correct one, of we maybe have to move the iterator to
	      // the previous position.

	      // check if the iterator at the current position is valid
	      if (iterator.isValid()) {
	         // iterator is valid, so we either have the correct annotation of we
	         // have to move to the
	         // previous one, lets check the current FS from the iterator
	         // get current FS
	         coverFs = (AnnotationFS) iterator.get();
	         // check if the coverFS covers the current match type annotation
	         if ((coverFs.getBegin() <= annot.getBegin())
	               && (coverFs.getEnd() >= annot.getEnd())) {
	            // we found the covering annotation
	            return coverFs;
	         } else {
	            // current coverFs does not cover the current match type annotation
	            // lets try to move iterator to the previous annotation and check
	            // again
	            iterator.moveToPrevious();
	            // check if the iterator is still valid after me move it to the
	            // previous FS
	            if (iterator.isValid()) {
	               // get FS
	               coverFs = (AnnotationFS) iterator.get();
	               // check the found coverFS covers the current match type
	               // annotation
	               if ((coverFs.getBegin() <= annot.getBegin())
	                     && (coverFs.getEnd() >= annot.getEnd())) {
	                  // we found the covering annotation
	            	   
	                  return coverFs;
	               }
	            }
	         }
	      } else {
	         // iterator is invalid lets try to move the iterator to the last FS and
	         // check the FS
	         iterator.moveToLast();
	         // check if the iterator is valid after we move it
	         if (iterator.isValid()) {
	            // get FS
	            coverFs = (AnnotationFS) iterator.get();
	            // check the found coverFS covers the current match type annotation
	            if ((coverFs.getBegin() <= annot.getBegin())
	                  && (coverFs.getEnd() >= annot.getEnd())) {
	               // we found the covering annotation
	            	
	               return coverFs;
	            }
	         }
	      }
	      // no covering annotation found
	      return null;
	   }
}
