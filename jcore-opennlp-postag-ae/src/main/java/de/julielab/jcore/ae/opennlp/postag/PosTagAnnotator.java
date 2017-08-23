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
package de.julielab.jcore.ae.opennlp.postag;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class PosTagAnnotator extends JCasAnnotator_ImplBase {

	public static final String PARAM_MODEL_FILE = "modelFile";

	public static final String PARAM_TAGSET = "tagset";

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PosTagAnnotator.class);

	/**
	 * instance of the Opennlp POS Tagger
	 */
	private POSTaggerME tagger;

	private Constructor<?> typeConstructor;
	/**
	 * the used postagset
	 */
	@ConfigurationParameter(
			name = PARAM_TAGSET,
			description = "The UIMA POSTag subtype to be used for the POS annotations.",
			mandatory = true)
	private String postagset;

	@ConfigurationParameter(
			name = PARAM_MODEL_FILE,
			description = "The OpenNLP POS model file path. It is expected that a tag dictionary has been included into the model at training time.",
			mandatory = true)
	private String modelFilePath;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

		super.initialize(aContext);

		try {

			LOGGER.info("[OpenNLP POSTag Annotator]initializing OpenNLP POSTag Annotator ...");
			// Get configuration parameter values
			modelFilePath = (String) aContext.getConfigParameterValue(PARAM_MODEL_FILE);
			postagset = (String) aContext.getConfigParameterValue(PARAM_TAGSET);

			InputStream modelIs;
			File modelFile = new File(modelFilePath);
			if (modelFile.exists()) {
				modelIs = new FileInputStream(modelFile);
			} else {
				LOGGER.debug("File \"{}\" does not exist. Searching for the model as a classpath resource.",
						modelFilePath);
				modelIs =
						getClass().getResourceAsStream(
								modelFilePath.startsWith("/") ? modelFilePath : "/" + modelFilePath);
				if (null == modelIs)
					throw new IllegalArgumentException("The model file \"" + modelFilePath
							+ "\" could be found neither in the file system nor in the classpath.");
			}
			// Get OpenNLP POS Tagger, initialize with a model
			POSModel posModel = new POSModel(modelIs);
			tagger = new POSTaggerME(posModel);
		} catch (Exception e) {
			// LOGGER.error("[OpenNLP POStag Annotator] Could not load Part-of-speech model: " + e.getMessage());
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas aJCas) {

		LOGGER.debug("[OpenNLP POSTag Annotator]  processing document ...");
		ArrayList<Token> tokenList = new ArrayList<Token>();
		ArrayList<String> tokenTextList = new ArrayList<String>();

		FSIterator<Annotation> sentenceIterator = aJCas.getAnnotationIndex(Sentence.type).iterator();
		AnnotationIndex<Annotation> tokenIndex = aJCas.getAnnotationIndex(Token.type);

		if (!sentenceIterator.hasNext())
			LOGGER.debug("CAS has no sentences, no POS tags will be added");
		
		// iterate over Sentences
		while (sentenceIterator.hasNext()) {
			tokenList.clear();
			tokenTextList.clear();
			Sentence sentence = (Sentence) sentenceIterator.next();

			// iterate over Tokens
			FSIterator<Annotation> tokenIterator = tokenIndex.subiterator(sentence);
			if (!tokenIterator.hasNext())
				LOGGER.debug("Sentence has no tokens, no POS tags will be added for this sentence");
			while (tokenIterator.hasNext()) {
				Token token = (Token) tokenIterator.next();
				tokenList.add(token);
				tokenTextList.add(token.getCoveredText());
			}

			String[] tokenTagList = tagger.tag(tokenTextList.toArray(new String[tokenList.size()]));

			try {
				for (int i = 0; i < tokenList.size(); i++) {
					Token token = (Token) tokenList.get(i);
					String posTag = (String) tokenTagList[i];
					POSTag pos = null;

					try {
						if (null == typeConstructor) {
							Class<?>[] parameterTypes = new Class[] { JCas.class };
							Class<?> typeClass = Class.forName(postagset);
							typeConstructor = typeClass.getConstructor(parameterTypes);
						}
						pos = (POSTag) typeConstructor.newInstance(aJCas);
						pos.setBegin(token.getBegin());
						pos.setEnd(token.getEnd());
						pos.setValue(posTag);
						pos.setComponentId(PosTagAnnotator.class.getName());
						pos.addToIndexes();

					} catch (SecurityException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (IllegalArgumentException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (ClassNotFoundException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (NoSuchMethodException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (InstantiationException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (IllegalAccessException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					} catch (InvocationTargetException e1) {
						LOGGER.error("[OpenNLP POSTag Annotator]" + e1.getMessage());
					}

					FSArray postags = token.getPosTag();
					if (postags == null) {
						postags = new FSArray(aJCas, 1);
						try {
							postags.set(0, pos);
						} catch (CASRuntimeException e) {
							LOGGER.error("[OpenNLP POSTag Annotator]" + e.getMessage());
						}
						token.setPosTag(postags);
					} else {
						int numPosTags = postags.size();
						int lastElementIndex = numPosTags - 1;
						if (postags.get(lastElementIndex) != null) {
							FSArray extendedPosTags = new FSArray(aJCas, numPosTags + 1);
							extendedPosTags.copyFromArray(postags.toArray(), 0, 0, numPosTags);
							extendedPosTags.set(numPosTags, pos);
							postags = extendedPosTags;
						} else {
							while ((lastElementIndex > 0) && (postags.get(lastElementIndex - 1) == null)) {
								lastElementIndex--;
							}
							postags.set(lastElementIndex, pos);
						}
					}

				}
			} catch (CASRuntimeException e) {
				LOGGER.error("[OpenNLP POSTag Annotator]" + e.getMessage());
				LOGGER.error("[OpenNLP POSTag Annotator]  list of tags shorter than list of words");
			}
		}
	}

}