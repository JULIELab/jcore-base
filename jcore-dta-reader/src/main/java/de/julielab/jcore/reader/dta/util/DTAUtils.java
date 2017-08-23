/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.reader.dta.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;

import de.julielab.jcore.reader.dta.DTAFileReader;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public class DTAUtils {
	public static CollectionReader getReader(final String inputFile,
			final boolean normalize) throws InvalidXMLException, IOException,
			ResourceInitializationException {
		return CollectionReaderFactory.createReader(DTAFileReader.class,
				DTAFileReader.PARAM_INPUTFILE, inputFile,
				DTAFileReader.PARAM_NORMALIZE, normalize);
	}

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
			final int windowPerSide) {
		final List<List<String>> list = new ArrayList<>();
		final FSIterator<Annotation> sentences = jcas.getAnnotationIndex(
				Sentence.type).iterator();
		while (sentences.hasNext()) {
			final Sentence sentence = (Sentence) sentences.next();
			final List<Token> tokens = JCasUtil.selectCovered(Token.class,
					sentence);
			if (tokens.size() < ((windowPerSide * 2) + 1))
				continue; // to short
			for (int i = windowPerSide; i < (tokens.size() - windowPerSide); ++i) {
				final List<String> inWindow = new ArrayList<>(
						(windowPerSide * 2) + 1);
				list.add(inWindow);
				for (int j = i - windowPerSide; j < (i + windowPerSide + 1); ++j)
					inWindow.add(tokens.get(j).getCoveredText());
			}
		}
		return list;
	}
}