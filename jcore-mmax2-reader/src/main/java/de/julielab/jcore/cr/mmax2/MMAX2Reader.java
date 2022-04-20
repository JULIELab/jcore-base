package de.julielab.jcore.cr.mmax2;

import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jules.mmax.MarkableContainer;
import de.julielab.jules.mmax.Statistics;
import de.julielab.jules.mmax.WordInformation;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.discourse.MMAX2Discourse;
import org.eml.MMAX2.discourse.MMAX2DiscourseElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ResourceMetaData(name = "JCoRe MMAX2 reader", description = "Collection reader for MMAX2 annotation projects.", vendor = "JULIE Lab Jena, Germany")
public class MMAX2Reader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_INPUT_DIR = "InputDir";
    public static final String PARAM_ANNOTATION_LEVELS = "AnnotationLevels";
    public static final String PARAM_ORIGINAL_TEXT_FILES = "OriginalTextFiles";
    public static final String PARAM_UIMA_ANNOTATION_TYPES = "UimaAnnotationTypes";
    private final static Logger log = LoggerFactory.getLogger(MMAX2Reader.class);
    @ConfigurationParameter(name = PARAM_INPUT_DIR, description = "Should point to the directory of which the MMAX2 projects are sub directories of.")
    private String inputDir;
    @ConfigurationParameter(name = PARAM_ANNOTATION_LEVELS, description = "The names of the MMAX2 annotation levels to create annotations for.")
    private String[] annotationLevels;
    @ConfigurationParameter(name = PARAM_UIMA_ANNOTATION_TYPES, description = "The fully qualified names of the UIMA annotation types to be used for the representation of the input annotation level. Must match the indices of " + PARAM_ANNOTATION_LEVELS + ", i.e. the ith level will be added to the CAS as the ith type.")
    private String[] uimaTypeNames;
    @ConfigurationParameter(name = PARAM_ORIGINAL_TEXT_FILES, mandatory = false, description = "The MMAX2 base data consists of tokenized text and does not keep track of the original text. This parameter should point to a directory containing the original text files. The file names should match the MMAX2 project IDs.")
    private String originalTextFilesDir;

    private LinkedList<File> folderList;
    private String actualPath;
    private HashMap<String, String> levels2uimaNames;
    private List<Class<?>> uimaAnnotationClasses;
    private int numDocuments;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        inputDir = (String) context.getConfigParameterValue(PARAM_INPUT_DIR);
        annotationLevels = (String[]) context.getConfigParameterValue(PARAM_ANNOTATION_LEVELS);
        uimaTypeNames = (String[]) getUimaContext().getConfigParameterValue(PARAM_UIMA_ANNOTATION_TYPES);
        originalTextFilesDir = (String) context.getConfigParameterValue(PARAM_ORIGINAL_TEXT_FILES);
        actualPath = null;
        if (annotationLevels.length != uimaTypeNames.length)
            throw new IllegalArgumentException("The number of annotation levels and the number of UIMA type names must match. But the given annotation levels are '" + Arrays.toString(annotationLevels) + "' and the UIMA types names are '" + Arrays.toString(uimaTypeNames) + "'.");
        try {
            uimaAnnotationClasses = Arrays.stream(uimaTypeNames).map(name -> {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Could not initialize UIMA annotation classes from parameter values {}", Arrays.toString(uimaTypeNames));
            throw new ResourceInitializationException(e);
        }
        levels2uimaNames = IntStream.range(0, annotationLevels.length).collect(HashMap::new, (m, i) -> m.put(annotationLevels[i], uimaTypeNames[i]), (m1, m2) -> m1.putAll(m2));
        setUpFolderList();
    }

    private void setUpFolderList() throws ResourceInitializationException {
        folderList = new LinkedList<>();
        if (!inputDir.endsWith(File.separator))
            this.inputDir += File.separator;

        File rootX = new File(inputDir);

        if (!rootX.exists()) {
            File dir1 = new File(".");
            try {
                rootX = new File(dir1.getCanonicalPath() + inputDir);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            if (!rootX.exists()) {
                log.error("{} does not exist", inputDir);
                throw new ResourceInitializationException(new IllegalArgumentException(inputDir + " does not exist"));
            }
        }

        for (String rootFolder : rootX.list()) {
            if (!rootFolder.endsWith(File.separator))
                rootFolder += File.separator;
            File root = new File(inputDir + rootFolder);
            if (root.isDirectory()) {
                this.folderList.add(root);
            }
        }
        numDocuments = folderList.size();
    }

    private String getPMID() throws CollectionException {
        try {
            FileInputStream fstream = new FileInputStream(this.actualPath + "Basedata.uri");
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            // Read File Line By Line
            int count = 0;
            String pmid = "";
            while ((strLine = br.readLine()) != null) {
                count++;
                pmid = strLine;
            }
            if (count > 1) {
                log.error("unknown data in {}Basedata.uri", actualPath);
                System.exit(1);
                return null;
            }
            return pmid;
        } catch (IOException e) {
            log.error("Error while parsing {}Basedata.uri", actualPath);
            throw new CollectionException(e);
        }
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void getNext(JCas jCas) throws CollectionException {
        Statistics.projects++;
        actualPath = this.folderList.poll().getAbsolutePath() + "/";
        // rename style file from default_style.xsl to generic_nongui_style.xsl
        // (necessary for api use)
        File style = new File(actualPath + "Styles/default_style.xsl");
        style.renameTo(new File(actualPath + "Styles/generic_nongui_style.xsl"));

        File mmaxfile = new File(actualPath + "project.mmax");
        MMAX2Discourse discourse = MMAX2Discourse.buildDiscourse(mmaxfile.getAbsolutePath());

        // text from basedata with spaces between all words
        String documentText = discourse.getNextDocumentChunk();

        WordInformation[] words = new WordInformation[discourse.getDiscourseElementCount()];

        int textPosition = 0;
        // Words from basedata
        for (MMAX2DiscourseElement elem : discourse.getDiscourseElements()) {
            WordInformation word = new WordInformation();
            word.setId(elem.getID());
            int discoursePosition = elem.getDiscoursePosition();
            word.setPosition(discoursePosition);
            StringBuilder textBuilder = new StringBuilder();
            int end = discourse.getDisplayEndPositionFromDiscoursePosition(discoursePosition);
            for (textPosition = discourse.getDisplayStartPositionFromDiscoursePosition(discoursePosition); textPosition <= end; textPosition++) {
                textBuilder.append(documentText.charAt(textPosition));
            }
            word.setText(textBuilder.toString());
            words[discoursePosition] = word;
        }

        this.produceOutput(discourse, words, jCas);

        // set stylefile back to normal
        style = new File(actualPath + "Styles/generic_nongui_style.xsl");
        style.renameTo(new File(actualPath + "Styles/default_style.xsl"));

        Statistics.projects++;
    }

    private void produceOutput(MMAX2Discourse discourse, WordInformation[] words, JCas jCas) throws CollectionException {
        StringBuilder out = new StringBuilder();
        StringBuilder outPlain = new StringBuilder();
        String pmid = this.getPMID();
        if (originalTextFilesDir != null && this.originalTextFilesDir.length() > 0)
            this.handleOriginalTextInformation(pmid, words);

        Map<Integer, Token> pos2offsets = new HashMap<>();

        for (int i = 0; i < words.length; i++) {
            WordInformation word = words[i];

            Token token = new Token(jCas, outPlain.length(), outPlain.length() + word.getText().length());
            token.setComponentId(getClass().getCanonicalName());
            token.addToIndexes();
            pos2offsets.put(word.getPosition(), token);

            outPlain.append(word.getText());
            if (word.isFollowedBySpace()) {
                out.append(" ");
                outPlain.append(" ");
            }
        }
        for (int i = 0; i < annotationLevels.length; ++i) {
            Iterator<Markable> iterator = discourse.getMarkableLevelByName(annotationLevels[i], false).getMarkables().stream().map(Markable.class::cast).filter(Predicate.not(Markable::isDiscontinuous)).iterator();
            int id = 0;
            while (iterator.hasNext()) {
                Markable markable = iterator.next();
                int beginPosition = markable.getLeftmostDiscoursePosition();
                int endPosition = markable.getRightmostDiscoursePosition();
                int beginOffset = pos2offsets.get(beginPosition).getBegin();
                int endOffset = pos2offsets.get(endPosition).getEnd();
                Annotation a;
                try {
                    a = JCoReAnnotationTools.getAnnotationByClassName(jCas, uimaTypeNames[i]);
                } catch (Exception e) {
                    throw new CollectionException(e);
                }
                a.setBegin(beginOffset);
                a.setEnd(endOffset);
                if (a instanceof ConceptMention)
                    ((ConceptMention) a).setSpecificType(markable.getAttributeValue(markable.getMarkableLevelName()));
                else if (a instanceof Sentence)
                    ((Sentence)a).setId(String.valueOf(id));
                a.addToIndexes();
                ++id;
            }
        }
        for (WordInformation word : words) {
            for (MarkableContainer mc : word.getMarkables()) {
                int beginPosition = mc.getBegin();
                if (beginPosition == word.getPosition()) {
                    int endPosition = mc.getEnd();
                    int beginOffset = pos2offsets.get(beginPosition).getBegin();
                    int endOffset = pos2offsets.get(endPosition).getEnd();
                    Gene gene = new Gene(jCas, beginOffset, endOffset);
                    gene.addToIndexes();
                }
            }
        }
        String textPlain = outPlain.toString();
        jCas.setDocumentText(textPlain);
    }

    private void handleOriginalTextInformation(String pmid, WordInformation[] words) throws CollectionException {
        if (originalTextFilesDir.length() > 0 && !originalTextFilesDir.endsWith("/"))
            originalTextFilesDir += File.separator;

        File file = new File(originalTextFilesDir + pmid);
        if (!file.exists()) {
            log.warn("no original File found for {} using only mmax text.", pmid);
            return;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            int wordCounter = 0;
            int i;
            try {
                WordInformation actualWord = words[wordCounter];
                String actualText = actualWord.getText();
                actualWord.setFollowedBySpace(false);
                int wordCharCounter = 0;
                while ((i = isr.read()) >= 0) {
                    if (wordCharCounter >= actualText.length()) {
                        wordCounter++;
                        if (wordCounter < words.length) {
                            actualWord = words[wordCounter];
                            actualText = actualWord.getText();
                            actualWord.setFollowedBySpace(false);
                            wordCharCounter = 0;
                        } else {
                            if (!Character.isWhitespace(i)) {
                                log.warn("original Text contains more words than mmax information");
                            }
                            return;
                        }
                    }

                    if (actualText.charAt(wordCharCounter) == i || Character.toLowerCase(actualText.charAt(wordCharCounter)) == Character.toLowerCase(i)) {
                        wordCharCounter++;
                    } else {
                        if (!Character.isWhitespace(i)) {
                            log.warn("there is a non whitespace character different in original text at document {} critical character is '{}' near word '{}' (MMAX2 word ID {})", pmid, i, actualText, actualWord.getId());
                        } else {
                            words[wordCounter - 1].setFollowedBySpace(true);
                        }
                    }
                }
                isr.close();
            } catch (IOException e) {
                log.error("Error attempting to read original text file ", e);
                throw new CollectionException(e);
            }
        } catch (Exception e) {
            log.error("Error attempting to read original text file", e);
            if (e instanceof CollectionException)
                throw (CollectionException) e;
            throw new CollectionException(e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(numDocuments - folderList.size(), numDocuments, "document")};
    }

    @Override
    public boolean hasNext() {
        return !this.folderList.isEmpty();
    }


}
