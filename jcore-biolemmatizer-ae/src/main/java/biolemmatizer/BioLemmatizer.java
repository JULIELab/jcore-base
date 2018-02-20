
package biolemmatizer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Token;
import edu.ucdenver.ccp.nlp.biolemmatizer.LemmataEntry;
import edu.ucdenver.ccp.nlp.biolemmatizer.LemmataEntry.Lemma;

public class BioLemmatizer extends JCasAnnotator_ImplBase {
	
	public static edu.ucdenver.ccp.nlp.biolemmatizer.BioLemmatizer bioLemm;
	
	private final static Logger log = LoggerFactory.getLogger(BioLemmatizer.class);
	
	/**
	 * This method is called a single time by the framework at component
	 * creation. Here, descriptor parameters are read and initial setup is done.
	 */
	@Override
	public void initialize(final UimaContext aContext) throws ResourceInitializationException {
		bioLemm = new edu.ucdenver.ccp.nlp.biolemmatizer.BioLemmatizer();
	}

	/**
	 * This method is called for each document going through the component. This
	 * is where the actual work happens.
	 */
	@Override
	public void process(final JCas aJCas) throws AnalysisEngineProcessException {
		try {
			AnnotationIndex<Annotation> jcoreTokenIndex = aJCas.getAnnotationIndex(Token.type);
			FSIterator<Annotation> tokenIterator = jcoreTokenIndex.iterator();
			while (tokenIterator.hasNext()) {
				Annotation annot = tokenIterator.get();
				String token = annot.getCoveredText();
				
				TypeSystem typeSystem = aJCas.getTypeSystem();
				Feature posFeat = typeSystem.getFeatureByFullName("de.julielab.jcore.types.POSTag");
				Feature lemmaFeat = typeSystem.getFeatureByFullName("de.julielab.jcore.types.Lemma");
				String tag = annot.getFeatureValueAsString(posFeat);
				
				LemmataEntry lemmaEntry = bioLemm.lemmatizeByLexicon(token, tag);
				Collection<Lemma> lemmaCollection = lemmaEntry.getLemmas();
				Lemma lemma = lemmaCollection.iterator().next();
				String lem = lemma.getLemma();
				annot.setStringValue(lemmaFeat, lem);
				tokenIterator.next();

//				Type jcoreToken = annot.getType(); 
//				List<Feature> features = jcoreToken.getFeatures();
//				Iterator<Feature> featureIterator = features.iterator();
//				LemmataEntry lemmaEntry = bioLemm.lemmatizeByLexicon(token, "");
//				Collection<Lemma> lemmaCollection = lemmaEntry.getLemmas();
//				Lemma lemma = lemmaCollection.iterator().next();
//				String lemmata = lemmaEntry.lemmasToString();
//				while (featureIterator.hasNext()) {
//					Feature feature = featureIterator.next();
//					if (feature.getName().contentEquals("de.julielab.jcore.types.Token:posTag[]")) {
//					if (feature.getName().equalsIgnoreCase("posTag")) {
//						String postag = annot.getStringValue(feature);
//						LemmataEntry lemmaEntry = bioLemm.lemmatizeByLexicon(token, postag);
//						Collection<Lemma> lemmaCollection = lemmaEntry.getLemmas();
//						Lemma lemma = lemmaCollection.iterator().next();
//						String lem = lemma.getLemma();
//						String lemmata = lemmaEntry.lemmasToString();
//						if (feature.getName().contentEquals("de.julielab.jcore.types.Token:Lemma")) {
//						if (feature.getShortName().equalsIgnoreCase("lemma")) {
//							annot.setStringValue(feature, lem);
//						}
//					}
//					if (feature.getName().contentEquals("de.julielab.jcore.types.Token:Lemma")) {
//					if (feature.getShortName().equalsIgnoreCase("lemma")) {
//						LemmataEntry lemmaEntry = bioLemm.lemmatizeByLexicon(token, "");
//						Collection<Lemma> lemmaCollection = lemmaEntry.getLemmas();
//						Lemma lemma = lemmaCollection.iterator().next();
//						String lem = lemma.getLemma();
//						String lemmata = lemmaEntry.lemmasToString();
//						annot.setStringValue(feature, lem);
//						de.julielab.jcore.types.Lemma jcoreLemma;
//						
//						
//					}
//				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
