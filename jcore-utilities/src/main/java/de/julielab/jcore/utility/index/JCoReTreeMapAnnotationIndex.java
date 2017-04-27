package de.julielab.jcore.utility.index;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * <p>
 * Use when: You want a fuzzy search on the index keys (please see the note in
 * the next paragraph) or ou specifically need a {@link TreeMap} instead of a
 * {@link HashMap} (if not, refer to {@link JCoReHashMapAnnotationIndex}) for
 * some other reason. This only makes sense when you need to impose a specific
 * ordering on the map keys that is useful for one of the <tt>searchFuzzy</tt>
 * methods (e.g. {@link #searchFuzzy(Annotation)}). This index allows to match
 * multiple map keys with a single input key, for example by using a
 * {@link Comparators#longOverlapComparator()} for the <tt>TreeMap</tt> and
 * {@link TermGenerators#longOffsetTermGenerator()} for the keys.
 * </p>
 * <p>
 * PLEASE NOTE: In order to retrieve the correct results, the index
 * <tt>TreeSet</tt> comparator must be consistent with equals <em>with regards
 * to the index keys</em> (see the JavaDoc to {@link Comparator}). That means,
 * it does not need to be consistent with equals for the search key (in fact,
 * not being consistent with the search key is the trick) but results will be
 * undefined if the comparator is not consistent with equals with regards to the
 * keys in the index.
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

	/**
	 * <p>
	 * Returns all annotations associated with an index key that is equal to the
	 * given <tt>key</tt> by means of the comparator given to the
	 * <tt>TreeMap</tt> that is used as an index.
	 * </p>
	 * <p>
	 * If that comparator is consistent with <tt>equals</tt> (see the JavaDoc to
	 * {@link Comparator}) the index will basically return the same results how
	 * a <tt>HashMap</tt> would. However, if the comparator is in general not
	 * consistent with equals, but it is for all elements in the index, and the
	 * index keys for which holds
	 * <code>index.comparator().compare(indexkey, <tt>key</tt>) == 0</code> are
	 * located in the index without a gap between them (i.e. no indexkey that
	 * would not return 0 on comparison with <tt>key</tt>), then this method
	 * will return all annotations for the matching keys. This can be used to
	 * retrieve overlapping annotations, for example.
	 * </p>
	 * 
	 * @param key
	 *            The key to retrieve annotations for.
	 * @return All annotations associated with an index key where
	 *         <code>index.comparator().compare(indexkey, <tt>key</tt>) ==
	 *         0</code>.
	 */
	public Stream<T> searchFuzzy(K key) {
		if (index.isEmpty())
			return Stream.empty();
		TreeMap<K, Collection<T>> index = (TreeMap<K, Collection<T>>) this.index;
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

		return index.subMap(lower, firstInclusive, higher, lastInclusive).values().stream().flatMap(c -> c.stream());
	}

}
