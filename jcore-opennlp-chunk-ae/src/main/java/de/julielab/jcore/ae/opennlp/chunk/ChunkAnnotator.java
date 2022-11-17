/** 
 * OpenNLPChunker.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: buyko
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: 30.01.2008 
 * 
 * OpenNLP Chunker provides chunks to tokens in IOB format (e.g. B-NP, I-VP). 
 * This UIMA wrapper provides all needed input parameters to the OpenNLP Chunker and converts the IOB output of 
 * the OpenNLP Chunker in CAS. 
 **/
package de.julielab.jcore.ae.opennlp.chunk;

import de.julielab.jcore.types.*;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChunkAnnotator extends JCasAnnotator_ImplBase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ChunkAnnotator.class);
	/**
	 * component Id
	 */
	private static final String COMPONENT_ID = "de.julielab.jcore.ae.OpenNLPChunker";
	/**
	 * instance of the OpenNLP chunker
	 */
	private ChunkerME chunker;
	/**
	 * the POS Tagset preferred by this Chunker. If Chunker is trained on PennBioIE, it is
	 * recommendable to use PennBioIE POS Tagset
	 */
	private static String posTagSetPreference;
	/**
	 * chunk tag mappings names
	 */	
	private static String[] mappings;


	/**
	 * mappings from chunk tags to CAS
	 */
	private static Map<String, String> mappingsTable;

	private static Map<String, Integer> defaultChunkTagMap;

	private static String[] DEFAULT_CHUNK_TAGS = new String[]{"NP", "PP", "VP", "ADJP", "CONJP", "LST", "SBAR", "PRT", "ADVP"};



	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		LOGGER.info("initializing OpenNLP Chunk Annotator ...");
		super.initialize(aContext);

		// path to the model (File) - or name of the model (in Classpath)
		String modelFileName = "";

		final Object o = aContext.getConfigParameterValue("modelFile");
		if (o != null)
			modelFileName = (String) o;
		else
		{
			LOGGER.error("[OpenNLP Chunk Annotator] descriptor incomplete, no model file specified!");
			throw new ResourceInitializationException();
		}

		// POS Tagset preferred by this Chunker
		String posTagSetPreference = "";
		final Object o2 = aContext.getConfigParameterValue("posTagSetPref");
		if (o2 != null)
			posTagSetPreference = (String) o2;
		else
		{
			LOGGER.error("[OpenNLP Chunk Annotator] descriptor incomplete, posTagSetPref not specified!");
			throw new ResourceInitializationException();
		}

		// Mappings
		mappings = (String[]) aContext.getConfigParameterValue("mappings");		
		if (mappings != null && mappings.length > 0){
			try {
				loadMappings();
			} catch (AnnotatorConfigurationException e) {
				throw new ResourceInitializationException(e);
			}
		}
		else {
			loadDefaultChunkTagMap();
			LOGGER.info("[OpenNLP Chunk Annotator] Working with default mappings in getChunk()");
		}

		// Read the Model
		LOGGER.debug("[OpenNLP Chunk Annotator] Reading sentence model...");
		final File modelFile = new File(modelFileName);

		InputStream is;
		if (!modelFile.exists())
		{
			// perhaps the parameter value does not point to a file but to a classpath resource
			String resourceLocation = modelFileName.startsWith("/") ? modelFileName : "/" + modelFileName;
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
				LOGGER.error("[OpenNLP Chunk Annotator] Sentence model {} not found.", modelFile.getAbsolutePath());
				throw new ResourceInitializationException();
			}
		}


		// Load the model
		try
		{
			ChunkerModel chunkerModel = new ChunkerModel(is);
			chunker = new ChunkerME(chunkerModel);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			LOGGER.error("[OpenNLP Chunk Annotator] Error while opening chunk model {}. . Sentence Annotations will not be done.",modelFile);
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

	private void loadDefaultChunkTagMap() {
		defaultChunkTagMap = new HashMap<String, Integer>();
		int i = 0;
		for (String chunkTag : DEFAULT_CHUNK_TAGS) {
			defaultChunkTagMap.put(chunkTag, i);
			i++;
		}
	}


	// load mappings of chunk tag names
	private void loadMappings() throws AnnotatorConfigurationException {
		mappingsTable = new HashMap<String, String>();
		for (int i = 0; i < mappings.length; i++) {
			String[] pair = mappings[i].split(";");
			if (pair.length != 2) {
				throw new AnnotatorConfigurationException();
			} 
			else {
				String chunkTag = pair[0];
				String classTag = pair[1];
				mappingsTable.put(chunkTag, classTag);
			}
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {

		LOGGER.debug("[OpenNLP Chunk Annotator] processing document ...");		

		JFSIndexRepository indexes = aJCas.getJFSIndexRepository();		
		AnnotationIndex<Annotation> sentenceIndex = indexes.getAnnotationIndex(Sentence.type);
		AnnotationIndex<Annotation> tokenIndex = indexes.getAnnotationIndex(Token.type);
		FSIterator<Annotation> sentenceIterator = sentenceIndex.iterator();

		//iterate over Sentences
		while (sentenceIterator.hasNext()) {

			Sentence sentence = (Sentence) sentenceIterator.next();
			FSIterator tokenIterator = tokenIndex.subiterator(sentence);

			//get number of Tokens contained in Sentence and move iterator back to beginning
//			int numTokens = 0;
//			while (tokenIterator.isValid()){
//				numTokens++;
//				tokenIterator.moveToNext();
//			}
//			tokenIterator.moveToFirst();
//			Token[] tokenArray = new Token[numTokens];
//			String[] tokenTextArray = new String[numTokens];
//			String[] tagArray = new String[numTokens];
			java.util.List<Token> tokensInSentence = new ArrayList<>();
			java.util.List<String> tokenTags = new ArrayList<>();

			int i = 0;

			// iterate over Tokens in current sentence
			while (tokenIterator.hasNext()) {
				Token token = (Token) tokenIterator.next();
				tokensInSentence.add(token);
//				tokenArray[i] = token;
//				tokenTextArray[i] = token.getCoveredText();
				POSTag postag = null;
				// if a POS TagSet preference exists try to get a correspondent POSTag for the current token
				if (posTagSetPreference != null) {
					postag = getPrefPOSTag(token);
				} 
				//if no preferred POS tag was found or no preference exists get the first POS tag available...
				if (postag == null){ 
					postag = token.getPosTag(0);
				}
				//if no POS tag exists at all, throw error message
				if (postag == null){
					LOGGER.error("Token has no POS tag annotation: " + token.getCoveredText());
					throw new AnalysisEngineProcessException();
				}
//				tagArray[i] = postag.getValue();
				tokenTags.add(postag.getValue());
				i++;
			}

			// OpenNLP Chunker predicts chunks
//			String[] chunks = chunker.chunk(tokenTextArray, tagArray);
			String[] chunks = chunker.chunk(tokensInSentence.stream().map(Token::getCoveredText).toArray(String[]::new), tokenTags.toArray(String[]::new));
			createChunkAnnotations(chunks, tokensInSentence.toArray(Token[]::new), aJCas);

		}
	}

	/**
	 * Creates Chunk annotations based on the predicted chunks. 
	 * @param chunks
	 * @param tokenArray
	 * @param aJCas
	 * @throws AnalysisEngineProcessException
	 */
	private void createChunkAnnotations(String[] chunks, Token[] tokenArray, JCas aJCas) throws AnalysisEngineProcessException {

		Chunk chunkAnnotation = null;
		boolean inChunk = false;
		for (int index = 0; index < chunks.length; index++) {
			// if chunk is of type 'begin'...
			if (chunks[index].charAt(0) == 'B') {
				//...complete already started Chunk annotation before starting a new one
				if (inChunk){
					chunkAnnotation.setEnd(tokenArray[index - 1].getEnd());
					chunkAnnotation.addToIndexes();
				}
				String chunkType = chunks[index].substring(2); //"B-xxx"
				//...start new Chunk annotation
				chunkAnnotation = getChunk(chunkType, aJCas);
				chunkAnnotation.setComponentId(COMPONENT_ID);
				chunkAnnotation.setBegin(tokenArray[index].getBegin());
				inChunk = true;
			}
			// if chunk is of type 'outside' or is the last predicted chunk...
			else if (chunks[index].charAt(0) == 'O' || index + 1 == chunks.length) {
				// ... complete previously started Chunk annotation 
				if (inChunk){
					chunkAnnotation.setEnd(tokenArray[index-1].getEnd());
					chunkAnnotation.addToIndexes();
				}
				inChunk = false;
			}
		}		
	}

	/**
	 * Tries to create annotation of type chunkValue. In case of success returns 
	 * the annotation. Otherwise throws an exception.
	 * 
	 * @param chunkValue
	 * @param aJCas
	 * @return
	 * @throws AnalysisEngineProcessException
	 */
	private Chunk getChunk(String chunkValue, JCas aJCas) throws AnalysisEngineProcessException {

		Chunk chunk = null;

		//if no mappings have been specified in the descriptor, works with a list of default mappings:
		if (mappings == null || mappings.length == 0){

			Integer value = defaultChunkTagMap.get(chunkValue);
			if (value == null) {
				value = DEFAULT_CHUNK_TAGS.length;
			}
			switch(value) {
			case 0: 
				chunk = new ChunkNP(aJCas); 
				break;
			case 1: 
				chunk = new ChunkPP(aJCas); 
				break;
			case 2: 
				chunk = new ChunkVP(aJCas); 
				break;
			case 3: 
				chunk = new ChunkADJP(aJCas); 
				break;
			case 4: 
				chunk = new ChunkCONJP(aJCas); 
				break;
			case 5: 
				chunk = new ChunkLST(aJCas); 
				break;
			case 6: 
				chunk = new ChunkSBAR(aJCas); 
				break;
			case 7: 
				chunk = new ChunkPRT(aJCas); 
				break;
			case 8: 
				chunk = new ChunkADVP(aJCas); 
				break;
			default: 
				chunk = new Chunk(aJCas); 
				LOGGER.debug("Chunk type " + chunkValue + " is not in the list of predefined mappings.");
			}

			//	        if (chunkValue.equals("NP")){
			//	        	chunk = new ChunkNP(aJCas);
			//	        }
			//	        else if (chunkValue.equals("PP")){
			//	        	chunk = new ChunkPP(aJCas);
			//	        }
			//	        else if (chunkValue.equals("VP")){
			//	        	chunk = new ChunkVP(aJCas);
			//	        }
			//	        else if (chunkValue.equals("ADJP")){
			//	        	chunk = new ChunkADJP(aJCas);
			//	        }
			//	        else if (chunkValue.equals("CONJP")){
			//	        	chunk = new ChunkCONJP(aJCas);
			//	        }
			//	        else if (chunkValue.equals("LST")){
			//	        	chunk = new ChunkLST(aJCas);
			//	        }
			//	        else if (chunkValue.equals("SBAR")){
			//	        	chunk = new ChunkSBAR(aJCas);
			//	        }
			//	        else if (chunkValue.equals("PRT")){
			//	        	chunk = new ChunkPRT(aJCas);
			//	        }
			//	        else if (chunkValue.equals("ADVP")){
			//	        	chunk = new ChunkADVP(aJCas);
			//	        }
			//	        else {
			//	        	chunk = new Chunk(aJCas);
			//	        	LOGGER.info("Chunk type " + chunkValue + " is not in the list of predefined mappings.");
			//	        }
		}
		else {
			try {
				// look for a class name in mappingsTable
				String className = (String) mappingsTable.get(chunkValue);
				if (className != null) {
					Class<?> myNewClass = Class.forName(className);
					Constructor<?> myConstructor = myNewClass.getConstructor(new Class[] { JCas.class });
					chunk = (Chunk) myConstructor.newInstance(aJCas);
				} 
				else {
					chunk = new Chunk(aJCas);
					LOGGER.debug("Chunk " + chunkValue + " is not in mapping file.");				
				}
			} catch (SecurityException e) {
				LOGGER.error("[OpenNLP Chunk Annotator]" + e.getMessage());
				throw new AnalysisEngineProcessException(e);
			} catch (IllegalArgumentException e) {
				LOGGER.error("[OpenNLP Chunk Annotator]" + e.getMessage());
				throw new AnalysisEngineProcessException(e);
			} catch (ClassNotFoundException e) {
				LOGGER.error("[OpenNLP Chunk Annotator]" + e.getMessage());
				throw new AnalysisEngineProcessException(e);
			} catch (NoSuchMethodException e) {
				LOGGER.error("[OpenNLP Chunk Annotator]" + e.getMessage());
				throw new AnalysisEngineProcessException(e);
			} catch (InstantiationException e) {
				LOGGER.error("[OpenNLP Chunk Annotator]" + e.getMessage());
				throw new AnalysisEngineProcessException(e);
			} catch (IllegalAccessException e) {
				LOGGER.error("[OpenNLP Chunk Annotator]" + e.getMessage());
				throw new AnalysisEngineProcessException(e);
			} catch (InvocationTargetException e) {
				LOGGER.error("[OpenNLP Chunk Annotator]" + e.getMessage());
				throw new AnalysisEngineProcessException(e);
			}
		}
		return chunk;
	}


	/**
	 * Returns the first POSTag annotation associated with the given token that has the 
	 * required type (i.e. that belongs to the requested posTagSet). If no such POSTag
	 * is found, returns null. (In general tokens may be provided with POSTags from 
	 * different POSTagSets.)
	 * 
	 * @param token
	 * @return
	 */
	private POSTag getPrefPOSTag(Token token) {
		FSArray posTags = token.getPosTag();
		for (int i = 0; i < posTags.size(); i++) {
			POSTag posTag = (POSTag) posTags.get(i);
			if (posTag != null) {
				// compare to the desired type of POS Tag Set
				if (posTag.getType().getName().equals(posTagSetPreference)) {
					return posTag;
				}
			}
		}
		return null;
	}

}
