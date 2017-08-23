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
package de.julielab.jcore.ae.opennlp.token;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class TokenAnnotator extends JCasAnnotator_ImplBase {

	private static final String PARAM_NAME = "ModelFile";

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(TokenAnnotator.class);

	/**
	 * component Id
	 */
	public static final String COMPONENT_ID = TokenAnnotator.class.getName();

	/**
	 * instance of the OpenNLP Tokenizer
	 */
	private TokenizerME tokenizer;

	private String modelFilePath;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		LOGGER.info("Initializing OpenNLP Token Annotator ...");
		super.initialize(aContext);
		modelFilePath = (String) aContext.getConfigParameterValue(PARAM_NAME);

		try {
			InputStream is;
			File modelFile = new File(modelFilePath);
			if (modelFile.exists()) {
				is = new FileInputStream(modelFile);
			} else {
				String modelResource = modelFilePath.startsWith("/") ? modelFilePath
						: "/" + modelFilePath;
				is = getClass().getResourceAsStream(modelResource);
			}
			if (null == is)
				throw new ResourceInitializationException(
						ResourceInitializationException.COULD_NOT_ACCESS_DATA,
						new Object[] { modelFilePath });
			tokenizer = new TokenizerME(new TokenizerModel(is));
		} catch (IOException e) {
			LOGGER.error("Could not load tokenizer model: " + e.getMessage());
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas aJCas) {
		int tokenNumber = 1;

		LOGGER.trace("Processing document ...");
		AnnotationIndex<Annotation> sentenceIndex = aJCas
				.getJFSIndexRepository().getAnnotationIndex(Sentence.type);

		FSIterator<Annotation> sentenceIterator = sentenceIndex.iterator();
		if (!sentenceIterator.hasNext())
			LOGGER.debug("Current document has no annotations of type {}, skipping.", Sentence.class);
		while (sentenceIterator.hasNext()) {
			Sentence sentence = (Sentence) sentenceIterator.next();

			String text = sentence.getCoveredText();
			Span[] tokenSpans = tokenizer.tokenizePos(text);
			for (int i = 0; i < tokenSpans.length; i++) {
				Span span = tokenSpans[i];
				Token token = new Token(aJCas);
				token.setId("" + tokenNumber);
				token.setBegin(sentence.getBegin() + span.getStart());
				token.setEnd(sentence.getBegin() + span.getEnd());
				token.setComponentId(COMPONENT_ID);
				token.addToIndexes();
				tokenNumber++;

			}
		}

	}
}
