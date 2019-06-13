package de.julielab.jcore.ae.jemas;

import junit.framework.TestCase;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class EmotionAnalyzerTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(EmotionAnalyzerTest.class);
	private static final String DESCRIPTOR = "src/main/resources/de/julielab/jcore/ae/jemas/desc/jcore-jemas-ae.xml";

	private static final String TEST_SENTENCES = "Plectranthus barbatus is a medicinal plant used to treat a wide range of disorders including seizure.";
	private static final int[] tokenBegins = { 0, 13, 22, 25, 27, 37, 43, 48,
			51, 57, 59, 64, 70, 73, 83, 93, 100 };
	private static final int[] tokenEnds = { 12, 21, 24, 26, 36, 42, 47, 50,
			56, 58, 63, 69, 72, 82, 92, 100, 101 };
	private static final String[] posTags = { "NN", "NN", "VBZ", "DT", "JJ",
			"NN", "VBN", "TO", "VB", "DT", "JJ", "NN", "IN", "NNS", "VBG",
			"NN", "." };
//	private static final String TEST_LEMMAS = "plectranthus;barbatus;be;a;medicinal;plant;use;to;treat;a;wide;range;of;disorder;include;seizure;.";
	
	private static final String[] LEMMAS = {"plectranthus", "barbatus", "be", "a", "medicinal", "plant", "use", "to", "treat", 
			"a", "wide", "range", "of", "disorder", "include", "seizure", "."};
	
	private static final double trueValence = -1.5;
	private static final double trueArousal = 0.75;
	private static final double trueDominance = -1.5;
	private static final int trueEmotionalWordCount = 4;
	
	private static final double DELTA = 0.001;

	public JCas getJCas() throws UIMAException {
		JCas aJCas = JCasFactory.createJCas();
		aJCas.setDocumentText(TEST_SENTENCES);

		Sentence sent = new Sentence(aJCas);
		sent.setBegin(0);
		sent.setEnd(101);
		sent.addToIndexes();

		for (int i = 0; i < tokenBegins.length; i++) {
			Token tok = new Token(aJCas);
			tok.setBegin(tokenBegins[i]);
			tok.setEnd(tokenEnds[i]);
			FSArray posArray = new FSArray(aJCas, 1);
			PennBioIEPOSTag posTag = new PennBioIEPOSTag(aJCas);
			posTag.setValue(posTags[i]);
			posArray.set(0, posTag);
			tok.setPosTag(posArray);
			//insert lemma information to CAS
			Lemma lemma = new Lemma(aJCas);
			lemma.setBegin(tok.getBegin());
			lemma.setEnd(tok.getEnd());
			lemma.setValue(LEMMAS[i]);
			tok.setLemma(lemma);
			tok.addToIndexes();
		}
		return aJCas;
	}
	
	private   LexicalDocumentEmotion getEmotion(JCas jcas) {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator it = indexes.getAnnotationIndex(LexicalDocumentEmotion.type).iterator();
		LexicalDocumentEmotion emo = (LexicalDocumentEmotion) it.next();
		return emo;
	}
	
	

	@SuppressWarnings("rawtypes")
	public void testProcess() throws Exception {
		
		JCas aJCas = getJCas();
		
		//************************* test with test lexicon *************************//
		AnalysisEngine emotionAnalyzer = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jemas.desc.jcore-jemas-ae",
				"lexiconPath", "src/test/resources/de/julielab/jcore/ae/jemas/lexicons/test-lexicon.vad");
		emotionAnalyzer.process(aJCas);
		//
		
	    LexicalDocumentEmotion emo = getEmotion(aJCas);
		
		Double predValence = emo.getValence();
		Double predArousal = emo.getArousal();
		Double predDominance = emo.getDominance();
		LOGGER.debug("Testing with small test lexicon");
		LOGGER.debug("testProcess() - predicted: " + predValence + ", " + predArousal + ", " + predDominance + ", " +emo.getEmotionalWordCount());
		LOGGER.debug("testProcess() -    wanted: " + trueValence + ", " + trueArousal + ", " + trueDominance + ", " + trueEmotionalWordCount);
		assertEquals(trueValence, predValence, DELTA);
		assertEquals(trueArousal, predArousal, DELTA);
		assertEquals(trueDominance, predDominance, DELTA);
		assertEquals(trueEmotionalWordCount, emo.getEmotionalWordCount());
		emotionAnalyzer.destroy();
		
		
		//************************* test with warriner lexicon (default settings) *************************//
		aJCas = getJCas();
		emotionAnalyzer = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jemas.desc.jcore-jemas-ae");
		emotionAnalyzer.process(aJCas);
	
		emo = getEmotion(aJCas);
		
		predValence = emo.getValence();
		predArousal = emo.getArousal();
		predDominance = emo.getDominance();
		LOGGER.debug("Testing with full scale default lexicon (Warriner)");
		LOGGER.debug("testProcess() - predicted: " + predValence + ", " + predArousal + ", " + predDominance + ", " +emo.getEmotionalWordCount());
		
	}

	
	
	
}
