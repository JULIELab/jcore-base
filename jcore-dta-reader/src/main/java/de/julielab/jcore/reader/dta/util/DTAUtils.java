package de.julielab.jcore.reader.dta.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public class DTAUtils {
	public static boolean hasAnyClassification(final JCas jcas, final Class<?>... classes) {
		final FSIterator<Annotation> it = jcas.getAnnotationIndex(DocumentClassification.type).iterator();
		while (it.hasNext()) {
			final DocumentClassification classification = (DocumentClassification) it.next();
			for (final Class<?> c : classes)
				if (c.isInstance(classification))
					return true;
		}
		return false;
	}

	public static List<List<String>> slidingSymetricWindow(final JCas jcas, int windowPerSide) {
		List<List<String>> list = new ArrayList<>();
		FSIterator<Annotation> sentences = jcas.getAnnotationIndex(Sentence.type).iterator();
		while (sentences.hasNext()) {
			Sentence sentence = (Sentence) sentences.next();
			List<Token> tokens = JCasUtil.selectCovered(Token.class, sentence);
			if (tokens.size() < windowPerSide * 2 + 1)
				continue; // to short
			for (int i = windowPerSide; i < tokens.size() - windowPerSide; ++i) {
				List<String> inWindow = new ArrayList<>(windowPerSide * 2 + 1);
				list.add(inWindow);
				for (int j = i - windowPerSide; j < i + windowPerSide + 1; ++j)
					inWindow.add(tokens.get(j).getCoveredText());
			}
		}
		return list;
	}
}