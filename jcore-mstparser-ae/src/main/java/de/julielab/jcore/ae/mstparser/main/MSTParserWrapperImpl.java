/** 
 * MSTParserWrapper.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the  Eclipse Public License (EPL) v3.0
 *
 * Author: Lichtenwald
 * 
 * Current version: 2.1
 * Since version:   1.0
 *
 * Creation date: Jan 15, 2008 
 * 
 * This is the wrapper for the MST parser.
 **/
package de.julielab.jcore.ae.mstparser.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upenn.seas.mstparser.Alphabet;
import edu.upenn.seas.mstparser.DependencyParser;
import edu.upenn.seas.mstparser.DependencyPipe;
import edu.upenn.seas.mstparser.ParserOptions;

/**
 * This is the wrapper for the MST parser.
 * 
 * @author Lichtenwald
 */
public class MSTParserWrapperImpl implements MSTParserWrapper, SharedResourceObject {

	static Random random = new Random(System.currentTimeMillis());

	public static final String COMPONENT_ID = "de.julielab.jcore.ae.mstparser.main.MSTParserWrapper";

	private static Alphabet dataAlphabet;

	private static double[] parameters;

	private static final Logger LOGGER = LoggerFactory.getLogger(MSTParserWrapperImpl.class);

	private static final String EMPTY_STRING = "";

	private static boolean first = false;

	private static Alphabet typeAlphabet;

	private static String format;

	private static String modelFilename;

	/**
	 * Load the parser model and to set up some parameters.
	 * 
	 * @param modelFilename
	 *            String which specifies the path to the model file
	 * @param temporaryPath
	 *            String which specifies the path to the directory where
	 *            temporary files will be stored
	 * @param projective
	 *            Boolean which determines whether the dependency relations are
	 *            projective
	 * @param format
	 *            String which specifies the format of test sentences (allowed
	 *            values: MST, CONLL)
	 * @return parser DependencyParser which was properly set up using the
	 *         configuration input parameters
	 * @throws Exception
	 */
	// This isn't like a shared resource should be used in UIMA. The
	// MSTParserWrapperImpl keeps the loaded data as static member
	// variables. I think it works (at least as long as there is only
	// one parser or one configuration used) but isn't quite the
	// canonical way.
	public DependencyParser loadModel() {
		ParserOptions options = new ParserOptions(new String[] { "model-name:" + modelFilename, "format:" + format });
		try {
			DependencyPipe pipe = new DependencyPipe(options);
			loadModelToPipe(pipe, options.modelName);
			DependencyParser parser = new DependencyParser(pipe, options);
			parser.setParameters(parameters);
			return parser;
		} catch (IOException e) {
			LOGGER.error("loadModel - Could not read from file. Message: " + e.getMessage());
			throw new RuntimeException(e);
		} catch (Exception e) {
			LOGGER.error("loadModel - An Error occurred. Message: " + e.getMessage());
		}
		return null;
	}

	private synchronized static boolean checkFirst() {
		if (!first) {
			first = true;
			return true;
		}
		return false;
	}

	private static synchronized void loadModelToPipe(DependencyPipe pipe, String file) {
		pipe.dataAlphabet = dataAlphabet;
		pipe.typeAlphabet = typeAlphabet;
		while (pipe.dataAlphabet == null && pipe.typeAlphabet == null) {
			try {
				// System.out.println(pipe.testint + " waiting");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pipe.dataAlphabet = dataAlphabet;
			pipe.typeAlphabet = typeAlphabet;
		}
		pipe.closeAlphabets();
	}

	/**
	 * Let the parser parse the input sentence and then return the parsed
	 * sentence.
	 * 
	 * @param parser
	 *            - DependencyParser which will be parsing the input sentence
	 * @param inputSentence
	 *            - String which specifies the sentence to be parsed by the
	 *            parser
	 * @return parsedSentence String which specifies the result of the parsing
	 * @throws IOException
	 */
	public String predict(DependencyParser parser, String inputSentence) throws IOException {
		if (StringUtils.isBlank(inputSentence)) {
			LOGGER.warn("Input sentence was empty.");
			return null;
		}
		String parsedSentence = null;
		try {
			parsedSentence = parser.outputParses(inputSentence);

		} catch (Exception e) {
			LOGGER.error("predict - Could not parse: " + e.getMessage());
			LOGGER.error("Input sentence was: " + inputSentence);
			e.printStackTrace();
		} // of catch
		return parsedSentence;
	}

	public void load(DataResource resource) throws ResourceInitializationException {
		File configFile = new File(resource.getUri());
		try {
			BufferedReader reader = new BufferedReader(new FileReader(configFile));
			String value_modelFileName = reader.readLine();
			String value_format = reader.readLine();
			try {
				if (checkParameters(value_modelFileName, value_format))
					loadModelParts(value_modelFileName, value_format);
			} catch (Exception e) {
				LOGGER.error("Can't load Model from file " + value_modelFileName);
			}
		} catch (IOException e) {
			LOGGER.error("Can't read parameters from Config File " + configFile);
		}
	}

	private static synchronized void loadModelParts(String modelFilename, String format) {
		MSTParserWrapperImpl.modelFilename = modelFilename;
		MSTParserWrapperImpl.format = format;
		if (checkFirst()) {
			ObjectInputStream in;
			try {
				LOGGER.info("Loading model...");
				in = new ObjectInputStream(new FileInputStream(modelFilename));
				parameters = (double[]) in.readObject();
				dataAlphabet = (Alphabet) in.readObject();
				typeAlphabet = (Alphabet) in.readObject();
				in.close();
			} catch (FileNotFoundException e) {
				LOGGER.error("Model could not be loaded");
				e.printStackTrace();
			} catch (IOException e) {
				LOGGER.error("Model could not be loaded");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				LOGGER.error("Model could not be loaded");
				e.printStackTrace();
			}
		} else {
			LOGGER.info("Model already loaded");
		}
	}

	/**
	 * Checks the validity of the parameters obtained from the descriptor.
	 * Parameters are valid if they all have values and if these values are
	 * allowed
	 * 
	 * @param value_Format
	 * @param value_TemporaryPath
	 * @param value_ModelFileName
	 * 
	 * @return valid boolean which indicates the validity of the parameters
	 */
	private boolean checkParameters(String value_modelFileName, String value_format) {
		boolean parameters_valid = true;
		if (value_modelFileName == null) {
			LOGGER.error("Parameter " + MSTParserAnnotator.MODEL_FILE_NAME + " has an invalid value: " + value_modelFileName
					+ ". Please set it to a valid value.");
			parameters_valid = false;
		} else {
			File file = new File(value_modelFileName);
			if (!file.exists()) {
				LOGGER.error("File " + value_modelFileName + " does not exist. Please check the parameter " + MSTParserAnnotator.MODEL_FILE_NAME + "!");
				parameters_valid = false;
			} else if (!file.canRead()) {
				LOGGER.error("Cannot read the file " + value_modelFileName + "! Please check the file properties.");
				parameters_valid = false;
			}
		}
		if (value_format == null || !(value_format.equals(MSTParserAnnotator.FORMAT_CONLL) || value_format.equals(MSTParserAnnotator.FORMAT_MST))) {
			LOGGER.error("Parameter " + MSTParserAnnotator.FORMAT + " has an invalid value: " + value_format + ". Allowed are only \""
					+ MSTParserAnnotator.FORMAT_CONLL + "\" and \"" + MSTParserAnnotator.FORMAT_MST + "\".");
			parameters_valid = false;
			value_format = EMPTY_STRING;
		}
		return parameters_valid;
	}

}
