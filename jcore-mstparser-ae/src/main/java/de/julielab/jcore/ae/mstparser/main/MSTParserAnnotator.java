/**
 * MSTParserAnnotator.java
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
 * This is the AE which uses the MST parser
 **/
package de.julielab.jcore.ae.mstparser.main;

import de.julielab.jcore.types.*;
import edu.upenn.seas.mstparser.DependencyParser;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * This is the analysis engine (AE) which uses the MST parser.
 *
 * @author Lichtenwald
 */
public class MSTParserAnnotator extends JCasAnnotator_ImplBase {

    private static final int TIMEOUT = 30;

    public static final String COMPONENT_ID = "de.julielab.jcore.ae.mstparser.main.MSTParserAnnotator";

    public static final String PARAM_MAX_NUM_TOKENS = "MaxNumTokens";

    @ConfigurationParameter(name = PARAM_MAX_NUM_TOKENS, description = "The maximum number of tokens a sentence may have to be subject to parsing. If a sentence has more tokens, it will be skipped by the component. If no value is given, no restriction of the number of tokens is imposed.")
    private Integer maxNumTokens;

    @ExternalResource(key = RESOURCE_MODEL, mandatory = true)
    private MSTParserWrapper mstParserWrapper;

    private DependencyParser mstParser;

    private ExecutorService executor;

    private static final Logger LOGGER = LoggerFactory.getLogger(MSTParserAnnotator.class);

    static final String MODEL_FILE_NAME = "modelFileName";

    static final String TEMPORARY_PATH = "temporaryPath";

    private static final String PROJECTIVE = "proj";

    private static final String NON_PROJECTIVE = "non-proj";

    private static final String LABEL_DUMMY = "<no-type>";

    private static final String DEPENDENCY_DUMMY = "0";

    static final String FORMAT = "format";

    static final String FORMAT_MST = "MST";

    static final String FORMAT_CONLL = "CONLL";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String TABULATOR = "\t";

    private static final String PLACEHOLDER = "_";

    private static final String EMPTY_STRING = "";

    private static final boolean defaultProjective = true;

    public static final String RESOURCE_MODEL = "SharedModel";

    // private static String value_modelFileName = null;
    //
    // private static String value_temporaryPath = null;
    //
    // private static String value_format = null;

    private static boolean parameters_valid = true;

    /*--------------------------------------------------------------------------------------------*/
    @Override
    /**
     * Initialize the parser using configuration parameters from the aContext
     *
     * @param aContext
     *            UimaContext which carries the configuration parameter
     */
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        try {
            LOGGER.info("MST Parser Annotator is being initialized ... ");

            // invoke default initialization
            super.initialize(aContext);

            // get parameters from UIMA context
            // value_modelFileName = (String)
            // aContext.getConfigParameterValue(MODEL_FILE_NAME);
            // value_temporaryPath = (String)
            // aContext.getConfigParameterValue(TEMPORARY_PATH);
            // value_format = (String) aContext.getConfigParameterValue(FORMAT);
            maxNumTokens = (Integer) aContext.getConfigParameterValue(PARAM_MAX_NUM_TOKENS);
            if (maxNumTokens != null) {
                LOGGER.info("Skipping sentences with more than " + maxNumTokens + " tokens");
            }

            // This isn't like a shared resource should be used in UIMA. The
            // MSTParserWrapperImpl keeps the loaded data as static member
            // variables. I think it works (at least as long as there is only
            // one parser or one configuration used) but isn't quite the
            // canonical way.
            aContext.getResourceObject(RESOURCE_MODEL);
            mstParserWrapper = new MSTParserWrapperImpl();

            mstParser = mstParserWrapper.loadModel();

            executor = Executors.newCachedThreadPool();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Cannot innitialize MST Parser " + e.getMessage());
            throw new ResourceInitializationException(e);
        }
    } // of initialize

    /*--------------------------------------------------------------------------------------------*/
    @Override
    /**
     * Run the analysis engine using data which was extracted from the jcas.
     *
     * @param jcas
     *            JCas which will be used to get the information which will be processed.
     */
    public void process(JCas jcas) throws AnalysisEngineProcessException {
        if (parameters_valid) {
            LOGGER.trace("MST Parser Annotator is processing ... ");
            AnnotationIndex<Annotation> sentenceIndex = jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
            AnnotationIndex<Annotation> tokenIndex = jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);
            FSIterator<Annotation> sentenceIterator = sentenceIndex.iterator();

            while (sentenceIterator.hasNext()) {
                Sentence casSentence = (Sentence) sentenceIterator.next();
                FSIterator<Annotation> tokenIterator = tokenIndex.subiterator(casSentence);
                List<Token> tokenList = new ArrayList<Token>();
                List<String> tokenTextList = new ArrayList<String>();
                List<POSTag> posTagList = new ArrayList<POSTag>();
                List<String> posTagTextList = new ArrayList<String>();

                int i = 0;
                while (tokenIterator.hasNext()) {
                    Token token = (Token) tokenIterator.next();
                    i++;
                    String tokenText = token.getCoveredText();
                    tokenList.add(token);
                    tokenTextList.add(tokenText);

                    if (token.getPosTag() == null || token.getPosTag().size() == 0 || token.getPosTag(0) == null) {
                        String docId = getDocId(jcas);
                        throw new IllegalStateException(
                                "The parser expects that each token has (at least) one part of speech tag at index 0 of the PosTag feature array. However, the token \""
                                        + token.getCoveredText() + "\", offsets " + token.getBegin() + "-"
                                        + token.getEnd() + " of document " + docId
                                        + " does not appear to have a POS tag.");
                    }

                    POSTag posTag = token.getPosTag(0);
                    String posText = posTag.getValue();
                    posTagList.add(posTag);
                    posTagTextList.add(posText);
                }
                if (maxNumTokens != null && i > maxNumTokens) {
                    LOGGER.warn(
                            "Skipping sentence with > " + maxNumTokens + " tokens: " + casSentence.getCoveredText());
                    break;
                }
                final String inputSentence = getSentence(tokenTextList, posTagTextList, null, null);
                /**
                 * set temporary file to the Random file name
                 */
                String parsedSentence = null;
                try {
                    Callable<String> task = new Callable<String>() {
                        @Override
                        public String call() throws IOException {
                            return mstParserWrapper.predict(mstParser, inputSentence);
                        }
                    };
                    Future<String> future = executor.submit(task);
                    try {
                        parsedSentence = future.get(TIMEOUT, TimeUnit.SECONDS);
                    } catch (TimeoutException ex) {
                        LOGGER.warn(
                                "The processing of a sentence was cancelled because it took too long (more than {} seconds). The actual sentence is put out below.",
                                TIMEOUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        // handle the interrupts
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        // handle other exceptions
                    } finally {
                        future.cancel(true); // may or may not desire this
                    }
                }
                // catch (IOException e) {
                // LOGGER.error("[MST Parser Exception]" + e.getMessage());
                // }
                catch (OutOfMemoryError e) {
                    String docId = getDocId(jcas);
                    LOGGER.warn(
                            "OutOfMemory error occured when parsing the sentence \"{}\" of document \"{}\". "
                                    + "This does not mean necessarely your application has too less allocated memory. "
                                    + "On rare occasions (very rare: it seems there is only one document in the whole of MEDLINE "
                                    + "causing this error, (PMID: 23717185) which contains a ridiculous large enumeration "
                                    + "of terms), the parser seems to consume arbitrary amounts of memory without actually being"
                                    + " able to parse the input. In such a case you would observe a sudden pike in memory consumption "
                                    + "(e.g. from constantly around 10GB to suddenly 20GB then 30GB, depending on the maximum "
                                    + "available amount of memory set by the -Xmx option). Only if the memory consumption is constantly "
                                    + "near the maximum, you should consider to increase the amount of allocated memory.",
                            casSentence.getCoveredText(), docId);
                }

                // When the parser had an error, we will only notice here when
                // something is null which shouldn't be.
                try {
                    writeCas(mstParser, jcas, parsedSentence, tokenList);
                } catch (Exception e) {
                    LOGGER.error("Sentence could not be parsed and will not have syntactic annotations in the CAS: "
                            + casSentence.getCoveredText());
                }
            } // of while
        } else {
            LOGGER.warn("Cannot continue parsing. Please check the parameters!");
        } // of if
    } // of process

    protected String getDocId(JCas jcas) {
        FSIterator<Annotation> it = jcas.getJFSIndexRepository().getAnnotationIndex(Header.type).iterator();
        String docId = "<unknown>";
        if (it.hasNext()) {
            docId = ((Header) it.next()).getDocId();
        }
        return docId;
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        executor.shutdown();
        super.collectionProcessComplete();
    }

    @Override
    public void destroy() {
        executor.shutdown();
        super.destroy();
    }

    /*--------------------------------------------------------------------------------------------*/
    /**
     * Write the parsed sentence information back to the Cas.
     *
     * @param mstParser
     *            DependencyParser which will be checked for toe used format (MST or CONLL)
     * @param jcas
     *            JCas which the parsed info will be written to
     * @param parsedSentence
     *            String which contain the parsed information
     * @param tokenList
     *            ArrayList which contain Tokens
     */
    private void writeCas(DependencyParser mstParser, JCas jcas, String parsedSentence, List<Token> tokenList) {
        Token token;
        int dependencyIndex = 0;
        List<Integer> depList = getDependencyRelations(parsedSentence);
        List<String> labelsList = getLabels(parsedSentence);

        for (int i = 0; i < tokenList.size(); i++) {
            token = tokenList.get(i);
            dependencyIndex = depList.get(i) - 1;
            Token headToken = null;

            if (dependencyIndex >= 0) {
                headToken = tokenList.get(dependencyIndex);
                FSArray depRelationFSArray = new FSArray(jcas, 1);
                DependencyRelation depRelation = new DependencyRelation(jcas);
                depRelation.setHead(headToken);
                setProjective(depRelation, mstParser);
                depRelation.setLabel(labelsList.get(i));

                // TODO: This works, but if you experience problems with
                // dependency relations, here is the first place to look
                depRelation.setBegin(tokenList.get(i).getBegin());
                depRelation.setEnd(tokenList.get(i).getEnd());
                depRelation.setComponentId(COMPONENT_ID);
                depRelation.addToIndexes(jcas);
                depRelationFSArray.set(0, depRelation);
                depRelationFSArray.addToIndexes(jcas);
                token.setDepRel(depRelationFSArray);
            } else {
                DependencyRelation depRelation = new DependencyRelation(jcas);
                depRelation.setComponentId(COMPONENT_ID);
                depRelation.addToIndexes();
                FSArray depRelationFSArray = new FSArray(jcas, 1);
                depRelationFSArray.set(0, depRelation);
                depRelationFSArray.addToIndexes();
                token.setDepRel(depRelationFSArray);
            } // of if else
        } // of for
    } // of writeCas

    /**
     * Construct an input sentence for the MST parser using the tokenTextList and the posTagList. If the sentence will
     * be in MST or CONLL format depends on the value of mstParser.options.format
     *
     * @param tokens
     *            List which contains Strings which represent the tokens
     * @param posTags
     *            List which contains Strings which in turn represent the POS Tags
     * @param labels
     *            List containing Strings which represent the labels
     * @param depRelations
     *            List containing Strings which represent dependencies
     * @return sentence String which contain the input sentence for the MST parser
     */
    private String getSentence(List<String> tokens, List<String> posTags, List<String> labels,
            List<String> depRelations) {
        if (labels == null || labels.size() != tokens.size()) {
            labels = createList(LABEL_DUMMY, tokens.size());
        }
        if (depRelations == null || depRelations.size() != tokens.size()) {
            depRelations = createList(DEPENDENCY_DUMMY, tokens.size());
        }
        StringBuffer sentence = new StringBuffer();
        if (mstParser.options.format.equals("MST")) {
            sentence.append(addListElements(tokens));
            sentence.append(addListElements(posTags));
            sentence.append(addListElements(labels));
            sentence.append(addListElements(depRelations));
        } else if (mstParser.options.format.equals("CONLL")) {
            for (int i = 0; i < tokens.size(); i++) {
                sentence.append(i + 1); // 1. ID
                sentence.append(TABULATOR + tokens.get(i).replaceAll("\n", " ").replaceAll("\t", " ")); // 2.
                                                                                                        // TEXT
                sentence.append(TABULATOR + PLACEHOLDER); // 3. LEMMA TODO:
                                                          // warum wird token
                                                          // text ohne
                                                          // Lemmatisierung
                                                          // verwendet??
                sentence.append(TABULATOR + posTags.get(i));// 4. Coarse POS
                sentence.append(TABULATOR + posTags.get(i));// 5. POS
                sentence.append(TABULATOR + PLACEHOLDER); // 6. FEATURES ("|"-
                                                          // or
                // "_"-separated)
                sentence.append(TABULATOR + depRelations.get(i));// 7. HEAD
                sentence.append(TABULATOR + labels.get(i)); // 8. Dep.Rel. (9.
                                                            // Head of Phrase,
                                                            // 10. Dep.Rel. to
                                                            // head of phrase)
                sentence.append(LINE_SEPARATOR);
            }
        }
        return sentence.toString();
    }

    /*--------------------------------------------------------------------------------------------*/
    /**
     * Set projective in the dependency relation using the decode type in the mstparser's options
     *
     * @param depRelation
     *            DependencyRelation which will be updated after setting the projective value
     * @param mstParser
     *            DependencyParser which options will be used to set the projective value
     */
    private void setProjective(DependencyRelation depRelation, DependencyParser mstParser) {
        String decodeType = mstParser.options.decodeType;

        if (decodeType.equals(PROJECTIVE)) {
            depRelation.setProjective(true);
        } else if (decodeType.equals(NON_PROJECTIVE)) {
            depRelation.setProjective(false);
        } else {
            LOGGER.error("setProjective: decode mode is invalid. Setting projective to the default value "
                    + defaultProjective);
            depRelation.setProjective(defaultProjective);
        } // of else
    } // of setProjective

    /**
     * Compute the dependency list using the parsed sentence information.
     *
     * @param parsedSentence
     *            String which represents the parsed sentence information
     * @return outputList ArrayList which contains the dependency index for each element (each token)
     */
    private List<Integer> getDependencyRelations(String parsedSentence) {
        List<Integer> depRels = new ArrayList<Integer>();
        try {
            if (mstParser.options.format.equals("MST")) {
                String depRelLine = parsedSentence.split(LINE_SEPARATOR)[3];
                for (String depRel : depRelLine.split(TABULATOR)) {
                    depRels.add(Integer.parseInt(depRel));
                }
            } else if (mstParser.options.format.equals("CONLL")) {
                for (String line : parsedSentence.split(LINE_SEPARATOR)) {
                    depRels.add(Integer.parseInt(line.split(TABULATOR)[6]));
                }
            }
        } catch (Exception e) {
            LOGGER.error("The parsed sentence has unexpected format (no 8th. column exists).");
        }
        return depRels;
    }

    /**
     * Compute the labels list using the parsed sentence information.
     *
     * @param parsedSentence
     *            String which represents the parsed sentence information
     * @return labels ArrayList which contains the labels
     */
    private List<String> getLabels(String parsedSentence) {
        List<String> labels = new ArrayList<String>();
        try {
            if (mstParser.options.format.equals("MST")) {
                String labelLine = parsedSentence.split(LINE_SEPARATOR)[2];
                labels = Arrays.asList(labelLine.split(TABULATOR));
            } else if (mstParser.options.format.equals("CONLL")) {
                for (String line : parsedSentence.split(LINE_SEPARATOR)) {
                    labels.add(line.split(TABULATOR)[7]);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected format of parsed sentence.");
        }
        return labels;
    }

    /**
     * Create a list of a specified size and fill it with specified String
     *
     * @param labelDummy
     *            String which will be put into every element of the created list
     * @param listSize
     *            Integer which specifies the size of the list to be created
     * @return outputList ArrayList which was created and filled with specified String
     */
    private List<String> createList(String element, int listSize) {
        List<String> outputList = new ArrayList<String>();
        for (int i = 0; i < listSize; i++) {
            outputList.add(element);
        }
        return outputList;
    }

    /**
     * Add to the sentence new elements from the elementTextList
     *
     * @param list
     *            ArrayList containing elements which will be added to the sentence
     * @return sentence String which new elements will be added to
     */
    private String addListElements(List<String> list) {
        StringBuffer sentence = new StringBuffer();
        if (!list.isEmpty()) {
            for (String element : list) {
                if (sentence.length() > 0) {
                    sentence.append(TABULATOR);
                }
                sentence.append(element);
            }
            sentence.append(LINE_SEPARATOR);
        }
        return sentence.toString();
    }
}

// /*--------------------------------------------------------------------------------------------*/
// /**
// * Return the labels list using the parsed sentence information. Dependent
// * on the format (MST or CONLL) one of the two methods will be called
// *
// * @param mstParser
// * DependencyParser which will be checked for the used format
// * (MST or CONLL)
// * @param parsedSentence
// * String which represents the parsed sentence information
// * @return outputList ArrayList which contains the labels for each element
// * (each token)
// */
// private ArrayList<String> getLabelsList(DependencyParser mstParser,
// String parsedSentence) {
// ArrayList<String> outputList = new ArrayList<String>();
//
// if (mstParser.options.format.equals(FORMAT_MST)) {
// outputList = getMSTLabelsList(parsedSentence);
// } else if (mstParser.options.format.equals(FORMAT_CONLL)) {
// outputList = getCONLLLabelsList(parsedSentence);
// } // of else
// return outputList;
// } // of getLabelsList

// /*--------------------------------------------------------------------------------------------*/
// /**
// * Compute the labels list using the parsed sentence information in the MST
// * format.
// *
// * @param parsedSentence
// * String which represents the parsed sentence information
// * @return outputList ArrayList which contains the labels
// */
// private ArrayList<String> getMSTLabelsList(String parsedSentence) {
// ArrayList<String> outputList = new ArrayList<String>();
// String auxElement = EMPTY_STRING;
// String localParsedSentence = parsedSentence;
//
// try {
// // exclude tokens
// localParsedSentence = localParsedSentence
// .substring(localParsedSentence.indexOf(LINE_SEPARATOR) + 1);
// // exclude POS tags
// localParsedSentence = localParsedSentence
// .substring(localParsedSentence.indexOf(LINE_SEPARATOR) + 1);
// // exclude final line breaks
// localParsedSentence = localParsedSentence.substring(0,
// localParsedSentence.indexOf(LINE_SEPARATOR));
// localParsedSentence = localParsedSentence + TABULATOR;
// } catch (Exception e) {
// LOGGER.error("getMSTLabelsList - parsed sentence error! "
// + "The parsed sentence "
// + "has unexpected format. Try checking the input data.");
// } // of try catch
//
// while (localParsedSentence.contains(TABULATOR)) {
// auxElement = localParsedSentence.substring(0, localParsedSentence
// .indexOf(TABULATOR));
// localParsedSentence = localParsedSentence
// .substring(localParsedSentence.indexOf(TABULATOR) + 1);
// outputList.add(auxElement);
// } // of while
// return outputList;
// } // of getMSTLabelsList
//
// /*--------------------------------------------------------------------------------------------*/
// /**
// * Compute the labels list using the parsed sentence information in the
// * CONLL format.
// *
// * @param parsedSentence
// * String which represents the parsed sentence information
// * @return outputList ArrayList which contains the labels
// */
// private ArrayList<String> getCONLLLabelsList(String parsedSentence) {
// ArrayList<String> outputList = new ArrayList<String>();
// ArrayList<String> lineList = new ArrayList<String>();
// String line = EMPTY_STRING;
// String localParsedSentence = parsedSentence;
// try {
// while (localParsedSentence.contains(LINE_SEPARATOR)) {
// line = localParsedSentence.substring(0, localParsedSentence
// .indexOf(LINE_SEPARATOR));
// localParsedSentence = localParsedSentence.substring(
// localParsedSentence.indexOf(LINE_SEPARATOR) + 1,
// localParsedSentence.length());
// if (line.length() > 0) {
// lineList.add(line);
// } // of if
// } // of while
//
// // Please note the following assumption: the dependency value is the
// // seventh "token" within the line; "tokens" are separated by
// // tabulator (\t)
// for (int i = 0; i < lineList.size(); i++) {
// line = lineList.get(i);
//
// // exclude the initial token index
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude the two token texts
// line = line.substring(line.indexOf(TABULATOR) + 1);
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude the two POS tag texts
// line = line.substring(line.indexOf(TABULATOR) + 1);
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude whatever this might be (hyphen)
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude the dependency
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude subsequent information like labels
// line = line.substring(0, line.indexOf(TABULATOR));
//
// outputList.add(line);
// } // of for
// } catch (Exception e) {
// LOGGER.error("getCONLLLabelsList - parsed sentence error! "
// + "The parsed sentence "
// + "has unexpected format. Try checking the input data.");
// } // of try catch
// return outputList;
// } // of getDependenciesList
//
// /*--------------------------------------------------------------------------------------------*/
// /**
// * Return an ArrayList with dependencies within the parsed sentence.
// * Dependent on the format one of the two methods will be called.
// *
// * @param mstParser
// * DependencyParser which will be checked for the used format
// * (MST or CONLL)
// * @param parsedSentence
// * String which represents the parsed sentence information
// * @return outputList ArrayList which contains the dependency index for each
// * element (each token)
// */
// private ArrayList<Integer> getDependenciesList(DependencyParser mstParser,
// String parsedSentence) {
// ArrayList<Integer> outputList = new ArrayList<Integer>();
//
// if (mstParser.options.format.equals(FORMAT_MST)) {
// outputList = getMSTDependenciesList(parsedSentence);
// } else if (mstParser.options.format.equals(FORMAT_CONLL)) {
// outputList = getCONLLDependenciesList(parsedSentence);
// } // of else
// return outputList;
// } // of getDependenciesList
//
// /*--------------------------------------------------------------------------------------------*/
// /**
// * Compute the dependency list using the parsed sentence information.
// *
// * @param parsedSentence
// * String which represents the parsed sentence information
// * @return outputList ArrayList which contains the dependency index for each
// * element (each token)
// */
// private ArrayList<Integer> getMSTDependenciesList(String parsedSentence) {
// ArrayList<Integer> outputList = new ArrayList<Integer>();
// String auxElement = EMPTY_STRING;
// String localParsedSentence = parsedSentence;
//
// try {
// // exclude tokens
// localParsedSentence = localParsedSentence
// .substring(localParsedSentence.indexOf(LINE_SEPARATOR) + 1);
//
// // exclude POS tags
// localParsedSentence = localParsedSentence
// .substring(localParsedSentence.indexOf(LINE_SEPARATOR) + 1);
//
// // exclude labels
// localParsedSentence = localParsedSentence
// .substring(localParsedSentence.indexOf(LINE_SEPARATOR) + 1);
//
// // exclude final line breaks
// localParsedSentence = localParsedSentence.substring(0,
// localParsedSentence.indexOf(LINE_SEPARATOR));
// localParsedSentence = localParsedSentence + TABULATOR;
// } catch (Exception e) {
// LOGGER.error("getMSTDependenciesList - parsed sentence error! "
// + "The parsed sentence "
// + "has unexpected format. Try checking the input data.");
// } // of try catch
//
// while (localParsedSentence.contains(TABULATOR)) {
// auxElement = localParsedSentence.substring(0, localParsedSentence
// .indexOf(TABULATOR));
// localParsedSentence = localParsedSentence
// .substring(localParsedSentence.indexOf(TABULATOR) + 1);
// outputList.add(Integer.parseInt(auxElement));
// } // of while
// return outputList;
// } // of getMSTDependenciesList
//
// /*--------------------------------------------------------------------------------------------*/
// /**
// * Compute the dependency list using the parsed sentence information in the
// * CONLL format.
// *
// * @param parsedSentence
// * String which represents the parsed sentence information
// * @return outputList ArrayList which contains the dependency index for each
// * element (each token)
// */
// private ArrayList<Integer> getCONLLDependenciesList(String parsedSentence) {
// ArrayList<Integer> outputList = new ArrayList<Integer>();
// ArrayList<String> lineList = new ArrayList<String>();
// String line = EMPTY_STRING;
// String localParsedSentence = parsedSentence;
// int dependencyIndex = 0;
// try {
// while (localParsedSentence.contains(LINE_SEPARATOR)) {
// line = localParsedSentence.substring(0, localParsedSentence
// .indexOf(LINE_SEPARATOR));
// localParsedSentence = localParsedSentence.substring(
// localParsedSentence.indexOf(LINE_SEPARATOR) + 1,
// localParsedSentence.length());
// if (line.length() > 0) {
// lineList.add(line);
// } // of if
// } // of while
//
// // Please note the following assumption: the dependency value is
// // the seventh "token" within the line; "tokens" are separated by
// // tabulator (\t)
// for (int i = 0; i < lineList.size(); i++) {
// line = lineList.get(i);
//
// // exclude the initial token index
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude the two token texts
// line = line.substring(line.indexOf(TABULATOR) + 1);
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude the two POS tag texts
// line = line.substring(line.indexOf(TABULATOR) + 1);
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude whatever this might be (hyphen)
// line = line.substring(line.indexOf(TABULATOR) + 1);
//
// // exclude subsequent information like labels
// line = line.substring(0, line.indexOf(TABULATOR));
//
// dependencyIndex = Integer.parseInt(line);
// outputList.add(dependencyIndex);
// } // of for
// } catch (Exception e) {
// LOGGER.error("getCONLLDependenciesList - parsed sentence error! "
// + "The parsed sentence "
// + "has unexpected format. Try checking the input data.");
// } // of try catch
// return outputList;
// } // of getCONLLDependenciesList
//
// /*--------------------------------------------------------------------------------------------*/
// /**
// * Return an input sentence for the MST parser. Dependent on the format one
// * of the two methods will be called.
// *
// * @param mstParser
// * DependencyParser which will be checked for the used format
// * (MST or CONLL)
// * @param tokenTextList
// * ArrayList which contain Strings which represent the tokens
// * @param posTagTextList
// * ArrayList which contain Strings which in turn represent the
// * POS Tags
// * @param labelTextList
// * ArrayList containing Strings which represent the labels
// * @param depTextList
// * ArrayList containing Strings which represent dependencies
// * @return sentence String which contain the input sentence for the MST
// * parser
// */
// private String getSentence(DependencyParser mstParser,
// ArrayList<String> tokenTextList, ArrayList<String> posTagTextList,
// ArrayList<String> labelTextList, ArrayList<String> depTextList) {
// String sentence = EMPTY_STRING;
//
// if (mstParser.options.format.equals(FORMAT_MST)) {
// sentence = getMSTFormatSentence(tokenTextList, posTagTextList,
// null, null);
// } else if (mstParser.options.format.equals(FORMAT_CONLL)) {
// sentence = getCONLLFormatSentence(tokenTextList, posTagTextList,
// null, null);
// } // of else
// return sentence;
// } // of getSentence
//
// /*--------------------------------------------------------------------------------------------*/
// /**
// * Construct an input sentence for the MST parser using the tokenTextList
// * and the posTagList. The sentence will be in the MST format
// *
// * @param tokenTextList
// * ArrayList which contain Strings which represent the tokens
// * @param posTagTextList
// * ArrayList which contain Strings which in turn represent the
// * POS Tags
// * @param labelTextList
// * ArrayList containing Strings which represent the labels
// * @param depTextList
// * ArrayList containing Strings which represent dependencies
// * @return sentence String which contain the input sentence for the MST
// * parser
// */
// private String getMSTFormatSentence(ArrayList<String> tokenTextList,
// ArrayList<String> posTagTextList, ArrayList<String> labelTextList,
// ArrayList<String> depTextList) {
// StringBuffer sentence = new StringBuffer(EMPTY_STRING);
// ArrayList<String> localLabelTextList = labelTextList;
// ArrayList<String> localDepTextList = depTextList;
//
// sentence.append(addElementText(tokenTextList));
// sentence.append(addElementText(posTagTextList));
//
// if (!(localLabelTextList == null)
// && (localLabelTextList.size() == tokenTextList.size())) {
// sentence.append(addElementText(localLabelTextList));
// } else {
// localLabelTextList = createList(LABEL_DUMMY, tokenTextList.size());
// sentence.append(addElementText(localLabelTextList));
// } // of if else
//
// if (!(localDepTextList == null)
// && (localDepTextList.size() == tokenTextList.size())) {
// sentence.append(addElementText(localDepTextList));
// } else {
// localDepTextList = createList(DEPENDENCY_DUMMY, tokenTextList
// .size());
// sentence.append(addElementText(localDepTextList));
// } // of if else
// return sentence.toString();
// } // of getMSTFormatSentence
//
// /*--------------------------------------------------------------------------------------------*/
// /**
// * Construct an input sentence for the MST parser using the tokenTextList
// * and the posTagList. The sentence will be in the CONLL format
// *
// * @param tokenTextList
// * ArrayList which contain Strings which represent the tokens
// * @param posTagTextList
// * ArrayList which contain Strings which in turn represent the
// * POS Tags
// * @param labelTextList
// * ArrayList containing Strings which represent the labels
// * @param depTextList
// * ArrayList containing Strings which represent dependencies
// * @return sentence String which contain the input sentence for the MST
// * parser
// */
// private String getCONLLFormatSentence(ArrayList<String> tokenTextList,
// ArrayList<String> posTagTextList, ArrayList<String> labelTextList,
// ArrayList<String> depTextList) {
// String sentence = EMPTY_STRING;
// ArrayList<String> localLabelTextList = labelTextList;
// ArrayList<String> localDepTextList = depTextList;
//
// if ((localLabelTextList == null)
// || (localLabelTextList.size() == tokenTextList.size())) {
// localLabelTextList = createList(LABEL_DUMMY, tokenTextList.size());
// } // of if
//
// if ((localDepTextList == null)
// || (localDepTextList.size() == tokenTextList.size())) {
// localDepTextList = createList(DEPENDENCY_DUMMY, tokenTextList
// .size());
// } // of if
//
// for (int i = 0; i < tokenTextList.size(); i++) {
// sentence = sentence + (i + 1) + TABULATOR + tokenTextList.get(i)
// + TABULATOR + tokenTextList.get(i) + TABULATOR
// + posTagTextList.get(i) + TABULATOR + posTagTextList.get(i)
// + TABULATOR + PLACEHOLDER + TABULATOR
// + localDepTextList.get(i) + TABULATOR
// + localLabelTextList.get(i) + LINE_SEPARATOR;
// } // of for
//
// return sentence;
// } // of getCONLLFormatSentence
//
// /*--------------------------------------------------------------------------------------------*/
// /**
// * Create a list of a specified size and fill it with specified String
// *
// * @param labelDummy
// * String which will be put into every element of the created
// * list
// * @param listSize
// * Integer which specifies the size of the list to be created
// * @return outputList ArrayList which was created and filled with specified
// * String
// */
// private ArrayList<String> createList(String labelDummy, int listSize) {
// ArrayList<String> outputList = new ArrayList<String>();
//
// for (int i = 0; i < listSize; i++) {
// outputList.add(labelDummy);
// } // of for
// return outputList;
// } // of createList
//
//
// /**
// * Add to the sentence new elements from the elementTextList
// *
// * @param elementTextList
// * ArrayList containing elements which will be added to the
// * sentence
// * @return sentence String which new elements will be added to
// */
// private String addElementText(List<String> elementTextList) {
// String sentence = EMPTY_STRING;
// for (int i = 0; i < elementTextList.size(); i++) {
// sentence = sentence + elementTextList.get(i) + TABULATOR;
// } // of for
//
// try {
// sentence = sentence.substring(0, sentence.length() - 1);
// } catch (StringIndexOutOfBoundsException e) {
// LOGGER.error("addElementText - "
// + "There might not be POS tag information. "
// + "Please check the POS tagger!");
// } // of try catch
// sentence = sentence + LINE_SEPARATOR;
// return sentence;
// } // of addElementText
