package lingscope.algorithms;

import lingscope.structures.AnnotatedSentence;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 *
 * @author shashank
 */
public abstract class Annotator {

    protected String beginTag;
    protected String interTag;
    protected String otherTag;

    public Annotator(String beginTag, String interTag, String otherTag) {
        this.beginTag = beginTag;
        this.interTag = interTag;
        this.otherTag = otherTag;
    }

    public abstract void serializeAnnotator(String trainingFile, String modelFile);

    public abstract AnnotatedSentence annotateSentence(String sentence, boolean isTokenized);

    public void loadAnnotator(String modelFile) {
        try (FileInputStream fis = new FileInputStream(modelFile)) {
            loadAnnotator(fis);
        }  catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void loadAnnotator(InputStream is);

    /**
     * Checks if the given target phrase is negated in the given sentence. Only
     * the first word of the target phrase is used
     * @param sentence
     * @param isTokenized
     * @param targetPhrase
     * @return
     */
    public boolean isTargetNegated(String sentence, boolean isTokenized, String targetPhrase) {
        AnnotatedSentence annotatedSentence = annotateSentence(sentence, isTokenized);
        String[] targetPhraseWords = targetPhrase.split("\\s+");
        List<String> words = annotatedSentence.getWords();
        List<Boolean> areNegated = annotatedSentence.getIsAnnotatedTags();
        int index = 0;
        for (String word : words) {
            if (targetPhraseWords[0].equalsIgnoreCase(word)) {
                return areNegated.get(index);
            }
            ++index;
        }
        System.err.println("Phrase not found: " + targetPhrase);
        return false;
    }
}
