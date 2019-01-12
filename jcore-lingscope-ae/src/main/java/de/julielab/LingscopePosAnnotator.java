package de.julielab;

import de.julielab.jcore.types.LikelihoodIndicator;
import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import lingscope.algorithms.Annotator;
import lingscope.drivers.AnnotatedFilesMerger;
import lingscope.drivers.CueAndPosFilesMerger;
import lingscope.drivers.SentenceTagger;
import lingscope.structures.AnnotatedSentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LingscopePosAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_CUE_MODEL = "CueModel";
    public static final String PARAM_SCOPE_MODEL = "ScopeModel";
    private final static Logger log = LoggerFactory.getLogger(LingscopePosAnnotator.class);
    private Annotator cueAnnotator;
    private Annotator scopeAnnotator;
    @ConfigurationParameter(name = PARAM_CUE_MODEL, description = "The model that is used to recognize the negation or hedge cue words in text. There are different models for negation and hedge detection in Lingscope, indicated by the directory names 'negation_models' and 'hedge_models' in the respective downloads from the Lingscope SourceForge page. The cue detection models are always those where the string 'cue' follows the 'baseline' or 'crf' string in the filename. Thus, all 'baseline_cue_*' and 'crf_cue_*' files are cue identification models. The 'crf_scope_cue_*' models, in contrast, are scope detection models that replace the cue words by the string CUE.")
    private File cueModelFile;

    @ConfigurationParameter(name = PARAM_SCOPE_MODEL, description = "The model that is used to detect the scope of a previously found negation or hedge cue word. There are different models for negation and hedge detection in Lingscope, indicated by the directory names 'negation_models' and 'hedge_models' in the respective downloads from the Lingscope SourceForge page. The cue detection models are always those where the string 'cue' follows the 'baseline' or 'crf' string in the filename. Thus, all 'baseline_cue_*' and 'crf_cue_*' files are cue identification models. The 'crf_scope_cue_*' models, in contrast, are scope detection models that replace the cue words by the string CUE.")
    private File scopeModelFile;
    private boolean replaceCue;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        cueModelFile = new File((String) aContext.getConfigParameterValue(PARAM_CUE_MODEL));
        scopeModelFile = new File((String) aContext.getConfigParameterValue(PARAM_SCOPE_MODEL));

        String cueModelType;
        if (cueModelFile.getName().startsWith("baseline"))
            cueModelType = "baseline";
        else if (cueModelFile.getName().startsWith("crf"))
            cueModelType = "crf";
        else
            cueModelType = "negex";
        log.info("Inferred the cue detection type '{}' from the cue model file '{}'", cueModelType, cueModelFile.getName());

        replaceCue = !scopeModelFile.getName().contains("words");
        log.info("Inferred the strategy as to whether to replace found cue words with the CUE string or not from the scope model file '{}' to: Replace: {}", scopeModelFile.getName(), replaceCue);

        // While there seems to be a "baseline" scope annotator and also negex, only crf seems to actually work for
        // the POS algorithm
        String scopeModelType = "crf";
        cueAnnotator = SentenceTagger.getAnnotator(cueModelType, "cue");
        cueAnnotator.loadAnnotator(cueModelFile.getAbsolutePath());
        scopeAnnotator = SentenceTagger.getAnnotator(scopeModelType, "scope");
        scopeAnnotator.loadAnnotator(scopeModelFile.getAbsolutePath());
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        final AnnotationIndex<PennBioIEPOSTag> posIt = aJCas.getAnnotationIndex(PennBioIEPOSTag.class);
        final FSIterator<Annotation> sentIt = aJCas.getAnnotationIndex(Sentence.type).iterator();
        while (sentIt.hasNext()) {
            Annotation sent = sentIt.next();
            final FSIterator<PennBioIEPOSTag> subiterator = posIt.subiterator(sent);
            StringBuilder sb = new StringBuilder();
            List<PennBioIEPOSTag> posTags = new ArrayList<>();
            while (subiterator.hasNext()) {
                PennBioIEPOSTag pos = subiterator.next();
                sb.append(pos.getValue()).append(" ");
            }
            // Remove the trailing whitespace
            sb.deleteCharAt(sb.length() - 1);

            String posSentence = sb.toString();
            AnnotatedSentence cueTaggedSentence = cueAnnotator.annotateSentence(sent.getCoveredText(), false);
            AnnotatedSentence posCueMerged = CueAndPosFilesMerger.merge(cueTaggedSentence, posSentence, replaceCue);
            AnnotatedSentence scopeMarkedSentence = scopeAnnotator.annotateSentence(posCueMerged.getSentenceText(), true);
            AnnotatedSentence scopeWordsMarkedSentence = AnnotatedFilesMerger.merge(cueTaggedSentence, scopeMarkedSentence);
            System.out.println(scopeWordsMarkedSentence.getTags());
            System.out.println(cueTaggedSentence.getTags());

            LikelihoodIndicator indicator = null;
            for (int i = 0; i < posTags.size(); i++) {
                PennBioIEPOSTag posTag = posTags.get(i);
                final String tag = cueTaggedSentence.getTags().get(i);
                if (tag.startsWith("B")) {
                    if (indicator != null) {
                        indicator.setEnd(posTags.get(i - 1).getEnd());
                        indicator.addToIndexes();
                    }
                    indicator = new LikelihoodIndicator(aJCas);
                    indicator.setBegin(posTag.getBegin());
                }
                if (tag.equals("O")) {
                    if (indicator != null) {
                        indicator.setEnd(posTags.get(i - 1).getEnd());
                        indicator.addToIndexes();
                    }
                    indicator = null;
                }
            }


        }
    }
}
