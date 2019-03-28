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
package de.julielab.jcore.utility.index;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.HashMap;

/**
 * <p>
 * Use when: You basically just need a map that maps from some key to
 * annotations associated with that key.
 * </p>
 * A trivial subclass of {@link JCoReMapAnnotationIndex} that uses a HashMap
 * index.
 * 
 * @author faessler
 *
 * @param <T>
 *            The annotation type the index is over.
 * @param <K>
 *            The key type used to index the annotations.
 */
public class JCoReHashMapAnnotationIndex<K extends Comparable<K>, T extends Annotation>
		extends JCoReMapAnnotationIndex<K, T> {

	public JCoReHashMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator) {
		super(HashMap::new, indexTermGenerator, searchTermGenerator);
	}

	public JCoReHashMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator, JCas jCas, Type type) {
		super(HashMap::new, indexTermGenerator, searchTermGenerator, jCas, type);
	}

	public JCoReHashMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator, JCas jCas, int type) {
		super(HashMap::new, indexTermGenerator, searchTermGenerator, jCas, type);
	}

}
