package de.julielab.jcore.utility.index;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
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
public class JCoReTreeMapAnnotationIndex<K extends Comparable<K>, T extends Annotation>
		extends JCoReMapAnnotationIndex<K, T> {

	public JCoReTreeMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator) {
		super(TreeMap::new, indexTermGenerator, searchTermGenerator);
	}

	public JCoReTreeMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator, JCas jCas, Type type) {
		super(TreeMap::new, indexTermGenerator, searchTermGenerator, jCas, type);
	}

	public JCoReTreeMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator, JCas jCas, int type) {
		super(TreeMap::new, indexTermGenerator, searchTermGenerator, jCas, type);
	}

	public JCoReTreeMapAnnotationIndex(Comparator<K> comparator, IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator) {
		super(() -> new TreeMap<>(comparator), indexTermGenerator, searchTermGenerator);
	}

	public JCoReTreeMapAnnotationIndex(Comparator<K> comparator, IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator, JCas jCas, Type type) {
		super(() -> new TreeMap<>(comparator), indexTermGenerator, searchTermGenerator, jCas, type);
	}

	public JCoReTreeMapAnnotationIndex(Comparator<K> comparator, IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator, JCas jCas, int type) {
		super(() -> new TreeMap<>(comparator), indexTermGenerator, searchTermGenerator, jCas, type);
	}

	@SuppressWarnings("unchecked")
	public Stream<T> searchFuzzy(Annotation a) {
		Object o = searchTermGenerator.generateIndexTerms(a);
		if (o instanceof Stream) {
			Stream<K> searchTerms = (Stream<K>) o;
			try {
				return searchFuzzy(searchTerms);
			} finally {
				searchTerms.close();
			}
		} else {
			return searchFuzzy((K) o);
		}
	}

	public Stream<T> searchFuzzy(Stream<K> searchTerms) {
		Stream<T> result = null;
		for (Iterator<K> it = searchTerms.iterator(); it.hasNext();) {
			K t = it.next();
			if (result == null)
				result = searchFuzzy(t);
			else
				Stream.concat(result, searchFuzzy(t));

		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public Stream<T> searchFuzzy(K key) {
		if (index.isEmpty())
			return Stream.empty();
		TreeMap<K, T> index = (TreeMap<K, T>) this.index;
		boolean firstInclusive = false;
		boolean lastInclusive = false;
		K lower = index.lowerKey(key);
		if (lower == null) {
			lower = index.firstKey();
			firstInclusive = true;
		}
		K higher = index.higherKey(key);
		if (higher == null) {
			higher = index.lastKey();
			lastInclusive = true;
		}

		return index.subMap(lower, firstInclusive, higher, lastInclusive).values().stream();
	}

}
