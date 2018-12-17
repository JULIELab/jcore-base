/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.acronymtagger.main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.AbbreviationLongform;
import de.julielab.jcore.utility.index.Comparators;
import de.julielab.jcore.utility.index.JCoReTreeMapAnnotationIndex;
import de.julielab.jcore.utility.index.TermGenerators;
import de.julielab.jcore.utility.index.TermGenerators.LongOffsetIndexTermGenerator;

public class Postprocessing {

	private static final Comparator<Long> overlapComparator = Comparators.longOverlapComparator();
	private final static LongOffsetIndexTermGenerator termGenerator = TermGenerators.longOffsetTermGenerator();

	public static void doPostprocessing(JCas jcas) {
		JCoReTreeMapAnnotationIndex<Long, Abbreviation> acronymIndex = new JCoReTreeMapAnnotationIndex<>(
				overlapComparator, termGenerator, termGenerator);
		acronymIndex.index(jcas, Abbreviation.type);
		unifyForLongestAcronym(jcas, acronymIndex);
		removeAcronymsOnFullforms(jcas, acronymIndex);
	}

	private static void removeAcronymsOnFullforms(JCas jcas,
			JCoReTreeMapAnnotationIndex<Long, Abbreviation> acronymIndex) {
		Set<Abbreviation> toRemove = new HashSet<>();
		FSIterator<Annotation> it = jcas.getAnnotationIndex(AbbreviationLongform.type).iterator();
		while (it.hasNext()) {
			Annotation longForm = it.next();
			Stream<Abbreviation> overlappedAbbreviations = acronymIndex.search(termGenerator.asKey(longForm));
			overlappedAbbreviations.forEach(toRemove::add);
		}
		for (Abbreviation a : toRemove) {
			a.removeFromIndexes();
		}
	}

	private static void unifyForLongestAcronym(JCas jcas,
			JCoReTreeMapAnnotationIndex<Long, Abbreviation> acronymIndex) {
		Set<Abbreviation> toRemove = new HashSet<>();
		FSIterator<Annotation> it = jcas.getAnnotationIndex(Abbreviation.type).iterator();
		while (it.hasNext()) {
			Abbreviation a = (Abbreviation) it.next();
			Stream<Abbreviation> overlappingAbbreviations = acronymIndex.search(a);
			overlappingAbbreviations.filter(oa -> !oa.equals(a)).forEach(overlappingAbbreviation -> {
				if (overlappingAbbreviation != null) {
					AbbreviationLongform aFull = a.getTextReference();
					AbbreviationLongform oFull = overlappingAbbreviation.getTextReference();
					// we allow embedded acronyms when the full forms also
					// overlap
					if (overlapComparator.compare(termGenerator.asKey(aFull), termGenerator.asKey(oFull)) != 0) {
						int aLength = a.getEnd() - a.getBegin();
						int overlappingLength = overlappingAbbreviation.getEnd() - overlappingAbbreviation.getBegin();
						if (aLength > overlappingLength)
							toRemove.add(overlappingAbbreviation);
						if (overlappingLength > aLength)
							toRemove.add(a);
						// if the length should be equal, we can't decide and
						// don't
						// do anything
					}
				}
			});
		}
		for (Abbreviation a : toRemove) {
			a.removeFromIndexes();
		}
	}
}
