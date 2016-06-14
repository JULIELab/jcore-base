package de.julielab.jcore.reader.dta.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

import de.julielab.jcore.reader.dta.DTAFileReader;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public class DTAUtils {
	public static boolean hasAnyClassification(final JCas jcas,
			final Class<?>... classes) {
		final FSIterator<Annotation> it = jcas.getAnnotationIndex(
				DocumentClassification.type).iterator();
		while (it.hasNext()) {
			final DocumentClassification classification = (DocumentClassification) it
					.next();
			for (final Class<?> c : classes)
				if (c.isInstance(classification))
					return true;
		}
		return false;
	}

	public static List<List<String>> slidingSymetricWindow(final JCas jcas,
			int windowPerSide) {
		List<List<String>> list = new ArrayList<>();
		FSIterator<Annotation> sentences = jcas.getAnnotationIndex(
				Sentence.type).iterator();
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

	public static CollectionReader getReader(final String inputFile,
			boolean normalize) throws InvalidXMLException, IOException,
			ResourceInitializationException {
		return CollectionReaderFactory.createReader(DTAFileReader.class,
				DTAFileReader.PARAM_INPUTFILE, inputFile,
				DTAFileReader.PARAM_NORMALIZE, normalize);
	}
}