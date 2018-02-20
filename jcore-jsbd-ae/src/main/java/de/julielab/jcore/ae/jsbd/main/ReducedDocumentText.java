package de.julielab.jcore.ae.jsbd.main;

import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.uima.jcas.JCas;

import de.julielab.jcore.utility.JCoReAnnotationIndexMerger;

public class ReducedDocumentText {
	private NavigableMap<Integer, Integer> spanSumMap;
	private String reducedText;

	public ReducedDocumentText(JCas cas, Set<String> ignoredTypes) throws ClassNotFoundException {
		buildMap(cas, ignoredTypes);
	}

	/**
	 * Creates a map that maps those positions of the reduced text that correspond
	 * to an intermediate next position after an ignored annotation in the original
	 * text to the sum of ranges covered by ignored annotations up to the original
	 * offset.
	 * 
	 * @param cas
	 * @param ignoredTypes
	 * @throws ClassNotFoundException
	 */
	public void buildMap(JCas cas, Set<String> ignoredTypes) throws ClassNotFoundException {
		StringBuilder sb = new StringBuilder();
		spanSumMap = new TreeMap<>();
		spanSumMap.put(0, 0);
		JCoReAnnotationIndexMerger merger = new JCoReAnnotationIndexMerger(ignoredTypes, true, null, cas);
		int spanSum = 0;
		int lastBegin = 0;
		int lastEnd = -1;
		// For each ignored annotation, there could be following annotations overlapping
		// with the first, effectively enlengthening the ignored span. Thus, we iterate
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
		reducedText = sb.toString();
	}

	public int getOriginalOffsetForReducedOffset(int reducedOffset) {
		Entry<Integer, Integer> floorEntry = spanSumMap.floorEntry(reducedOffset);
		return reducedOffset + floorEntry.getValue();
	}

	public String getReducedText() {
		return reducedText;
	}
}
