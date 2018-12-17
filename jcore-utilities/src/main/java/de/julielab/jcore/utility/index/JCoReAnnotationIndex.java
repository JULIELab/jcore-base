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

/**
 * A common interface for JCoRe annotation indexes. This interface is not so
 * much intended to be actually used in code (though it could be), but should
 * rather allow to easily access all available JCoRe annotation indexed in an
 * IDE for an overview and easy navigation. Please refer to the JavaDoc of
 * implementing classes to decide when to use which index.
 * 
 * @author faessler
 *
 * @param <T>
 *            The annotation type being indexed for efficient retrieval.
 */
public interface JCoReAnnotationIndex<T extends Annotation> {
	void index(T a);

	void index(JCas jCas, int type);

	void index(JCas jcas, Type type);

	void add(T a);
}
