/**
 * TokenAnnotatorTest.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 *
 * Current version: 2.2.3 Since version: 1.0
 *
 * Creation date: Nov 29, 2006
 *
 * This is an UIMA wrapper for the JULIE Token Boundary Detector (JTBD). It
 * produces token annotations, given sentence annotations. Each sentence is
 * separately split into its single tokens. It
 *
 *
 * TODO: double-check whether last symbol is always correctly tokenized!
 **/

package de.julielab.jcore.ae.jtbd.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.ae.jtbd.EOSSymbols;
import de.julielab.jcore.ae.jtbd.Tokenizer;
import de.julielab.jcore.ae.jtbd.Unit;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class TokenAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TokenAnnotator.class);

	public static final String PARAM_MODEL = "ModelFilename";

	private static final String COMPONENT_ID = "JULIE Token Boundary Detector";

	public static final String USE_DOC_TEXT_PARAM = "UseDocText";

	private Tokenizer tokenizer;

	@ConfigurationParameter(name = USE_DOC_TEXT_PARAM, defaultValue = "false")
	private static boolean useCompleteDocText = false;

	private int tokenNumber; // used as token ID

	@ConfigurationParameter(name = PARAM_MODEL, mandatory = true, description = "Path to the tokenizer model.")
	private String modelFilename;

	private void createToken(final JCas jcas, final int begin, final int end) {
		final Token annotation = new Token(jcas);
		annotation.setBegin(begin);
		annotation.setEnd(end);
		annotation.setId("" + tokenNumber);
		annotation.setComponentId(COMPONENT_ID);
		annotation.addToIndexes();
		LOGGER.debug("createToken() - created token: " + jcas.getDocumentText().substring(begin, end)
				+ " "
				+ begin
				+ " - "
				+ end);
		tokenNumber++;
	}

	/**
	 * Initialisiation of JTBD: load the model
	 * 
	 * @parm aContext the parameters in the descriptor
	 */
	@Override
	public void initialize(final UimaContext aContext) throws ResourceInitializationException {

		LOGGER.info("[JTBD] initializing JTBD Annotator ...");

		// invoke default initialization
		super.initialize(aContext);

		// initialize Tokenizer
		tokenizer = new Tokenizer();
		InputStream is = null;
		try {
			// get model file name from parameters
			modelFilename = (String) aContext.getConfigParameterValue(PARAM_MODEL);

			try {
				is = new FileInputStream(modelFilename);
			} catch (IOException e) {
				LOGGER.debug("File \"{}\" does not exist. Searching for the model as a classpath resource.",
						modelFilename);
				is = getClass().getResourceAsStream(modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename);
				if (null == is)
					throw new IllegalArgumentException("The model file \"" + modelFilename
							+ "\" could be found neither in the file system nor in the classpath.");
				LOGGER.info("Loading model as classpathresource");
			}
			tokenizer.readModel(is);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
		// define if sentence annotations should be taken into account
		final Object useDocTextParam = aContext.getConfigParameterValue(USE_DOC_TEXT_PARAM);
		if (useDocTextParam != null)
			useCompleteDocText = (Boolean) useDocTextParam;
		if (useCompleteDocText)
			LOGGER.info("initialize() - whole documentText is tokenized");
		else
			LOGGER.info("initialize() - will tokenize only text covered by sentence annotations");
	}

	/**
	 * the process method is in charge of doing the tokenization
	 */
	@Override
	public void process(final JCas aJCas) throws AnalysisEngineProcessException {

		LOGGER.debug("process() - starting processing document");

		tokenNumber = 1;

		// if useCompleteDocText is true, tokenize complete documentText
		if (useCompleteDocText) {
			LOGGER.debug("process() - tokenizing whole document text!");
			final String text = aJCas.getDocumentText();
			writeTokensToCAS(text, 0, aJCas);
		}
		// if useCompleteDocText is false, tokenize sentence per sentence
		else {
			final JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
			final Iterator<Annotation> sentenceIter = indexes.getAnnotationIndex(Sentence.type).iterator();
			while (sentenceIter.hasNext()) {
				final Sentence sentence = (Sentence) sentenceIter.next();
				LOGGER.debug("process() - going to next sentence having length: " + (sentence.getEnd() - sentence
						.getBegin()));
				final String text = sentence.getCoveredText();
				writeTokensToCAS(text, sentence.getBegin(), aJCas);
			}
		}
	}

	/**
	 * Tokenize non empty input and write tokens to CAS by interpreting the Unit objects. JTBD splits each sentence into
	 * several units (see Tomanek et al. Medinfo 2007 paper) and decides for each such unit whether it is at the end of
	 * a token or not (label "N" means: not at the end, "P": at the end). Makes an extra token for terminal end of
	 * sentence symbols.
	 * 
	 * @param text
	 * @param offset
	 * @param aJCas
	 * @return
	 * @throws AnalysisEngineProcessException
	 */
	private void writeTokensToCAS(final String text, final int offset, final JCas aJCas)
			throws AnalysisEngineProcessException {

		// skip empty input text
		if ((text == null) || text.isEmpty())
			LOGGER.debug("writeTokensToCAS() - input for JTBD tokenizer is null or empty!");
		else {
			// if input text is not a single EOS
			if ((text.length() > 1) || !EOSSymbols.contains(text.charAt(text.length() - 1))) {
				LOGGER.debug("writeTokensToCAS() - tokenizing input: " + text);

				// predict units
				final ArrayList<Unit> units = tokenizer.predict(text);

				LOGGER.debug("+++predition done!++++");

				// throw error if no units could be predicted
				if ((units == null) || (units.size() == 0)) {
					LOGGER.error("writeTokensToCAS() - no units found by JTBD for: " + text);
					throw new AnalysisEngineProcessException();
				}

				int begin = 0;
				int end = 0;
				boolean startNewToken = true;
				// iterate through units, write a token whenever a unit with label 'P' signals the end of a token
				// note that no unit exists for terminal EOS in input text!
				for (final Unit unit : units) {
					if (startNewToken)
						begin = unit.begin + offset;
					end = unit.end + offset;
					if (unit.label.equals("N"))
						startNewToken = false;
					else if (unit.label.equals("P")) {
						createToken(aJCas, begin, end);
						startNewToken = true;
					} else {
						LOGGER.error("writeTokensToCAS() - found unit label '" + unit.label
								+ "' (only 'N' and 'P' are allowed");
						throw new AnalysisEngineProcessException();
					}
				}
				// This case (last unit had label 'N') should not happen. Analysis of JTBD is pending.
				if (!startNewToken) {
					createToken(aJCas, begin, end);
					LOGGER.debug("writeTokensToCAS() - found terminal unit with label 'N' (expected 'P'). Check behaviour of JTBD! Token text: " + aJCas
							.getDocumentText().subSequence(begin, end));
					// throw new AnalysisEngineProcessException();
				}
			}
			// if last character of a sentence is a EOS, make it a separate token
			final Character lastChar = text.charAt(text.length() - 1);
			if (EOSSymbols.contains(lastChar)) {
				final int start = (offset + text.length()) - 1;
				final int end = offset + text.length();
				createToken(aJCas, start, end);
			}
		}

	}

}
