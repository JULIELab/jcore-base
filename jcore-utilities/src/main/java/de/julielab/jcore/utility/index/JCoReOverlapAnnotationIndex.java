package de.julielab.jcore.utility.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.utility.JCoReTools;

/**
 * This index allows to index annotations and then retrieve all those
 * annotations overlapping in any way with another annotation or an arbitrary
 * given pair of begin and end offsets.
 * 
 * @author faessler
 *
 * @param <E>
 *            The annotation type the index should be over.
 */
public class JCoReOverlapAnnotationIndex<E extends Annotation> {
	private List<E> beginIndex;
	private List<E> endIndex;
	private boolean frozen;

	public JCoReOverlapAnnotationIndex() {
		beginIndex = new ArrayList<>();
		endIndex = new ArrayList<>();
	}

	public JCoReOverlapAnnotationIndex(JCas jcas, int type) {
		this();
		index(jcas, type);
		freeze();
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
		if (frozen)
			throw new IllegalStateException("This index is frozen and cannot except further items.");
		beginIndex.add(annotation);
		endIndex.add(annotation);
	}

	public void freeze() {
		frozen = true;
		Collections.sort(beginIndex, Comparators.beginOffsetComparator());
		Collections.sort(endIndex, Comparators.endOffsetComparator());
	}

	/**
	 * Returns all annotation in the index overlapping in any way with a
	 * (embedded, covering, partial overlappings). The resulting list is either
	 * sorted by begin or end offset. It is not easily predictable which case it
	 * is (could be added as a return value if that would be useful in any way).
	 * 
	 * @param a
	 *            The annotation to retrieve overlapping annotations from the
	 *            index for.
	 * @return All annotations in the index overlapping a.
	 */
	public <T extends Annotation> List<E> search(T a) {
		if (!frozen)
			throw new IllegalStateException(
					"This index is not frozen and cannot be used yet. Freeze the index before searching.");
		// The following is rather difficult to understand from the code. The
		// idea is the following:
		// We search annotations overlapping with a. Thus, we can rule out those
		// annotations that end before a or start after a.
		// In the next 4 lines, we determine how many annotations can be ruled
		// out because they start after a and how many end before a.
		int begin = a.getBegin();
		int end = a.getEnd();
		int indexBeginAfterEnd = insertionPoint(JCoReTools.binarySearch(beginIndex, an -> an.getBegin(), end));
		int indexEndBeforeBegin = insertionPoint(JCoReTools.binarySearch(endIndex, an -> an.getEnd(), begin));

		// Depending on which case rules out more annotations - ending before a
		// or starting after a - we look at the case that leaves us with the
		// fewest annotations. If those were the annotations that started after
		// a, then we keep those that start before a ends. Those are then sorted
		// by end offset. And then we rule those out that end before a starts.
		// This is our result set. The other case is vice versa.
		if (indexBeginAfterEnd < endIndex.size() - indexEndBeforeBegin) {
			List<E> beginBeforeEnd = new ArrayList<>(beginIndex.subList(0, indexBeginAfterEnd));
			Collections.sort(beginBeforeEnd, Comparators.endOffsetComparator());
			int indexEndAfterBegin = insertionPoint(JCoReTools.binarySearch(beginBeforeEnd, an -> an.getEnd(), begin));
			return beginBeforeEnd.subList(indexEndAfterBegin, beginBeforeEnd.size());
		} else {
			List<E> endAfterBegin = new ArrayList<>(endIndex.subList(indexEndBeforeBegin, endIndex.size()));
			Collections.sort(endAfterBegin, Comparators.beginOffsetComparator());
			int indexBeginBeforeEnd = insertionPoint(JCoReTools.binarySearch(endAfterBegin, an -> an.getBegin(), end));
			return endAfterBegin.subList(0, indexBeginBeforeEnd);
		}
	}

	private int insertionPoint(int i) {
		return i < 0 ? -(i + 1) : i;
	}

	public void melt() {
		frozen = false;
	}
}
