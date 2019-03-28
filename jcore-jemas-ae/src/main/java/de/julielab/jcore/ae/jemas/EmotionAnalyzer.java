package de.julielab.jcore.ae.jemas;

import de.julielab.jcore.types.LexicalDocumentEmotion;
import de.julielab.jcore.types.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmotionAnalyzer extends JCasAnnotator_ImplBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmotionAnalyzer.class);
	private EmotionLexicon lexicon;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		String lexiconPath = (String) getContext().getConfigParameterValue("lexiconPath");
		// System.err.println();
		// System.err.println(lexiconPath);
		// System.err.println();
		lexicon = new EmotionLexicon(lexiconPath);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		LOGGER.debug("[JCoRe JEmAS] processing document ...");
		Emotion docEmo = new Emotion(0.0, 0.0, 0.0);
		FSIterator tokenIterator = aJCas.getAnnotationIndex(Token.type).iterator();
		int count = 0; // counts how many words could have been found in lexicon

		while (tokenIterator.hasNext()) {
			Token token = (Token) tokenIterator.next();

			String lemma = token.getLemma().getValue();
			Emotion lemmaEmo = lexicon.get(lemma);
			if (lemmaEmo != null) {
				count++;
//				// debug
//				System.err.println(count);
				docEmo.add(lemmaEmo);
//				// debug
//				System.err.println(lemma);
//				System.err.println(lemmaEmo.toString());
			}
		}
		docEmo.normalize(count);
		LexicalDocumentEmotion documentEmotion = new LexicalDocumentEmotion(aJCas);
		documentEmotion.setValence(docEmo.getValence());
		documentEmotion.setArousal(docEmo.getArousal());
		documentEmotion.setDominance(docEmo.getDominance());
		documentEmotion.setEmotionalWordCount(count);
		documentEmotion.addToIndexes();


	}
}
