package de.julielab.jcore.ae.lingscope;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.*;
import lingscope.algorithms.Annotator;
import lingscope.drivers.CueAndPosFilesMerger;
import lingscope.drivers.SentenceTagger;
import lingscope.structures.AnnotatedSentence;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ResourceMetaData(name = "JCoRe Lingscope AE", description = "This component uses the Lingscope negation/hedge detection algorithm and models to annotate negation/hedge cues and the scope to which the cues apply.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Token", "de.julielab.jcore.types.PennBioIEPOSTag"}, outputs = {"de.julielab.jcore.types.LikelihoodIndicator", "de.julielab.jcore.types.Scope"})
public class LingscopePosAnnotator extends JCasAnnotator_ImplBase {
    public static final String PARAM_CUE_MODEL = "CueModel";
    public static final String PARAM_SCOPE_MODEL = "ScopeModel";
    /**
     * String parameter indicating path to likelihood dictionary (One entry per
     * line; Entries consist of tab-separated lemmatized likelihood indicators
     * and assigned likelihood category)
     */
    public static final String PARAM_LIKELIHOOD_DICT_PATH = "LikelihoodDict";
    public static final String PARAM_IS_NEGATION_ANNOTATOR = "IsNegationAnnotator";
    private final static Logger log = LoggerFactory.getLogger(LingscopePosAnnotator.class);
    private Annotator cueAnnotator;
    private Annotator scopeAnnotator;
    /**
     * Maps lemmatized likelihood indicators to assigned likelihood categories
     */
    private Map<String, String> likelihoodDict = new HashMap<>();
    @ConfigurationParameter(name = PARAM_CUE_MODEL, description = "The model that is used to recognize the negation or hedge cue words in text. There are different models for negation and hedge detection in Lingscope, indicated by the directory names 'negation_models' and 'hedge_models' in the respective downloads from the Lingscope SourceForge page. The cue detection models are always those where the string 'cue' follows the 'baseline' or 'crf' string in the filename. Thus, all 'baseline_cue_*' and 'crf_cue_*' files are cue identification models. The 'crf_scope_cue_*' models, in contrast, are scope detection models that replace the cue words by the string CUE.")
    private String cueModelLocation;

    @ConfigurationParameter(name = PARAM_SCOPE_MODEL, description = "The model that is used to detect the scope of a previously found negation or hedge cue word. There are different models for negation and hedge detection in Lingscope, indicated by the directory names 'negation_models' and 'hedge_models' in the respective downloads from the Lingscope SourceForge page. The cue detection models are always those where the string 'cue' follows the 'baseline' or 'crf' string in the filename. Thus, all 'baseline_cue_*' and 'crf_cue_*' files are cue identification models. The 'crf_scope_cue_*' models, in contrast, are scope detection models that replace the cue words by the string CUE.")
    private String scopeModelLocation;

    @ConfigurationParameter(name = PARAM_LIKELIHOOD_DICT_PATH, mandatory = false, description = "String parameter indicating path to likelihood dictionary (One entry per line; Entries consist of tab-separated lemmatized likelihood indicators and assigned likelihood category). The dictionary passed here is only used to assign likelihood scores (low, medium, high) to negation and hedge cues. It is not used to detect the cues in the first place.")
    private String likelihoodDictFile;

    @ConfigurationParameter(name = PARAM_IS_NEGATION_ANNOTATOR, mandatory = false, defaultValue = "false", description = "If set to true, the recognized cue words will all be assigned the 'negation' likelihood, even if the model used is a hedge model.")
    private boolean isNegationAnnotator;

    private boolean replaceCue;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        cueModelLocation = (String) aContext.getConfigParameterValue(PARAM_CUE_MODEL);
        scopeModelLocation = (String) aContext.getConfigParameterValue(PARAM_SCOPE_MODEL);
        Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_LIKELIHOOD_DICT_PATH)).ifPresent(path -> LikelihoodUtils.loadLikelihoodDict(path, likelihoodDict));
        Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_IS_NEGATION_ANNOTATOR)).ifPresent(b -> isNegationAnnotator = b);

        String cueModelType;
        // We do not expect this to be an actual file (while it could be!). We just want to retrieve the name of the resource without the path
        File cueModelFile = new File(cueModelLocation);
        if (cueModelFile.getName().startsWith("baseline"))
            cueModelType = "baseline";
        else if (cueModelFile.getName().startsWith("crf"))
            cueModelType = "crf";
        else
            cueModelType = "negex";
        log.info("Inferred the cue detection type '{}' from the cue model file '{}'", cueModelType, cueModelLocation);

        replaceCue = !scopeModelLocation.contains("words");
        log.info("Inferred the strategy as to whether to replace found cue words with the CUE string or not from the scope model file '{}' to: Replace: {}", scopeModelLocation, replaceCue);

        // While there seems to be a "baseline" scope annotator and also negex, only crf seems to actually work for
        // the POS algorithm
        String scopeModelType = "crf";
        try {
            cueAnnotator = SentenceTagger.getAnnotator(cueModelType, "cue");
            cueAnnotator.loadAnnotator(FileUtilities.findResource(cueModelLocation));
            scopeAnnotator = SentenceTagger.getAnnotator(scopeModelType, "scope");
            scopeAnnotator.loadAnnotator(FileUtilities.findResource(scopeModelLocation));
        } catch (IOException e) {
            log.error("Could not initialize Lingscope annotators", e);
            throw new ResourceInitializationException(e);
        }
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        final AnnotationIndex<Token> tokIt = aJCas.getAnnotationIndex(Token.type);
        final FSIterator<Annotation> sentIt = aJCas.getAnnotationIndex(Sentence.type).iterator();
        while (sentIt.hasNext()) {
            Annotation sent = sentIt.next();
            final FSIterator<Token> subiterator = tokIt.subiterator(sent);
            StringBuilder sb = new StringBuilder();
            List<Token> tokens = new ArrayList<>();
            while (subiterator.hasNext()) {
                Token token = subiterator.next();
                final POSTag posTag = token.getPosTag(0);
                if (posTag == null)
                    throw new AnalysisEngineProcessException(new IllegalArgumentException("PoS tags are required but the current token has none."));
                sb.append(posTag.getValue()).append(" ");
                tokens.add(token);
            }
            if (sb.length() > 0) {
                // Remove the trailing whitespace
                sb.deleteCharAt(sb.length() - 1);

                String posSentence = sb.toString();
                AnnotatedSentence cueTaggedSentence = null;
                AnnotatedSentence posCueMerged = null;
                AnnotatedSentence scopeMarkedSentence = null;
                try {
                    // Important step here: replace pipes through slashes. Pipes are a reserved character for the internal tag representation format.
                    cueTaggedSentence = cueAnnotator.annotateSentence(tokens.stream().map(Annotation::getCoveredText).collect(Collectors.joining(" ")).replace("|", "/"), true);
                    posCueMerged = CueAndPosFilesMerger.merge(cueTaggedSentence, posSentence, replaceCue);
                    scopeMarkedSentence = scopeAnnotator.annotateSentence(posCueMerged.getSentenceText(), true);

                    final List<LikelihoodIndicator> likelihoodIndicators = addAnnotationToCas(tokens, cueTaggedSentence, () -> new LikelihoodIndicator(aJCas));
                    final List<Scope> scopes = addAnnotationToCas(tokens, scopeMarkedSentence, () -> new Scope(aJCas));

                    if (likelihoodIndicators.size() == scopes.size()) {
                        for (int i = 0; i < scopes.size(); i++) {
                            LikelihoodIndicator indicator = likelihoodIndicators.get(i);
                            Scope scope = scopes.get(i);
                            scope.setCue(indicator);
                        }
                    } else {
                        log.debug("Not assigning negation or hedge cues to their scopes because the number of cues and scopes differs.");
                        log.trace("The respective sentence is: '{}'. Cue tags: '{}', Scope tags: '{}'", sent.getCoveredText(), cueTaggedSentence.getTags(), scopeMarkedSentence.getTags());
                    }
                } catch (Throwable t) {
                    log.error("Lingscope error in sentence '{}'", sent.getCoveredText(), t);
                    log.error("PosCueMerged Sent Text: {}", posCueMerged != null ? posCueMerged.getSentenceText() : "<null>");
                    log.error("Tokens: {}", tokens.stream().map(Annotation::getCoveredText).collect(Collectors.joining(" ")));
                    log.error("Lemmas: {}", tokens.stream().map(Token::getLemma).map(Lemma::getValue).collect(Collectors.joining(" ")));
                    log.error("PoS: {}", posSentence);
                    log.error("Cue tags: {}", cueTaggedSentence != null ? cueTaggedSentence.getTags() : "<null>");
                    log.error("POS Cue merged: {}", posCueMerged != null ? posCueMerged.getTags() : "<null>");
                    log.error("Scope tags: {}", scopeMarkedSentence != null ? scopeMarkedSentence.getTags() : "<null>");
                    log.error("StackTrace:", t);
                    throw t;
                }
            }
        }
    }

    /**
     * This method is used to add the cues as well as the scopes to the CAS. The <tt>annotationSupplier</tt> is
     * responsible for providing the required annotation. This algorithm just checks for tags starting with 'B'(egin)
     * or being O(utside). All spans from B to another B or O are stored in one annotation.
     *
     * @param tokens             The PoS tags of the current sentence, used to derive the annotation offsets.
     * @param taggedSentence     The sentence tagged for cue words or scopes
     * @param annotationSupplier A supplier for the annotation type to create.
     * @param <T>                The cue or scope type to use
     * @return The created annotations from the <tt>taggedSentence</tt>.
     */
    private <T extends Annotation> List<T> addAnnotationToCas(List<Token> tokens, AnnotatedSentence taggedSentence, Supplier<T> annotationSupplier) throws AnalysisEngineProcessException {
        List<T> allIndicators = new ArrayList<>();
        T annotation = null;
        List<Token> tokensInCurrentExpression = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            final String tag = taggedSentence.getTags().get(i);
            if (tag.startsWith("B")) {
                if (annotation != null) {
                    endAnnotation(tokens, allIndicators, annotation, tokensInCurrentExpression, i);
                }
                annotation = annotationSupplier.get();
                annotation.setBegin(token.getBegin());
                tokensInCurrentExpression.add(token);
            }
            if (tag.startsWith("I")) {
                tokensInCurrentExpression.add(token);
            }
            if (tag.equals("O")) {
                if (annotation != null) {
                    endAnnotation(tokens, allIndicators, annotation, tokensInCurrentExpression, i);
                }
                annotation = null;
            }
        }
        return allIndicators;
    }

    private <T extends Annotation> void endAnnotation(List<Token> tokens, List<T> allIndicators, T annotation, List<Token> tokensInCurrentExpression, int i) throws AnalysisEngineProcessException {
        annotation.setEnd(tokens.get(i - 1).getEnd());
        // If this is a negation/hedge cue, set its likelihood, if possible
        if (annotation instanceof LikelihoodIndicator) {
            if (!likelihoodDict.isEmpty() && !isNegationAnnotator) {
                StringBuilder lemmaExpressionBuilder = new StringBuilder();
                for (Token t : tokensInCurrentExpression) {
                    final Lemma lemma = t.getLemma();
                    if (lemma == null)
                        throw new AnalysisEngineProcessException(new IllegalArgumentException("Lemmas are required when a likelihood dictionary is passed but the current token has none."));
                    final String lemmaValue = lemma.getValue();
                    lemmaExpressionBuilder.append(lemmaValue.toLowerCase()).append(" ");
                }
                // Prepare for the next expression, we don't need it any more
                tokensInCurrentExpression.clear();
                // Remove trailing whitespace
                lemmaExpressionBuilder.deleteCharAt(lemmaExpressionBuilder.length() - 1);
                String lemmatizedLikelihoodExpression = lemmaExpressionBuilder.toString();
                final String likelihoodScore = likelihoodDict.get(lemmatizedLikelihoodExpression);
                if (likelihoodScore != null) {
                    LikelihoodIndicator indicator = (LikelihoodIndicator) annotation;
                    indicator.setLikelihood(likelihoodScore);
                }
            } else if (isNegationAnnotator) {
                LikelihoodIndicator indicator = (LikelihoodIndicator) annotation;
                indicator.setLikelihood("negation");
            }
        }
        annotation.addToIndexes();
        allIndicators.add(annotation);
    }
}
