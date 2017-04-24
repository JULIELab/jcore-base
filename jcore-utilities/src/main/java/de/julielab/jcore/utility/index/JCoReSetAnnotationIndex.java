package de.julielab.jcore.utility.index;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * This class is actually more an ordered set than an index. It is backed by a {@link TreeSet}
 * @author faessler
 *
 * @param <E>
 */
public class JCoReSetAnnotationIndex<E extends Annotation> {
	private TreeSet<E> index;

	public JCoReSetAnnotationIndex(Comparator<? super E> comparator) {
		index = new TreeSet<>(comparator);
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
	
	private void index(E annotation) {
		index.add(annotation);
	}


	public NavigableSet<E> search(E a) {
		E lower = index.lower(a);
		if (lower == null)
			lower = index.floor(a);
		// check if we found some result at all
		if (index.comparator().compare(lower, a) != 0)
			return null;
		E higher = index.higher(a);
		return index.subSet(lower, false, higher, false);
	}

}
