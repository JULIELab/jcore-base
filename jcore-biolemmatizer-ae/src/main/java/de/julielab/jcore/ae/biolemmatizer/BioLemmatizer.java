package de.julielab.jcore.ae.biolemmatizer;

import de.julielab.jcore.types.Lemma;
import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Token;
import edu.ucdenver.ccp.nlp.biolemmatizer.LemmataEntry;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class BioLemmatizer extends JCasAnnotator_ImplBase {
    private final static Logger log = LoggerFactory.getLogger(BioLemmatizer.class);
    public static edu.ucdenver.ccp.nlp.biolemmatizer.BioLemmatizer bioLemm;

//	private final static Logger log = LoggerFactory.getLogger(BioLemmatizer.class);

    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        bioLemm = new edu.ucdenver.ccp.nlp.biolemmatizer.BioLemmatizer();
    }

    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        String tokenString = null;
        String tag = null;
        try {
            AnnotationIndex<Annotation> jcoreTokenIndex = aJCas.getAnnotationIndex(Token.type);
            FSIterator<Annotation> tokenIterator = jcoreTokenIndex.iterator();
            while (tokenIterator.hasNext()) {
                Token token = (Token) tokenIterator.get();
                tokenString = token.getCoveredText();
                LemmataEntry lemmaEntry = null;
                try {
                    if (token.getPosTag() != null) {
                        POSTag posTag = token.getPosTag(0);
                        tag = posTag.getValue();
                        lemmaEntry = bioLemm.lemmatizeByLexiconAndRules(tokenString, tag);
                    } else {
                        lemmaEntry = bioLemm.lemmatizeByLexiconAndRules(tokenString, "");
                    }
                } catch (java.lang.IllegalStateException | java.lang.IndexOutOfBoundsException e) {
                    // These two exceptions happen all the time due to internal stuff of the lemmatizer; log and ignore
                    log.debug("BioLemmatizer-internal exception: ", e);
                }
                if (lemmaEntry != null) {
                    Collection<LemmataEntry.Lemma> lemmaCollection = lemmaEntry.getLemmas();
                    LemmataEntry.Lemma lemma = lemmaCollection.iterator().next();
                    String lem = lemma.getLemma();
                    Lemma jcoreLemma = new Lemma(aJCas, token.getBegin(), token.getEnd());
                    jcoreLemma.setValue(lem);
                    token.setLemma(jcoreLemma);
                } else {
                    Lemma jcoreLemma = new Lemma(aJCas, token.getBegin(), token.getEnd());
                    jcoreLemma.setValue(tokenString);
                    token.setLemma(jcoreLemma);
                }
                tokenIterator.next();
            }
        } catch (Exception e) {
            log.error("Exception occurred:", e);
        }
    }

}
