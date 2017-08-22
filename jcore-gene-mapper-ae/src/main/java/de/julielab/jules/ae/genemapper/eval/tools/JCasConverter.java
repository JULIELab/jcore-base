package de.julielab.jules.ae.genemapper.eval.tools;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.commons.lang3.Range;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.types.Sentence;

public class JCasConverter {

	public static NavigableSet<Range<Integer>> mapSentences2Ranges(JCas aJCas) {
		NavigableSet<Range<Integer>> sentences = new TreeSet<Range<Integer>>(new Comparator<Range<Integer>>() {

			@Override
			public int compare(Range<Integer> gm1, Range<Integer> gm2) {
				if (gm1.getMinimum() == gm2.getMinimum()) {
					if (gm1.getMaximum() == gm2.getMaximum()) {
						return 0;
					} else if (gm1.getMaximum() < gm2.getMaximum()) {
						return -1;
					} else {
						return 1;
					}
				} else if (gm1.getMinimum() < gm2.getMinimum()) {
					return -1;
				} else {
					return 1;
				}
			}
				
		});

		FSIterator<Annotation> sentenceIt = aJCas.getAnnotationIndex(Sentence.type).iterator();
		while (sentenceIt.hasNext()) {
			Sentence sentence = (Sentence) sentenceIt.next();
			sentences.add(Range.between(sentence.getBegin(), sentence.getEnd()));
		}
		return sentences;
	}
}
