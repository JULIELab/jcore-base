package lingscope.algorithms;

import java.util.List;
import lingscope.structures.AnnotatedSentence;

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

    public abstract void loadAnnotator(String modelFile);

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
