/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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