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

import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * <p>
 * Use when: You just want an ordered set of annotations that can be retrieved
 * according to an {@link Comparator}. If you want the search keys to be of
 * another type than the annotations in the index or even keys not being
 * annotations at all, look at {@link JCoReMapAnnotationIndex} and its
 * subclasses.
 * </p>
 * This class is actually more an ordered set than an index. It is backed by a
 * {@link TreeSet}
 * 
 * @author faessler
 *
 * @param <E>
 *            The annotation type to index.
 */
public class JCoReSetAnnotationIndex<E extends Annotation> implements JCoReAnnotationIndex<E> {
	private TreeSet<E> index;

	public JCoReSetAnnotationIndex(Comparator<? super E> comparator, JCas jCas, Type type) {
		index = new TreeSet<>(comparator);
		if (jCas != null && type != null)
			index(jCas, type);
	}

	public JCoReSetAnnotationIndex(Comparator<? super E> comparator, JCas jCas, int type) {
		this(comparator, jCas, jCas.getCasType(type));
	}

	public JCoReSetAnnotationIndex(Comparator<? super E> comparator) {
		this(comparator, null, null);
	}

	/**
	 * Indexes the whole contents of the CAS annotation index of type
	 * <tt>type</tt>. For each annotation, the {@link #indexTermGenerator} is
	 * used to create terms with which the annotation will be associated in the
	 * index and can be retrieved by a <code>search</code> method.
	 * 
	 * @param jCas
	 *            A CAS instance.
	 * @param type
	 *            The annotation type to index.
	 */
	public void index(JCas jCas, int type) {
		index(jCas, jCas.getCasType(type));
	}

	/**
	 * Indexes the whole contents of the CAS annotation index of type
	 * <tt>type</tt>. For each annotation, the {@link #indexTermGenerator} is
	 * used to create terms with which the annotation will be associated in the
	 * index and can be retrieved by a <code>search</code> method.
	 * 
	 * @param jCas
	 *            A CAS instance.
	 * @param type
	 *            The annotation type to index.
	 */
	@SuppressWarnings("unchecked")
	public void index(JCas jCas, Type type) {
		FSIterator<Annotation> it = jCas.getAnnotationIndex(type).iterator();
		while (it.hasNext()) {
			Annotation annotation = (Annotation) it.next();
			index((E) annotation);
		}
	}

	/**
	 * Indexes the given annotation.
	 * 
	 * @param annotation
	 *            The annotation to add to the index.
	 */
	public void index(E annotation) {
		index.add(annotation);
	}

	/**
	 * The same as {@link #index(Annotation)}.
	 * 
	 * @param annotation
	 *            The annotation to add to the index.
	 */
	public void add(E annotation) {
		index(annotation);
	}

	public Stream<E> search(E a) {
		return searchSubset(a).stream();
	}

	public NavigableSet<E> searchSubset(E a) {
		if (index.isEmpty())
			return Collections.emptyNavigableSet();
		boolean firstInclusive = false;
		boolean lastInclusive = false;
		E lower = index.lower(a);
		if (lower == null) {
			lower = index.first();
			firstInclusive = true;
		}
		E higher = index.higher(a);
		if (higher == null) {
			higher = index.last();
			lastInclusive = true;
		}

		return index.subSet(lower, firstInclusive, higher, lastInclusive);
	}

	public E get(E a) {
		E result = null;
		try {
			result = index.floor(a);
			return result;
		} finally {
			E ceiling = index.ceiling(a);
			if (ceiling != null && index.comparator().compare(result, ceiling) != 0)
				throw new IllegalStateException(
						"There are multiple index items matching " + a + ". Use the search(E) method.");
		}
	}

	public boolean contains(Annotation a) {
		return index.contains(a);
	}

	public Stream<E> items() {
		return index.stream();
	}

	public TreeSet<E> getIndex() {
		return index;
	}

}
