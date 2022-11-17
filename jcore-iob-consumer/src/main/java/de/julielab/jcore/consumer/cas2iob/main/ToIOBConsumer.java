/**
 * ToIOBConsumer.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: faessler
 * <p>
 * Current version: 2.1
 * Since version:   1.0
 * <p>
 * Creation date: 05.09.2007
 * <p>
 * A CAS Consumer that converts UIMA annotations into IOB format
 */

/**
 *
 */
package de.julielab.jcore.consumer.cas2iob.main;

import de.julielab.jcore.consumer.cas2iob.utils.UIMAUtils;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Paragraph;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jcore.utility.index.Comparators;
import de.julielab.jcore.utility.index.JCoReTreeMapAnnotationIndex;
import de.julielab.jcore.utility.index.TermGenerators;
import de.julielab.segmentationEvaluator.IOBToken;
import de.julielab.segmentationEvaluator.IOToken;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author weigel, faessler
 */
@ResourceMetaData(name = "JCoRe IOB Writer", description = "This component help to write CAS entity or chunk annotations into " +
        "a text file in IOB format.")
public class ToIOBConsumer extends JCasAnnotator_ImplBase {

    public static final String PARAM_LABELS = "labels";
    public static final String PARAM_OUTFOLDER = "outFolder";
    public static final String PARAM_LABEL_METHODS = "labelNameMethods";
    public static final String PARAM_IOB_LABEL_NAMES = "iobLabelNames";
    public static final String PARAM_TYPE_PATH = "typePath";
    public static final String PARAM_MODE = "mode";
    public static final String PARAM_ADD_POS = "addPos";
    public static final String PARAM_COLUMN_SEPARATOR = "columnSeparator";
    public static final String PARAM_IOB_MARK_SEPARATOR = "iobMarkSeparator";
    private static final Logger LOGGER = LoggerFactory.getLogger(ToIOBConsumer.class);
    private final String SENTENCE_END_MARK = "SENTENCE_END_MARKER"; // there will be an empty line for each sentence marker
    private final String PARAGRAPH_END_MARK = "PARAGRAPH_END_MARKER"; // there will be 2 empty lines for each sentence marker
    @ConfigurationParameter(name = PARAM_OUTFOLDER, description = "Path to folder where IOB-files should be written to.")
    String outFolder = null;
    @ConfigurationParameter(name = PARAM_TYPE_PATH, mandatory = false, description = "The path of the UIMA types, e.g. \"de.julielab.jcore.types.\" (with terminating \".\"!). It is prepended to the class names in labelNameMethods. This parameter may be null which is equivalent to the empty String \"\".")
    String typePath = null;
    @ConfigurationParameter(name = PARAM_LABELS, mandatory = false, description = "The labels NOT to be exported into IOB format. Label does here not refer to an UIMA type but to the specific label aquired by the labelNameMethod.")
    String[] labels = null;
    HashMap<String, String> objNameMethMap = null;
    Map<String, String> labelIOBMap = null;
    int id = 1;
    @ConfigurationParameter(name = PARAM_MODE, mandatory = false, description = "This parameter determines whether the IOB or IO annotation schema should be used. The parameter defaults to IOB, the value is not case sensitive.", defaultValue = "IOB")
    private String mode = null;
    @ConfigurationParameter(name = PARAM_LABEL_METHODS, description = "This is the primary parameter to define from which types IOB labels should be derived. The parameter expects pairs of UIMA-annotation-type-names and their corresponding method for extracting the annotation label. Format: <annotationName>[\\s=/\\\\|]<method Name>. The annotation name is fully qualified name of the UIMA type. For abbreviation purposes, the \"" + PARAM_TYPE_PATH + "\" parameter can be used to define a type prefix that will then be prepended to all UIMA type names given in this parameter. So, for example, the prefix \"de.julielab.jcore.types.\" will allow to use the \"specificType\" feature of the \"de.julielab.jcore.types.Gene\" type by providing \"Gene=getSpecificType\".  If the name of the annotation class itself is to be being used as label, only the class name is expected: <annotationName> (here, again, applies the use of the \"" + PARAM_TYPE_PATH + "\" parameter). You also may specify a mix of pairs and single class names. If you give the name extracting method for a class and have also specified its superclass as a single class name, the given method is used rather than the superclass name.")
    private String[] labelNameMethods;
    @ConfigurationParameter(name = PARAM_IOB_LABEL_NAMES, mandatory = false, description = "Pairs of label names in UIMA (aquired by the methods given in labelNameMethods) and the name the label is supposed to get in the outcoming IOB file. Format: <UIMA label name>[\\s=/\\\\|]&lt;IOB label name&gt;")
    private String[] iobLabelNames;
    @ConfigurationParameter(name = PARAM_ADD_POS, mandatory = false, description = "If set to true and if annotations of (sub-)type de.julielab.jcore.types.POSTag are present in the CAS, the PoS tags will be added to the output file as the second column. Defaults to false.")
    private Boolean addPos;
    @ConfigurationParameter(name = PARAM_COLUMN_SEPARATOR, mandatory = false, description = "The string given with this parameter will be used to separate the columns in the output file. Defaults to a single tab character.", defaultValue = "\\t")
    private String separator;
    @ConfigurationParameter(name = PARAM_IOB_MARK_SEPARATOR, mandatory = false, description = "This string will be used to separate the IO(B) mark - i. e. I or B - from the entity or chunk label in the output file. Defaults to an underscore character.")
    private String iobMarkSeparator;

    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        LOGGER.info("Initializing...");

        final String regexp = "[\\s=/\\|]";

        labels = Optional.ofNullable((String[]) aContext.getConfigParameterValue(PARAM_LABELS)).orElse(new String[0]);

        outFolder = (String) aContext.getConfigParameterValue(PARAM_OUTFOLDER);

        labelNameMethods = (String[]) aContext.getConfigParameterValue(PARAM_LABEL_METHODS);

        iobLabelNames = (String[]) aContext.getConfigParameterValue(PARAM_IOB_LABEL_NAMES);

        typePath = (String) aContext.getConfigParameterValue(PARAM_TYPE_PATH);
        if (typePath == null) {
            typePath = "";
        }

        addPos = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_ADD_POS)).orElse(false);

        separator = Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_COLUMN_SEPARATOR)).orElse("\t");
        separator = separator.replaceAll("\\\\t", "\t");

        iobMarkSeparator = Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_IOB_MARK_SEPARATOR)).orElse("_");

        mode = (String) aContext.getConfigParameterValue(PARAM_MODE);
        if (mode.equals("IOB") || mode.equals("iob")) {
            mode = "IOB";
        } else if (mode.equals("IO") || mode.equals("io")) {
            mode = "IO";
        } else {
            throw new ResourceInitializationException();
        }

        if (labelNameMethods != null) {

            objNameMethMap = new HashMap<String, String>();

            for (int i = 0; i < labelNameMethods.length; i++) {
                String[] parts = labelNameMethods[i].split(regexp);
                if (parts.length == 1) {
                    objNameMethMap.put(typePath + parts[0], null);
                } else {
                    objNameMethMap.put(typePath + parts[0], parts[1]);
                }
            }

        }

        if (iobLabelNames != null) {

            labelIOBMap = new HashMap<String, String>();

            for (int i = 0; i < iobLabelNames.length; i++) {
                String[] parts = iobLabelNames[i].split(regexp);
                labelIOBMap.put(parts[0], parts[1]);
            }

        } else {
            labelIOBMap = Collections.emptyMap();
        }

    }

    public void process(JCas jCas) {

        LOGGER.trace("Converting CAS to IO(B)Tokens...");

        IOToken[] ioTokens = convertToIOB(jCas);

        LOGGER.trace("Writing IO(B) file...");

        BufferedWriter bw;
        String outPathName = Paths.get(outFolder, getDocumentId(jCas)).toString() + ".iob";
        if (Files.notExists(Paths.get(outFolder))) {
            (new File(outFolder)).mkdirs();
        }
        try {
            bw = new BufferedWriter(new FileWriter(outPathName));
            for (IOToken token : ioTokens) {
                if (token.getText().equals("") || token.getText().equals(SENTENCE_END_MARK)) {
                    // empty line for sentence break
                    bw.newLine();
                } else if (token.getText().equals("") || token.getText().equals(PARAGRAPH_END_MARK)) {
                    bw.newLine();
                } else {
                    final Stream.Builder<String> sb = Stream.builder();
                    sb.accept(token.getText());
                    sb.accept(token.getPos());
                    sb.accept(token.getIobMark().equals("O") ? token.getIobMark() : token.getIobMark() + iobMarkSeparator + token.getLabel());
                    String line = sb.build().filter(Objects::nonNull).collect(Collectors.joining(separator));
                    bw.write(line);
                    bw.newLine();
                }
            }
            // newline at the very end; this makes it easy to concatenate multiple output IOB files into one larger file
            bw.newLine();
            if (bw != null) {
                bw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.trace("The IO(B) file was written to " + outPathName);
    }

    /**
     * @param jcas
     */
    public IOToken[] convertToIOB(JCas jcas) {

        Boolean no_paragraphs = true;
        ArrayList<IOToken> ioTokens = new ArrayList<IOToken>();

        Iterator[] annotationIters = new Iterator[objNameMethMap.size()];

        Iterator it = objNameMethMap.keySet().iterator();
        for (int i = 0; it.hasNext(); i++) {

            String objName = (String) it.next();
            final Type type = jcas.getTypeSystem().getType(objName);

            annotationIters[i] = jcas.getAnnotationIndex(type).iterator();

        }

        TreeMap<Integer, IOToken> ioTokenMap = new TreeMap<Integer, IOToken>();

        // label all tokens that are in range of an annotation
        tokenLabeling(ioTokenMap, annotationIters, jcas);

        // add the rest of the tokens, i.e. tokens not in range of an annotation type mentioned in
        // the descriptor

        // get a list with all paragraphs
        Iterator<Annotation> paragraphIter = jcas.getAnnotationIndex(Paragraph.type).iterator();
        ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();
        while (paragraphIter.hasNext()) {
            paragraphs.add((Paragraph) paragraphIter.next());
        }
        Paragraph dParagraph = null;
        if (paragraphs.isEmpty()) {
            try {
                dParagraph = (Paragraph) JCoReAnnotationTools.getAnnotationByClassName(jcas, Paragraph.class.getName());
            } catch (ClassNotFoundException | SecurityException
                    | NoSuchMethodException | IllegalArgumentException
                    | InstantiationException | IllegalAccessException
                    | InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            dParagraph.setBegin(0);
            dParagraph.setEnd(jcas.getDocumentText().length());
            dParagraph.setComponentId(ToIOBConsumer.class.getCanonicalName());
            dParagraph.addToIndexes(jcas);

            paragraphs.add(dParagraph);
        }

        Paragraph lastPara = null;

//		int paraCount = 0;
        int overallSentCount = 0;

        Iterator<Annotation> sentIter = jcas.getAnnotationIndex(Sentence.type).iterator();

        while (sentIter.hasNext()) {
            Sentence sentence = (Sentence) sentIter.next();
            int sentCount = 0;
            // find paragraph in which this sentence falls
            // might be null if a sentence falls in more than one paragraphs, then we keep the last paragraph
            Paragraph currentParagraph = lastPara;
            for (Paragraph para : paragraphs) {
                if (para.getBegin() <= sentence.getBegin() && para.getEnd() >= sentence.getEnd()) {
                    currentParagraph = para;
                }

                //for (Sentence sentence : paragraphSentences) {

                //System.out.println("para: " + paraCount + ", sent: " + sentCount);
                // get tokens
                ArrayList<Token> tokenList = (ArrayList<Token>) UIMAUtils.getAnnotations(jcas, sentence, (new Token(
                        jcas, 0, 0)).getClass());
                for (int i = 0; i < tokenList.size(); i++) {
                    Token token = tokenList.get(i);

                    // if we are at the first token, we need to add a sentence break mark which is
                    // later replaced by an empty line
                    if (i == 0 && overallSentCount > 0) {
                        IOToken ioToken;
                        //if (sentCount == 0) {
                        if (currentParagraph != lastPara) {
                            // add paragraph end before this sentence
                            //System.out.println("para end");
                            ioToken = new IOBToken(PARAGRAPH_END_MARK, PARAGRAPH_END_MARK);
                        } else {
                            //System.out.println("sent end");
                            // add sentence end before this sentence
                            ioToken = new IOBToken(SENTENCE_END_MARK, SENTENCE_END_MARK);
                        }

                        ioTokenMap.put(token.getBegin() - 1, ioToken);
                    }

                    if (!ioTokenMap.containsKey(token.getBegin())) {
                        String pos = addPos && token.getPosTag().size() > 0 ? token.getPosTag(0).getValue() : null;
                        IOToken ioToken = new IOBToken(token.getCoveredText(), "O", pos);
                        ioTokenMap.put(token.getBegin(), ioToken);
                    }
                }
                overallSentCount++;
                sentCount++;
            }
        }

        Set beginSet = ioTokenMap.keySet();
        for (Iterator beginIt = beginSet.iterator(); beginIt.hasNext(); ) {
            Integer begin = (Integer) beginIt.next();
            IOToken ioToken = ioTokenMap.get(begin);
            ioTokens.add(ioToken);
        }

        // go over IOBtokens and cast them to IO tokens if necessary
        IOToken[] ret = new IOToken[ioTokens.size()];
        // System.out.println("converting tokens to mode: " + mode);
        if (mode.equals("IOB")) {
            // IOB tokens
            ret = ioTokens.toArray(ret);
        } else {
            // IO tokens
            for (int i = 0; i < ioTokens.size(); i++) {
                IOBToken iobToken = (IOBToken) ioTokens.get(i);
                ret[i] = iobToken.toXIoToken();
            }
        }

        // remove helper paragraph annotation
        if (dParagraph != null)
            dParagraph.removeFromIndexes();

        return ret;
    }

    /**
     * Generates all IOTokens which UIMA tokens are in range of an UIMA annotation given by
     * annotationIters. Therefore, every annotation is considered that is entered into the
     * consumer's descriptor.
     *
     * @param ioTokenMap
     * @param annotationIters
     * @param jcas
     */
    private void tokenLabeling(TreeMap<Integer, IOToken> ioTokenMap, Iterator[] annotationIters, JCas jcas) {

        for (int i = 0; i < annotationIters.length; i++) {
            Iterator annoIter = annotationIters[i];
            final JCoReTreeMapAnnotationIndex<Long, Token> tokenByAnnotation = new JCoReTreeMapAnnotationIndex<>(Comparators.longOverlapComparator(), TermGenerators.longOffsetTermGenerator(), TermGenerators.longOffsetTermGenerator(), jcas, Token.type);

            while (annoIter.hasNext()) {
                // get all annotations of this annotation iterator
                Annotation ann = (Annotation) annoIter.next();
                String label = getAnnotationLabel(ann);

                final Iterator<Token> subtokenIterator = tokenByAnnotation.searchFuzzy(ann).iterator();
                try {
                    Token token = subtokenIterator.next();
                    if (addPos && token.getPosTag() == null)
                        throw new IllegalStateException("The IOB consumer is configured to add the part of speech tag to each token but the token \"" + token.getCoveredText() + "\", " + token + " doesn't have any (the PoS list is null).");
                    String pos = addPos && token.getPosTag().size() > 0 ? token.getPosTag(0).getValue() : null;
                    Integer begin = token.getBegin();

                    if (!ioTokenMap.containsKey(begin)) {
                        IOToken ioToken = new IOBToken(token.getCoveredText(), "B_" + label, pos);
                        ioTokenMap.put(begin, ioToken);
                        while (subtokenIterator.hasNext()) {
                            token = subtokenIterator.next();
                            begin = token.getBegin();
                            ioToken = new IOBToken(token.getCoveredText(), "I_" + label, pos);
                            ioTokenMap.put(begin, ioToken);
                        }
                    } else {
                        handleCompetingAnnotations(ioTokenMap, label, subtokenIterator, token, begin, pos);
                    }

                } catch (NoSuchElementException e) {
                    LOGGER.warn("no token annotation in label annotation: " + ann.getCoveredText() + ", " + ann);
                    // e.printStackTrace();
                }

            }
        }

    }


    /**
     * @param ioTokenMap
     * @param label
     * @param subtokenIterator
     * @param token
     * @param begin
     * @param pos
     */
    private void handleCompetingAnnotations(TreeMap<Integer, IOToken> ioTokenMap, String label,
                                            Iterator subtokenIterator, Token token, Integer begin, String pos) {
        // computing length of existing annotation
        int oldLength = 0;
        Set keySet = ioTokenMap.keySet();
        for (Iterator keyIt = keySet.iterator(); keyIt.hasNext(); ) {
            Integer index = (Integer) keyIt.next();
            IOToken actToken = ioTokenMap.get(index);

            if (index >= begin) {
                if (!actToken.getLabel().equals(label) || (!actToken.getIobMark().equals("I") && oldLength > 0)) {
                    break;
                }
                ++oldLength;
            }
        }

        // getting new annotation and it's length by ArrayList.size()
        HashMap<IOToken, Integer> newTokenSeq = new HashMap<IOToken, Integer>();
        IOToken ioToken = new IOBToken(token.getCoveredText(), "B_" + label, pos);
        newTokenSeq.put(ioToken, begin);

        while (subtokenIterator.hasNext()) {
            token = (Token) subtokenIterator.next();
            begin = token.getBegin();
            ioToken = new IOBToken(token.getCoveredText(), "I_" + label, pos);
            newTokenSeq.put(ioToken, begin);
        }

        // if the new sequence is larger than the existing, override the old one
        if (newTokenSeq.size() > oldLength) {
            Set hashKeys = newTokenSeq.keySet();
            for (Iterator hashIt = hashKeys.iterator(); hashIt.hasNext(); ) {
                ioToken = (IOBToken) hashIt.next();
                begin = newTokenSeq.get(ioToken);
                ioTokenMap.put(begin, ioToken);
            }
        }
    }

    /**
     * get the label for a identified annotation. This is done using reflection.
     */
    private String getAnnotationLabel(Annotation ann) {
        String ret = null;

        Class annClass = ann.getClass();
        Method getLabelMethod = null;
        String methodName = objNameMethMap.get(annClass.getName());

        try {
            if (methodName == null) {
                ret = annClass.getName();
            } else {
                getLabelMethod = annClass.getMethod(methodName);
                ret = (String) getLabelMethod.invoke(ann, (Object[]) null);
            }
        } catch (NoSuchMethodException e) {
            LOGGER.error("The class \"" + annClass.getName() + "\" does not have a method \"" + methodName + "\".");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // perhaps a label was found but is part of the blacklist?
        if (ret != null) {
            for (String label : labels) {
                if (ret.equals(label)) {
                    ret = null;
                }
            }
        }

        // checking if the potentially found label is supposed to get a special name in the output
        // file
        if (ret != null && labelIOBMap.get(ret) != null) {
            ret = labelIOBMap.get(ret);
        }

        return ret;
    }

    private String getDocumentId(JCas cas) {
        Header header = null;
        try {
            header = (Header) cas.getAnnotationIndex(Header.type).iterator().next();
        } catch (NoSuchElementException e) {
            LOGGER.trace("No annotation of type {} found in current CAS", Header.class.getCanonicalName());
        }
        if (header != null) {
            return header.getDocId();
        }
        return String.valueOf(id++);
    }
}
