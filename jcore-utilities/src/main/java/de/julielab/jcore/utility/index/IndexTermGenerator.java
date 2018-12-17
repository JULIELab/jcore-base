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

import java.util.stream.Stream;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * <p>
 * An interface that defines a way to construct index or search terms from an
 * annotation <tt>a</tt>. Implementations are given to the constructor of
 * {@link JCoReMapAnnotationIndex} and are then internally used to create index
 * terms from annotations.
 * </p>
 * <p>
 * The class {@link TermGenerators} offers a range of predefined term
 * generators.
 * </p>
 * 
 * @author faessler
 *
 * @param <K>
 *            The type of index terms that should be created for an annotation,
 *            for example a String or an Integer. This type is also used by the
 *            index map for the keys, where the generated index terms will go
 *            to.
 */
@FunctionalInterface
public interface IndexTermGenerator<K extends Comparable<K>> {
	/**
	 * Generates a single index term or a stream of index terms for the
	 * annotation <tt>a</tt>. If it is known in advance if single terms or a
	 * stream is returned, the methods {@link #asKey(Annotation)} or
	 * {@link #asStream(Annotation)} might be used instead.
	 * 
	 * @param a
	 *            The annotation to generate terms for.
	 * @return The generated terms. Might be a single term of type K or a stream
	 *         of terms.
	 */
	public Object generateIndexTerms(Annotation a);

	/**
	 * Use this method when you know that this term generator returns a stream.
	 * 
	 * @return The search terms as a stream.
	 */
	@SuppressWarnings("unchecked")
	default Stream<K> asStream(Annotation a) {
		Object o = generateIndexTerms(a);
		if (o instanceof Stream)
			return (Stream<K>) o;
		return (Stream<K>) Stream.of((K)o);
	}

	/**
	 * Use this method when you know that this term generator returns single key
	 * values.
	 * 
	 * @return The search term.
	 */
	@SuppressWarnings("unchecked")
	default K asKey(Annotation a) {
		return (K) generateIndexTerms(a);
	}
}