/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.bionlpformat.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.ace.Sentence;

public class AnnotationFileMapper_Seg {

	public void mapEventFile(BufferedReader bufferedReader, JCas jcas)
		throws IOException {
		Map<String, Annotation> mappedAnnotations = new HashMap<String, Annotation>();
		mapFile(bufferedReader, jcas);
	}
		
	private void mapFile(BufferedReader bufferedReader, JCas jcas)
			throws IOException {
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			// Entities
			if (line.startsWith("T")) {
				mapEntity(line, jcas);
			}
		}
	}

	private void mapEntity(String entry, JCas jcas) {
		String[] headAndTail = entry.split("\t");
		String id = headAndTail[0];
		String tail = headAndTail[1];
		String[] tokens = tail.split(" ");
		Annotation annotation = null;
		if (tokens[0].equals("Sentence")) {
			Sentence sent = new Sentence(jcas);
			annotation = sent;
		} else if (tokens[0].equals("Token")) {
			Token tok = new Token(jcas);
			annotation = tok;
		}
		
		if (!annotation.equals(null)) {
			annotation.setId(id);
			annotation.setBegin(new Integer(tokens[1]));
			annotation.setEnd(new Integer(tokens[tokens.length-1]));
			annotation.addToIndexes();
		}
	}
}
