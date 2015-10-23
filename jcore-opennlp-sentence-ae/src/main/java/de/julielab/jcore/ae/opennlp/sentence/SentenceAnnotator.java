/** 
 * OpennlpSentenceDetector.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: buyko
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: 30.01.2008
 * 
 * This wrapper provides the interface to the OpenNLP Sentence Detector
 * @see http://opennlp.sourceforge.net/
 **/

package de.julielab.jcore.ae.opennlp.sentence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jules.types.Sentence;

public class SentenceAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(SentenceAnnotator.class);

	/**
	 * component id for CAS
	 */
	private static final String COMPONENT_ID = "de.julielab.jcore.ae.opennlp.sentence.OpenNLPSentenceDetector";

	/**
	 * OpenNLP Maximum Entropy SentenceDetector instance
	 */
	private opennlp.tools.sentdetect.SentenceDetectorME sentenceSplitter;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {

		LOGGER.info("initializing OpenNLP Sentence Annotator ...");

		super.initialize(aContext);

		/*
		 * Get the sentence model
		 */
		
		// path to the model (File) - or name of the model (in Classpath)
		String modelFilename = "";
		
		final Object o = aContext.getConfigParameterValue("modelFile");
		if (o != null)
			modelFilename = (String) o;
		else
		{
			LOGGER.error("[OpenNLP Sentence Annotator] descriptor incomplete, no model file specified!");
			throw new ResourceInitializationException();
		}
		
		// Read the Model
		LOGGER.debug("[OpenNLP Sentence Annotator] Reading sentence model...");
		final File modelFile = new File(modelFilename);
		
		InputStream is;
		if (!modelFile.exists())
		{
			// perhaps the parameter value does not point to a file but to a classpath resource
			String resourceLocation = modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename;
			is = getClass().getResourceAsStream(resourceLocation);
		}
		else
		{		
			try
			{
				is = new FileInputStream(modelFile);
			}
			catch (FileNotFoundException e)
			{
				LOGGER.error("[OpenNLP Sentence Annotator] Sentence model {} not found.", modelFile.getAbsolutePath());
				throw new ResourceInitializationException();
			}
		}
		
		// Load the model
		try
		{
			SentenceModel model = new SentenceModel(is);
			sentenceSplitter = new SentenceDetectorME(model);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			LOGGER.error("[OpenNLP Sentence Annotator] Error while opening sentence model {}. . Sentence Annotations will not be done.",modelFile);
		}
		finally
		{
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	@Override
	public void process(JCas aJCas)
	{
		LOGGER.info(" [OpenNLP Sentence Annotator] processing document ...");
		String text = aJCas.getDocumentText();
		// the span of the sentences in the input string
		Span sentenceOffsets[] = sentenceSplitter.sentPosDetect(text);
		for (int i = 0; i < sentenceOffsets.length; i++) {

		}
		for (int i = 0; i < sentenceOffsets.length; i++)
		{
			Sentence sentence = new Sentence(aJCas, sentenceOffsets[i].getStart(), sentenceOffsets[i].getEnd());
			sentence.setComponentId(COMPONENT_ID);
			sentence.addToIndexes();
		}
	}

}
