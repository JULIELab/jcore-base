/**
 * SentenceAnnotator.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: tomanek
 * <p>
 * Current version: 2.3
 * Since version:   1.0
 * <p>
 * Creation date: Nov 29, 2006
 * <p>
 * This is a wrapper to the JULIE Sentence Boundary Detector (JSBD).
 * It splits a text into single sentences and adds annotations of
 * the type Sentence to the respective UIMA (J)Cas.
 **/

package de.julielab.jcore.ae.jsbd.main;

import de.julielab.jcore.ae.jsbd.SentenceSplitter;
import de.julielab.jcore.ae.jsbd.Unit;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.utility.JCoReAnnotationIndexMerger;
import de.julielab.jcore.utility.JCoReCondensedDocumentText;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class SentenceAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_MODEL_FILE = "ModelFilename";
    public static final String PARAM_POSTPROCESSING = "Postprocessing";
    public static final String PARAM_SENTENCE_DELIMITER_TYPES = "SentenceDelimiterTypes";
    public static final String PARAM_CUT_AWAY_TYPES = "CutAwayTypes";
    public static final String PARAM_MAX_SENTENCE_LENGTH = "MaximumSentenceLength";
    public static final String PARAM_ALWAYS_SPLIT_NEWLINE = "AlwaysSplitAtNewlines";
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SentenceAnnotator.class);
    private static AtomicInteger numEmptyCases = new AtomicInteger();
    /**
     * Matches all letters (not numbers, punctuation etc.).
     */
    private final Matcher letterMatcher = Pattern.compile("\\p{L}\\p{M}*").matcher("");
    private final Matcher eolMatcher = Pattern.compile("(\\r\\n|\\r|\\n)").matcher("");
    private final Matcher semicoliMatcher = Pattern.compile(";").matcher("");
    private final Matcher wsMatcher = Pattern.compile(" ").matcher("");
    // activate post processing
    @ConfigurationParameter(name = PARAM_POSTPROCESSING, mandatory = false, defaultValue = {
            "false"}, description = "One of 'biomed' or 'medical'. Does some post processing to e.g. respect parenthesis and don't put a sentence boundary withing in a pair of opening and closing parenthesis.")
    private String postprocessingFilter = null;
    @ConfigurationParameter(name = PARAM_SENTENCE_DELIMITER_TYPES, mandatory = false, description = "An array of annotation types that should never begin or end within a sentence. For example, sentences should never reach out of a paragraph or a section heading.")
    private Set<String> sentenceDelimiterTypes;
    @ConfigurationParameter(name = PARAM_MODEL_FILE, mandatory = true)
    private String modelFilename;
    @ConfigurationParameter(name = PARAM_CUT_AWAY_TYPES, mandatory = false, description = "An array of fully qualified type names. Document text covered by annotations of these types will be ignored from sentence splitting. This means that sentence splitting happens as if the covered text of these annotations would not exist in the text. This helps for references, for example, which otherwise might confuse the sentence splitting. A post-processing step tries to extend sentences include such annotations if they appear directly after the sentence (e.g. references: '...as Smith et al. have shown.1 Further text follows...').")
    private Set<String> cutAwayTypes;
    @ConfigurationParameter(name = PARAM_MAX_SENTENCE_LENGTH, mandatory = false, description = "Optional. If given, this parameter defines the maximum length in characters any sentence will have. If the machine learning algorithm produces sentences exceeding the given maximum length, they will be split first by newline and, if necessary, also at semicoli. If there are still too large sentences then, they will be split at whitespaces to stay within the given bound. Defaults to 0 which means no maximum length.")
    private int maxSentenceLength;
    @ConfigurationParameter(name = PARAM_ALWAYS_SPLIT_NEWLINE, mandatory = false, description = "Optional. If true, newlines are also used as sentence boundaries.")
    private boolean alwaysSplitAtNewlines;
    private SentenceSplitter sentenceSplitter;

    /**
     * initiaziation of JSBD: load the model, set post processing
     *
     * @parm aContext the parameters in the descriptor
     */
    public void initialize(UimaContext aContext) throws ResourceInitializationException {

        // invoke default initialization
        super.initialize(aContext);

        try {
            // initialize sentenceSplitter
            sentenceSplitter = new SentenceSplitter();
            LOGGER.info("initializing JSBD Annotator ...");
            // Get configuration parameter values
            modelFilename = (String) aContext.getConfigParameterValue(PARAM_MODEL_FILE);

            InputStream modelIs;
            File modelFile = new File(modelFilename);
            if (modelFile.exists()) {
                modelIs = new FileInputStream(modelFile);
            } else {
                LOGGER.debug("File \"{}\" does not exist. Searching for the model as a classpath resource.",
                        modelFilename);
                modelIs = this.getClass()
                        .getResourceAsStream(modelFilename.startsWith("/") ? modelFilename : "/" + modelFilename);
                if (null == modelIs)
                    throw new IllegalArgumentException("The model file \"" + modelFilename
                            + "\" could be found neither in the file system nor in the classpath.");
            }
            sentenceSplitter.readModel(modelIs);

            // this parameter is not mandatory, so first check whether it is there
            Object pp = aContext.getConfigParameterValue(PARAM_POSTPROCESSING);
            if (pp != null) {
                postprocessingFilter = (String) pp;
            }

            String[] sentenceDelimiterTypesArray = (String[]) aContext
                    .getConfigParameterValue(PARAM_SENTENCE_DELIMITER_TYPES);
            if (null != sentenceDelimiterTypesArray)
                sentenceDelimiterTypes = new LinkedHashSet<>(Arrays.asList(sentenceDelimiterTypesArray));

            String[] ignoredTypesArray = (String[]) aContext.getConfigParameterValue(PARAM_CUT_AWAY_TYPES);
            if (null != ignoredTypesArray)
                cutAwayTypes = Stream.of(ignoredTypesArray).collect(toSet());
            maxSentenceLength = Optional.ofNullable((Integer) aContext.getConfigParameterValue(PARAM_MAX_SENTENCE_LENGTH)).orElse(0);
            alwaysSplitAtNewlines = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_ALWAYS_SPLIT_NEWLINE)).orElse(false);
        } catch (ClassNotFoundException | IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    /**
     * process method is in charge of doing the sentence splitting. If
     * processingScope is set, we iterate over Annotation objects of this type and
     * do the sentence splitting within this scope. Otherwise, the whole document
     * text is considered.
     *
     * @throws AnalysisEngineProcessException
     */
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        try {
            if (StringUtils.isBlank(aJCas.getDocumentText())) {
                final String docId = JCoReTools.getDocId(aJCas);
                LOGGER.warn("The document text of document {} is empty.", docId);
                final AnnotationIndex<Annotation> annotationIndex = aJCas.getAnnotationIndex();
                LOGGER.warn("All annotations in CAS:");
                for (Annotation a : annotationIndex) {
                    System.out.println(a);
                }
                return;
            }
            JCoReCondensedDocumentText documentText;
            try {
                // If there are no cut-away types, the document text will remain unchanged.
                documentText = new JCoReCondensedDocumentText(aJCas, cutAwayTypes, Set.of(','), true);
            } catch (ClassNotFoundException e1) {
                LOGGER.error("Could not create the text without annotations to be cut away in document {}", JCoReTools.getDocId(aJCas), e1);
                throw new AnalysisEngineProcessException(e1);
            }

            if (sentenceDelimiterTypes != null) {
                try {
                    // the index merger gives us access to all delimiter type
                    // indexes in one
                    JCoReAnnotationIndexMerger indexMerger = new JCoReAnnotationIndexMerger(sentenceDelimiterTypes, false,
                            null, aJCas);

                    // the idea: collect all start and end offsets of sentence
                    // delimiter annotations (sections, titles, captions, ...) in a
                    // list and sort ascending; then, perform sentence segmentation
                    // between every two adjacent offsets. This way, no sentence can
                    // cross any delimiter annotation border
                    List<Integer> borders = new ArrayList<>();
                    borders.add(0);
                    borders.add(documentText.getCondensedOffsetForOriginalOffset(aJCas.getDocumentText().length()));
                    while (indexMerger.incrementAnnotation()) {
                        Annotation a = (Annotation) indexMerger.getAnnotation();
                        // Here we convert the original offsets to the condensed offsets. If there are
                        // no cut-away types, the offsets will just remain unchanged. Otherwise we now
                        // have the borders of the condensed text passages associated with the sentence
                        // delimiter annotation.
                        borders.add(documentText.getCondensedOffsetForOriginalOffset(a.getBegin()));
                        borders.add(documentText.getCondensedOffsetForOriginalOffset(a.getEnd()));
                    }
                    borders.sort(null);

                    // now do sentence segmentation between annotation borders
                    for (int i = 1; i < borders.size(); ++i) {
                        int start = borders.get(i - 1);
                        int end = borders.get(i);

                        // skip leading whites spaces
                        while (start < end && (Character.isWhitespace(documentText.getCodensedText().charAt(start))))
                            ++start;

                        // get the string between the current annotation borders and recognized sentences
                        String textSpan = documentText.getCodensedText().substring(start, end);
                        if (!StringUtils.isBlank(textSpan))
                            doSegmentation(documentText, textSpan, start);
                    }

                } catch (ClassNotFoundException e) {
                    throw new AnalysisEngineProcessException(e);
                }
            } else {
                // sentence delimiter types are not given
                // if no processingScope set -> use documentText
                if (aJCas.getDocumentText() != null && aJCas.getDocumentText().length() > 0) {
                    doSegmentation(documentText, documentText.getCodensedText(), 0);
                } else {
                    if (numEmptyCases.get() < 10) {
                        LOGGER.debug("document text empty. Skipping this document.");
                        numEmptyCases.incrementAndGet();
                    } else if (numEmptyCases.get() == 10) {
                        LOGGER.warn("Encountered 10 documents with an empty text body. This message will not appear again " +
                                "to avoid scrolling in cases where this is expected.");
                    }

                }
            }
        } catch (Throwable t) {
            LOGGER.error("Could not perform sentence splitting of document {}", JCoReTools.getDocId(aJCas), t);
            throw t;
        }
    }

    private void doSegmentation(JCoReCondensedDocumentText documentText, String text, int offset) {
        List<String> lines = new ArrayList<>();
        lines.add(text);

        // make prediction
        List<Unit> units = sentenceSplitter.predict(lines, postprocessingFilter);

        // add to UIMA annotations
        addAnnotations(documentText, units, offset);
    }

    /**
     * Add all the sentences to CAS. Sentence is split into single units, for each
     * such unit we decide whether this unit is at the end of a sentence. If so,
     * this unit gets the label "EOS" (end-of-sentence).
     *
     * @param documentText the associated JCas
     * @param units        all sentence units as returned by JSBD
     * @param offset
     */
    private void addAnnotations(JCoReCondensedDocumentText documentText, List<Unit> units, int offset) {
        int start = 0;
        for (int i = 0; i < units.size(); i++) {
            Unit myUnit = units.get(i);
            String decision = units.get(i).label;

            if (start == -1) { // now a new sentence is starting
                start = myUnit.begin;
            }

            if (decision.equals("EOS") || (i == units.size() - 1)) {
                // end-of-sentence predicted (EOS)
                // or last unit reached (finish a last sentence here!)
                Sentence annotation = new Sentence(documentText.getCas());
                int begin = documentText.getOriginalOffsetForCondensedOffset(start + offset);
                int end = documentText.getOriginalOffsetForCondensedOffset(myUnit.end + offset);
                // Adjust offsets to exclude leading or trailing white spaces
                begin = adjustBeginOffsetForWhitespaces(begin, documentText);
                end = adjustEndOffsetForWhitespaces(end, documentText);
                if (begin < end) {
                    annotation.setBegin(begin);
                    annotation.setEnd(end);
                    annotation.setComponentId(this.getClass().getName());
                    // Only add the sentence if the text actually looks like a sentence!
                    // At the moment this means that at least one letter must be included.
                    try {
                        letterMatcher.reset(annotation.getCoveredText());
                    } catch (java.lang.StringIndexOutOfBoundsException e) {
                        LOGGER.error("Document {}. Invalid sentence offsets: {}-{}. Document text length: {}.", JCoReTools.getDocId(documentText.getCas()), begin, end, documentText.getCas().getDocumentText().length());
                        throw e;
                    }
                    if (letterMatcher.find()) {
                        if (LOGGER.isTraceEnabled()) {
                            String docId = JCoReTools.getDocId(documentText.getCas());
                            LOGGER.trace("Adding sentence with offsets {}-{}, length {} to document {}", begin, end, end - begin, docId);
                        }
                        if (maxSentenceLength > 0 && annotation.getEnd() - annotation.getBegin() > maxSentenceLength) {
                            Set<Sentence> subSentences = new HashSet<>();
                            LOGGER.debug("Sentence length {} exceeds maximum sentence length of {}. It is split into smaller chunks.", annotation.getEnd() - annotation.getBegin(), maxSentenceLength);
                            LOGGER.debug("Splitting at newlines.");
                            // Split at newlines
                            splitAtRegex(documentText, annotation, eolMatcher, subSentences);
                            // If the set of new sentences is empty, it just means that there were no newlines
                            if (subSentences.isEmpty())
                                subSentences.add(annotation);
                            // Resulting sentences that are still too long are also split at semicoli
                            Iterator<Sentence> sentIt = subSentences.iterator();
                            Set<Sentence> subSubSentences = new HashSet<>();
                            while (sentIt.hasNext()) {
                                Sentence s = sentIt.next();
                                if (s.getEnd() - s.getBegin() > maxSentenceLength) {
                                    LOGGER.debug("Newline splitting still produces overlong sentences. Splitting at semicoli.");
                                    sentIt.remove();
                                    int numSubSubBefore = subSubSentences.size();
                                    splitAtRegex(documentText, s, semicoliMatcher, subSubSentences);
                                    // If there was nothing added, there were no semicoli. Put the sentence back
                                    // to the set that is too long.
                                    if (numSubSubBefore == subSubSentences.size()) {
                                        subSubSentences.add(s);
                                    }
                                }
                            }
                            subSentences.addAll(subSubSentences);
                            // Sentences that are now still too long are just split at some whitespace
                            sentIt = subSentences.iterator();
                            subSubSentences = new HashSet<>();
                            while (sentIt.hasNext()) {
                                Sentence s = sentIt.next();
                                if (s.getEnd() - s.getBegin() > maxSentenceLength) {
                                    LOGGER.debug("Newline and semicoli splitting still produce overlong sentences. Chunking at whitespaces.");
                                    sentIt.remove();
                                    splitAtWhitespaces(documentText, s, subSubSentences);
                                }
                            }
                            for (Sentence s : subSentences)
                                s.addToIndexes();
                            for (Sentence s : subSubSentences)
                                s.addToIndexes();
                        } else if (alwaysSplitAtNewlines) {
                            LOGGER.debug("Splitting at newlines.");
                            Set<Sentence> subSentences = new HashSet<>();
                            // Split at newlines
                            splitAtRegex(documentText, annotation, eolMatcher, subSentences);
                            // If the set of new sentences is empty, it just means that there were no newlines
                            if (subSentences.isEmpty())
                                subSentences.add(annotation);
                            subSentences.forEach(Annotation::addToIndexes);
                        } else {
                            annotation.addToIndexes();
                        }
                    }
                }
                start = -1;
            }
        }
    }

    /**
     * <p>The ultimate fallback for overlong sentences: Split at whitespaces so that the resulting "sentences" are short enough.</p>
     *
     * @param documentText
     * @param overlongSentence
     * @param subSentences
     */
    private void splitAtWhitespaces(JCoReCondensedDocumentText documentText, Sentence overlongSentence, Set<Sentence> subSentences) {
        wsMatcher.reset(overlongSentence.getCoveredText());
        int currentSentenceLength = 0;
        int lastEnd = overlongSentence.getBegin();
        while (wsMatcher.find()) {
            if (currentSentenceLength + wsMatcher.end() > maxSentenceLength) {
                int subBegin = adjustBeginOffsetForWhitespaces(lastEnd, documentText);
                int subEnd = adjustEndOffsetForWhitespaces(overlongSentence.getBegin() + wsMatcher.start(), documentText);
                if (subEnd > subBegin) {
                    Sentence s = new Sentence(documentText.getCas(), subBegin, subEnd);
                    s.setComponentId(this.getClass().getName());
                    subSentences.add(s);
                    lastEnd = s.getEnd();
                    currentSentenceLength = 0;
                } else {
                    LOGGER.debug("Not creating whitespace-segmented sub-sentence because its offsets would be invalid: {}-{}", subBegin, subEnd);
                }
            }
            currentSentenceLength += wsMatcher.end();
        }
        // Also add a sentence of the tail
        int subBegin = adjustBeginOffsetForWhitespaces(lastEnd, documentText);
        int subEnd = adjustEndOffsetForWhitespaces(overlongSentence.getEnd(), documentText);
        if (subEnd > subBegin) {
            Sentence s = new Sentence(documentText.getCas(), subBegin, subEnd);
            s.setComponentId(this.getClass().getName());
            subSentences.add(s);
        } else {
            LOGGER.debug("Not creating whitespace-segmented sub-sentence because its offsets would be invalid: {}-{}", subBegin, subEnd);
        }
    }

    /**
     * <p>Fallback algorithm for sentences that exceed the maximum sentence length.</p>
     * <p>This splits simply at regular expression matches.</p>
     *
     * @param documentText
     * @param originalOverlongSentence
     * @param subSentences
     */
    private void splitAtRegex(JCoReCondensedDocumentText documentText, Sentence originalOverlongSentence, Matcher splitMatcher, Set<Sentence> subSentences) {
        splitMatcher.reset(originalOverlongSentence.getCoveredText());
        int lastEnd = originalOverlongSentence.getBegin();
        while (splitMatcher.find()) {
            int subBegin = adjustBeginOffsetForWhitespaces(lastEnd, documentText);
            int subEnd = adjustEndOffsetForWhitespaces(originalOverlongSentence.getBegin() + splitMatcher.start(), documentText);
            if (subBegin < subEnd) {
                Sentence s = new Sentence(documentText.getCas(), subBegin, subEnd);
                s.setComponentId(this.getClass().getName());
                subSentences.add(s);
                lastEnd = subEnd;
            } else {
                LOGGER.warn("Not creating regex-segmented sub-sentence because its offsets would be invalid: {}-{}", subBegin, subEnd);
            }
        }
        // Check if the current end is also the end of overly large sentence. This is true if
        // the original sentence ended in a newline. We give a margin of two additional characters
        // for another newline or a double newline.
        if (lastEnd < originalOverlongSentence.getEnd() - 2) {
            int subBegin = adjustBeginOffsetForWhitespaces(lastEnd, documentText);
            int subEnd = adjustEndOffsetForWhitespaces(originalOverlongSentence.getEnd(), documentText);
            if (subEnd > subBegin) {
                Sentence s = new Sentence(documentText.getCas(), subBegin, subEnd);
                s.setComponentId(this.getClass().getName());
                subSentences.add(s);
            } else {
                LOGGER.warn("Not creating regex-segmented sub-sentence because its offsets would be invalid: {}-{}", subBegin, subEnd);
            }
        }
    }

    private int adjustBeginOffsetForWhitespaces(int begin, JCoReCondensedDocumentText documentText) {
        while (begin < documentText.getCas().getDocumentText().length() && Character.isWhitespace(documentText.getCas().getDocumentText().charAt(begin)))
            ++begin;
        return begin;
    }

    private int adjustEndOffsetForWhitespaces(int end, JCoReCondensedDocumentText documentText) {
        while (end > 0 && Character.isWhitespace(documentText.getCas().getDocumentText().codePointAt(end - 1)))
            --end;
        return end;
    }
}
