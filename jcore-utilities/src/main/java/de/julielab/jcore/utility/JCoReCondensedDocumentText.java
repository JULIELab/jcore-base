package de.julielab.jcore.utility;

import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.uima.jcas.JCas;

/**
 * This class is helpful when some parts of the CAS document text should be cut
 * out according to a set of specific annotations. The class then represents the
 * document text that results from cutting out said text passages. It offers a
 * method to return the actual text string and a method to map the character
 * offsets of the compacted string to the original CAS document text.
 * 
 * @author faessler
 *
 */
public class JCoReCondensedDocumentText {
	private NavigableMap<Integer, Integer> spanSumMap;
	private String condensedText;
	private JCas cas;

	/**
	 * <p>
	 * Cuts away the covered text of annotations of a type in <tt>cutAwayTypes</tt>
	 * from the <tt>cas</tt> document text. If <tt>cutAwayTypes</tt> is null or
	 * empty, this class' methods will return the original CAS data.
	 * </p>
	 * 
	 * @param cas
	 *            The CAS for which the document text should be cut.
	 * @param cutAwayTypes
	 *            The types for cutting. May be null.
	 * @throws ClassNotFoundException
	 *             If <tt>cutAwayTypes</tt> contains non-existing type names.
	 */
	public JCoReCondensedDocumentText(JCas cas, Set<String> cutAwayTypes) throws ClassNotFoundException {
		this.cas = cas;
		buildMap(cas, cutAwayTypes);
	}

	/**
	 * <p>
	 * Creates a map that maps those positions of the small-cut text that correspond
	 * to an intermediate next position after a cut-away annotation in the original
	 * text to the sum of ranges covered by cut-away annotations up to the original
	 * offset.
	 * </p>
	 * <p>
	 * If <tt>cutAwayTypes</tt> is empty, no work will be done and the methods of
	 * this class we return the original text and offets of the CAS.
	 * </p>
	 * 
	 * @param cas
	 *            The CAS for create a cut-away document text for.
	 * @param cutAwayTypes
	 *            The qualified type names of the annotations whose covered text
	 *            should be cut away.
	 * @throws ClassNotFoundException
	 *             If <tt>cutAwayTypes</tt> contains type identifiers to
	 *             non-existing types.
	 */
	public void buildMap(JCas cas, Set<String> cutAwayTypes) throws ClassNotFoundException {
		if (cutAwayTypes == null || cutAwayTypes.isEmpty())
			return;
		StringBuilder sb = new StringBuilder();
		spanSumMap = new TreeMap<>();
		spanSumMap.put(0, 0);
		JCoReAnnotationIndexMerger merger = new JCoReAnnotationIndexMerger(cutAwayTypes, true, null, cas);
		int spanSum = 0;
		int lastBegin = 0;
		int lastEnd = -1;
		// For each ignored annotation, there could be following annotations overlapping
		// with the first, effectively enlargeing the ignored span. Thus, we iterate
		// until we find an ignored annotation the has a positive (not 0) distance to a
		// previous one. Then, we store the length of the span of ignored annotations
		// for the largest end of the previous annotations.
		while (merger.incrementAnnotation()) {
			int end = merger.getCurrentEnd();
			int begin = merger.getCurrentBegin();

			if (lastEnd > 0 && begin > lastEnd) {
				spanSum += lastEnd - lastBegin;
				spanSumMap.put(lastEnd - spanSum + 1, spanSum);
				lastBegin = begin;
				sb.append(cas.getDocumentText().substring(lastEnd, begin));
			} else if (lastEnd < 0) {
				lastBegin = begin;
				sb.append(cas.getDocumentText().substring(0, begin));
			}
			lastEnd = end;
		}
		// Since we iterate one annotation further than the annotation we store the span
		// for, we need to take care of the very last ignored annotation after the loop
		// - it has never been handled itself.
		if (lastEnd > 0) {
			spanSum += lastEnd - lastBegin;
			spanSumMap.put(lastEnd - spanSum + 1, spanSum);
		}
		if (lastEnd < cas.getDocumentText().length())
			sb.append(cas.getDocumentText().substring(lastEnd, cas.getDocumentText().length()));
		condensedText = sb.toString();
	}

	/**
	 * Given a character offset relative to the condensed document text, this method
	 * returns the corresponding offset in the original CAS document text.
	 * 
	 * @param condensedOffset
	 *            The character offset in the condensed document text string.
	 * @return The character offset relative to the original CAS document text
	 *         associated with <tt>condensedOffset</tt>.
	 */
	public int getOriginalOffsetForCondensedOffset(int condensedOffset) {
		if (spanSumMap == null)
			return condensedOffset;
		Entry<Integer, Integer> floorEntry = spanSumMap.floorEntry(condensedOffset);
		return condensedOffset + floorEntry.getValue();
	}

	public String getCodensedText() {
		return condensedText != null ? condensedText : cas.getDocumentText();
	}
}
