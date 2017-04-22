package de.julielab.jcore.utility;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

public class JCoReAnnotationIndex<T extends Annotation, K extends Comparable<K>> {

	private final TreeSet<IndexEntry<T, K>> index;
	private final IndexTermGenerator<K> indexTermGenerator;
	private final Comparator<? super Annotation> annotationComparator;
	private IndexTermGenerator<K> searchTermGenerator;

	/**
	 * 
	 * @param indexTermGenerator
	 *            Generates index terms of generic parameter type K. Those index
	 *            terms will be extracted from indexed annotations.
	 * @param searchTermGenerator
	 *            Generates search terms of generic parameter K. The index will
	 *            extract all {@link IndexEntry} items in the index matching one
	 *            of the generated terms. This may be the very same term
	 *            generator passed for indexTermGenerator.
	 * @param annotationComparator
	 *            A comparator by which the annotations in search results will
	 *            be compared by those methods returning a TreeSet.
	 */
	public JCoReAnnotationIndex(IndexTermGenerator<K> indexTermGenerator, IndexTermGenerator<K> searchTermGenerator,
			Comparator<Annotation> annotationComparator) {
		this.indexTermGenerator = indexTermGenerator;
		this.searchTermGenerator = searchTermGenerator;
		this.annotationComparator = annotationComparator;
		this.index = new TreeSet<>();
	}

	public void index(JCas jCas, int type) {
		index(jCas, jCas.getCasType(type));
	}

	@SuppressWarnings("unchecked")
	public void index(JCas jCas, Type type) {
		FSIterator<Annotation> it = jCas.getAnnotationIndex(type).iterator();
		while (it.hasNext()) {
			Annotation annotation = (Annotation) it.next();
			index((T) annotation);
		}
	}

	public void index(T a) {
		final IndexEntry<T, K> comparisonEntry = new IndexEntry<>();
		Stream<K> indexTerms = indexTermGenerator.generateIndexTerms(a);
		indexTerms.forEach(t -> {
			comparisonEntry.term = t;
			IndexEntry<T, K> soughtEntry = index.floor(comparisonEntry);
			if (soughtEntry == null || soughtEntry.compareTo(comparisonEntry) != 0) {
				soughtEntry = new IndexEntry<T, K>(t, annotationComparator);
				index.add(soughtEntry);
			}
			soughtEntry.annotations.add(a);
		});
	}

	public TreeSet<T> search(Annotation a) {
		TreeSet<T> hits = new TreeSet<>(annotationComparator);
		Stream<K> searchTerms = searchTermGenerator.generateIndexTerms(a);
		final IndexEntry<T, K> comparisonEntry = new IndexEntry<>();
		searchTerms.forEach(t -> {
			comparisonEntry.term = t;
			IndexEntry<T, K> hit = index.floor(comparisonEntry);
			if (hit != null && hit.compareTo(comparisonEntry) == 0)
				hits.addAll(hit.annotations);
		});

		return hits;
	}

	public TreeSet<T> search(K searchTerm) {
		TreeSet<T> hits = new TreeSet<>(annotationComparator);
		final IndexEntry<T, K> comparisonEntry = new IndexEntry<>();
		comparisonEntry.term = searchTerm;
		IndexEntry<T, K> hit = index.floor(comparisonEntry);
		if (hit != null && hit.compareTo(comparisonEntry) == 0)
			hits.addAll(hit.annotations);

		return hits;
	}

	public static class IndexEntry<T extends Annotation, K extends Comparable<K>> implements Comparable<IndexEntry<T,K>> {
		public K term;
		public TreeSet<T> annotations;

		private IndexEntry() {
		};

		public IndexEntry(K term, Comparator<? super Annotation> annotationComparator) {
			this.term = term;
			annotations = new TreeSet<T>(annotationComparator);
		}

		@Override
		public int compareTo(IndexEntry<T,K> o) {
			return term.compareTo(o.term);
		}
	}

	@FunctionalInterface
	public interface IndexTermGenerator<K extends Comparable<K>> {
		public Stream<K> generateIndexTerms(Annotation a);
	}
}
