package de.julielab.jcore.utility;

import org.apache.uima.jcas.JCas;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

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
	private NavigableMap<Integer, Integer> condensedPos2SumCutMap;
	private NavigableMap<Integer, Integer> originalPos2SumCutMap;
	private String condensedText;
	private JCas cas;
	private Set<Character> cutAwayFillCharacters;

	public JCas getCas() {
		return cas;
	}
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
		this(cas, cutAwayTypes, null);
	}

	/**
	 * <p>
	 * Cuts away the covered text of annotations of a type in <tt>cutAwayTypes</tt>
	 * from the <tt>cas</tt> document text. If <tt>cutAwayTypes</tt> is null or
	 * empty, this class' methods will return the original CAS data.
	 * </p>
	 * <p>The <tt>cutAwayFillCharacters</tt> set may provide characters that, when being the only character between
	 * to cut-away annotations, will add to the span of text being cut away. This way, enumerations of references
	 * (e.g. "4,6,8") can be completely removed, for example.</p>
	 * 
	 * @param cas
	 *            The CAS for which the document text should be cut.
	 * @param cutAwayTypes
	 *            The types for cutting. May be null.
	 * @param cutAwayFillCharacters Characters that, when being the only separator between two cut away annotations, are also cut away.
	 * @throws ClassNotFoundException
	 *             If <tt>cutAwayTypes</tt> contains non-existing type names.
	 */
	public JCoReCondensedDocumentText(JCas cas, Set<String> cutAwayTypes, Set<Character> cutAwayFillCharacters) throws ClassNotFoundException {
		this.cas = cas;
		this.cutAwayFillCharacters = cutAwayFillCharacters;
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
		condensedPos2SumCutMap = new TreeMap<>();
		condensedPos2SumCutMap.put(0, 0);
		originalPos2SumCutMap = new TreeMap<>();
		originalPos2SumCutMap.put(0, 0);
		JCoReAnnotationIndexMerger merger = new JCoReAnnotationIndexMerger(cutAwayTypes, true, null, cas);
		int cutSum = 0;
		int lastBegin = 0;
		int lastEnd = -1;
		// For each ignored annotation, there could be following annotations overlapping
		// with the first, effectively enlarging the ignored span. Thus, we iterate
		// until we find an ignored annotation that has a positive (not 0) distance to a
		// previous one. Then, we store the length of the span of cut-away annotations
		// for the largest end of the previous annotations.
		while (merger.incrementAnnotation()) {
			int end = merger.getCurrentEnd();
			int begin = merger.getCurrentBegin();

			boolean moreThanOneCharacterDistance = begin - lastEnd > 2;
			boolean previousCharacterIsCutAwayDelimiter = cutAwayFillCharacters == null || cutAwayFillCharacters.isEmpty() || (begin - lastEnd == 2 && cutAwayFillCharacters.contains(cas.getDocumentText().charAt(begin - 1)));
			if (lastEnd > 0 && begin > lastEnd && (previousCharacterIsCutAwayDelimiter || moreThanOneCharacterDistance)) {
				cutSum += lastEnd - lastBegin;
				int condensedPosition = lastEnd - cutSum + 1;
				condensedPos2SumCutMap.put(condensedPosition, cutSum);
				originalPos2SumCutMap.put(lastEnd, cutSum);
				lastBegin = begin;
				sb.append(cas.getDocumentText(), lastEnd, begin);
			} else if (lastEnd < 0) {
				lastBegin = begin;
				sb.append(cas.getDocumentText(), 0, begin);
			}
			lastEnd = end;
		}
		// Since we iterate one annotation further than the annotation we store the span
		// for, we need to take care of the very last ignored annotation after the loop
		// - it has never been handled itself.
		if (lastEnd > 0) {
			cutSum += lastEnd - lastBegin;
			int condensedPosition = lastEnd - cutSum + 1;
			condensedPos2SumCutMap.put(condensedPosition, cutSum);
			originalPos2SumCutMap.put(lastEnd, cutSum);
		}
		// If lastEnd is still -1, we just did not find any of the cut away annotations. Thus, we just copy the whole text.
		if (lastEnd == -1)
		    lastEnd = 0;
		if (lastEnd < cas.getDocumentText().length())
			sb.append(cas.getDocumentText().substring(lastEnd));
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
		if (condensedPos2SumCutMap == null)
			return condensedOffset;
		Entry<Integer, Integer> floorEntry = condensedPos2SumCutMap.floorEntry(condensedOffset);
		return condensedOffset + floorEntry.getValue();
	}
	
	/**
	 * Given a character offset relative to the original CAS document text, this method
	 * returns the corresponding offset in the condensed document text.
	 * 
	 * @param originalOffset
	 *            The character offset in the originalOffset document CAS text string.
	 * @return The character offset relative to the condensed document text
	 *         associated with <tt>originalOffset</tt>.
	 */
	public int getCondensedOffsetForOriginalOffset(int originalOffset) {
		if (originalPos2SumCutMap == null)
			return originalOffset;
		Entry<Integer, Integer> floorEntry = originalPos2SumCutMap.floorEntry(originalOffset);
		return originalOffset - floorEntry.getValue();
	}

	public String getCodensedText() {
		return condensedText != null ? condensedText : cas.getDocumentText();
	}
}
