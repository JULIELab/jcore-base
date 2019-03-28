
package de.julielab.jcore.ae.likelihooddetection;


import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.Token;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

@ResourceMetaData(name="JCoRe Likelihood Detection AE", description = "Analysis Engine to detect epistemic modal expressions and assign the appropriate likelihood category.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Token", "de.julielab.jcore.types.Lemma"}, outputs = "de.julielab.jcore.types.LikelihoodIndicator")
public class LikelihoodDetectionAnnotator extends JCasAnnotator_ImplBase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(LikelihoodDetectionAnnotator.class);

	/**
	 * String parameter indicating path to likelihood dictionary (One entry per
	 * line; Entries consist of tab-separated lemmatized likelihood indicators
	 * and assigned likelihood category)
	 */
	public static final String PARAM_LIKELIHOOD_DICT_PATH = "LikelihoodDict";

	/**
	 * The simple dictionary of likelihood indicators; Maps lemmatized
	 * likelihood indicators - in this case expressions comprising one word only
	 * - to assigned likelihood categories
	 */
	private HashMap<String, String> likelihoodDictSimple = new HashMap<String, String>();
	/**
	 * The complex dictionary of likelihood indicators; Maps lemmatized
	 * likelihood indicators - in this case expressions comprising multiple
	 * words - to assigned likelihood categories
	 */
	private ArrayList<Pair<String[], String>> likelihoodDictComplex = new ArrayList<Pair<String[], String>>();

	@ConfigurationParameter(name=PARAM_LIKELIHOOD_DICT_PATH, description = "String parameter indicating path to likelihood dictionary (One entry per " +
			"line; Entries consist of tab-separated lemmatized likelihood indicators " +
			"and assigned likelihood category)")
	private String dictFile;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		dictFile = (String) aContext
				.getConfigParameterValue(PARAM_LIKELIHOOD_DICT_PATH);
        try {
            loadLikelihoodDict(dictFile);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		detectLikelihood(aJCas);
	}

	private void loadLikelihoodDict(String dictFilePath) throws IOException {
        InputStream resource = FileUtilities.findResource(dictFilePath);
		if (resource == null) {
			LOGGER.error("ERR: Could not find likelihood dictionary file (path: "
							+ dictFilePath + ")");
            throw new IllegalArgumentException("Could not find likelihood dictionary at " + dictFilePath);
		}

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource))) {
			String line = "";

			while ((line = reader.readLine()) != null) {
				String[] entry = line.split("\t");

				if (entry.length != 2) {
					LOGGER.error("ERR: Likelihood dictionary file not in expected format. Critical line: "
									+ line);
                    throw new IllegalArgumentException("Likelihood dictionary has the wrong format (expected: two tab-separated columns). Critical line: " + line);
				}

				String indicator = entry[0].trim();
				String category = entry[1].trim();
				if (!indicator.contains(" ")) {
					likelihoodDictSimple.put(indicator, category);
				} else {
					String[] indicatorArray = indicator.split(" ");
					likelihoodDictComplex
							.add(Pair.of(indicatorArray, category));
				}
			}

			reader.close();
			LOGGER.info("Done loading likelihood dictionary.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void detectLikelihood(JCas aJCas) {
		FSIterator tokenIterator = aJCas.getAnnotationIndex(Token.type)
				.iterator();

		while (tokenIterator.hasNext()) {
			Token token = (Token) tokenIterator.next();
			Lemma lemma = token.getLemma();
			if (null == lemma)
				continue;
			String lemmaStr = lemma.getValue();
			LOGGER.debug("Current lemma: " + lemmaStr);
			boolean isIndicator = false;
			int begin = 0;
			int end = 0;
			String category = "";

			// look for expressions comprising one word only
			if (likelihoodDictSimple.get(lemmaStr) != null) {
				isIndicator = true;
				LOGGER.debug("Exact match: " + lemmaStr);
				begin = token.getBegin();
				end = token.getEnd();
				category = likelihoodDictSimple.get(lemmaStr);
				// look for expressions comprising multiple words
			} else {
				for (Pair<String[], String> entry : likelihoodDictComplex) {
					String[] entryLemmas = entry.getLeft();

					// look for a match on the first word of the expression
					if (entryLemmas[0].equals(lemmaStr)) {
						isIndicator = true;
						LOGGER.debug("First partial match: "
								+ entryLemmas[0]);
						begin = token.getBegin();
						category = entry.getRight();

						// see if the rest of the expression can be matched as
						// well; remember the number of next tokens we look at, so
						// that if there is no complete match we can move back to our
						// current position
						int numNextTokens = 0;
						for (int i = 1; i < entryLemmas.length && tokenIterator.isValid(); i++) {
							Token nextToken = (Token) tokenIterator.get();
							if (nextToken.getLemma() == null)
								continue;
							String nextLemmaStr = nextToken.getLemma()
									.getValue();
							if (entryLemmas[i].equals(nextLemmaStr)) {
								end = nextToken.getEnd();
								LOGGER.debug("Next partial match: "
										+ entryLemmas[i]);
								tokenIterator.moveToNext();
								numNextTokens++;
							} else {
								isIndicator = false;
								LOGGER.debug("Matching aborted at: "
										+ nextLemmaStr + " (Expected: "
										+ entryLemmas[i] + ")");
								for (int j = 0; j < numNextTokens; j++) {
									tokenIterator.moveToPrevious();
								}
								break;
							}
						}
						if (isIndicator == true) {
							break;
						}
					}
				}
			}

			if (isIndicator == true) {
				LikelihoodIndicator indicator = new LikelihoodIndicator(aJCas);
				indicator.setBegin(begin);
				indicator.setEnd(end);
				indicator.setLikelihood(category);
				indicator.setComponentId(this.getClass().getName());
				indicator.addToIndexes(aJCas);
			}
		}
	}
}
