package lingscope.drivers;

import generalutils.FileOperations;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lingscope.algorithms.Annotator;
import lingscope.algorithms.BaselineCueAnnotator;
import lingscope.algorithms.BaselineScopeAnnotator;
import lingscope.algorithms.CrfAnnotator;
import lingscope.algorithms.NegexCueAnnotator;
import lingscope.algorithms.NegexScopeAnnotator;
import lingscope.io.AnnotatedSentencesIO;
import lingscope.structures.AnnotatedSentence;

/**
 * Tags scope or cue in a single sentence
 * @author shashank
 */
public class SentenceTagger {

    public static final String CUE_START = "B-C";
    public static final String CUE_INTER = "I-C";
    public static final String SCOPE_START = "B-S";
    public static final String SCOPE_INTER = "I-S";
    public static final String OTHER = "O";

    /**
     * Tags the given sentence with the given annotator
     * @param annotator
     * @param sentence
     * @param isTokenized
     * @return
     */
    public static AnnotatedSentence tag(Annotator annotator, String sentence, boolean isTokenized) {
        return annotator.annotateSentence(sentence, isTokenized);
    }

    /**
     * Prints the usage for the sentence tagger
     */
    public static void usage() {
        System.out.println("Usage:\njava lingscope.drivers.SentenceTagger (cue|scope) (crf|baseline|negex) saved_model_file sentence_to_tag");
        System.out.println("\tSaved model for negation can be obtained from http://negscope.askhermes.org/");
        System.out.println("\tSaved model for speculation can be obtained from http://hedgescope.askhermes.org/");
        System.out.println("\tSaved model for NegEx can be obtained from http://code.google.com/p/negex/downloads/list");
    }

    /**
     * Given a list of annotated sentences, return a list where the annotated 
     * sentences are replaced with strings
     * @param annotatedSentences
     * @return
     */
    public static List<String> getStringListFromAnnotatedSentences(List<AnnotatedSentence> annotatedSentences) {
        List<String> strings = new ArrayList<String>(annotatedSentences.size());
        for (AnnotatedSentence annotatedSentence : annotatedSentences) {
            strings.add(annotatedSentence.getSentenceText());
        }
        return strings;
    }

    public static List<AnnotatedSentence> annotateSentences(Annotator annotator, List<String> inputSentences, boolean isTokenized) {
        List<AnnotatedSentence> outputSentences = new ArrayList<AnnotatedSentence>(inputSentences.size());
        for (String inputSentence : inputSentences) {
            AnnotatedSentence outputSentence = annotator.annotateSentence(inputSentence, isTokenized);
            outputSentences.add(outputSentence);
        }
        return outputSentences;
    }

    /**
     * Gets the list of sentences in string format from the given inputFile.
     * @param inputFile the file containing input sentences
     * @param isAnnotated set this as true if the input file contains annotated
     * sentences. Set this as false if the input file contains only sentences
     * as strings
     * @return the list of sentences in string format in the given inputFile
     */
    public static List<String> getStringList(String inputFile, boolean isAnnotated) {
        if (!isAnnotated) {
            try {
                return FileOperations.readFile(inputFile);
            } catch (Exception ex) {
                Logger.getLogger(SentenceTagger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        List<AnnotatedSentence> annotatedSentences = AnnotatedSentencesIO.read(inputFile);
        return getStringListFromAnnotatedSentences(annotatedSentences);
    }

    /**
     * Gets the Annotator from the given classifier and mark type
     * @param classifierType such as "baseline", "crf", or "negex"
     * @param markType
     * @return
     */
    public static Annotator getAnnotator(String classifierType, String markType) {
        if ("cue".equalsIgnoreCase(markType)) {
            if ("baseline".equalsIgnoreCase(classifierType)) {
                return new BaselineCueAnnotator(CUE_START, CUE_INTER, OTHER);
            } else if ("crf".equalsIgnoreCase(classifierType)) {
                return new CrfAnnotator(CUE_START, CUE_INTER, OTHER);
            } else if ("negex".equalsIgnoreCase(classifierType)) {
                return new NegexCueAnnotator(CUE_START, CUE_INTER, OTHER);
            }
            return null;
        } else if ("scope".equalsIgnoreCase(markType)) {
            if ("baseline".equalsIgnoreCase(classifierType)) {
                return new BaselineScopeAnnotator(SCOPE_START, SCOPE_INTER, OTHER, true, true);
            } else if ("crf".equalsIgnoreCase(classifierType)) {
                return new CrfAnnotator(SCOPE_START, SCOPE_INTER, OTHER);
            } else if ("negex".equalsIgnoreCase(classifierType)) {
                return new NegexScopeAnnotator(SCOPE_START, SCOPE_INTER, OTHER);
            }
            return null;
        }
        return null;
    }

    /**
     *
     * @param args
     * 0 - Annotator type ("cue" or "scope")
     * 1 - Model type ("crf", "baseline" or "negex")
     * 2 - Saved model file
     * 3 - Sentence to tag
     */
    public static void main(String[] args) {
        if (args.length < 4) {
            usage();
            System.exit(0);
        }
        Annotator annotator = getAnnotator(args[1], args[0]);
        if (annotator == null) {
            usage();
            System.exit(1);
        }
        annotator.loadAnnotator(args[2]);
        
        if ("file".equalsIgnoreCase(args[3])) {
            String fileName = args[4];
            try {
                List<String> sentences = FileOperations.readFile(fileName);
                for (String sentence : sentences) {
                    AnnotatedSentence annotatedSentence = tag(annotator, sentence, false);
                    System.out.println(annotatedSentence.getRawText());
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        } else {
            AnnotatedSentence sentence = tag(annotator, args[3], false);
            System.out.println(sentence.getRawText());
        }
    }
}
