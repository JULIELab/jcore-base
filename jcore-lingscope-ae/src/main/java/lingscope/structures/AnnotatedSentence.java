package lingscope.structures;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an AnnotatedSentence
 * @author shashank
 */
public class AnnotatedSentence {

    public static String nonScopeTag = "O";

    private String rawText;
    private List<String> words;
    private List<String> tags;
    private List<Boolean> isAnnotatedTags;

    /**
     * Initializes an AnnotatedSentence object
     */
    public AnnotatedSentence() {
        words = new ArrayList<String>();
        tags = new ArrayList<String>();
        isAnnotatedTags = new ArrayList<Boolean>();
    }

    /**
     * Creates an object of AnnotatedSentence with the given rawText
     * @param rawText
     */
    public AnnotatedSentence(String rawText) {
        this();
        processRawText(rawText);
    }

    /**
     * Processes the given raw text and extracts tags from it
     * @param rawText
     */
    public final void processRawText(String rawText) {
        this.rawText = rawText;
        words.clear();
        tags.clear();
        isAnnotatedTags.clear();
        String[] tokens = rawText.split("\\s+");
        for (String token : tokens) {
            String[] wordTag = token.split("\\|");
            words.add(wordTag[0]);
            tags.add(wordTag[1]);
            if (nonScopeTag.equalsIgnoreCase(wordTag[1])) {
                isAnnotatedTags.add(Boolean.FALSE);
            } else {
                isAnnotatedTags.add(Boolean.TRUE);
            }
        }
    }

    /**
     * Gets the is annotated tags for this AnnotatedSentence
     * @return
     */
    public List<Boolean> getIsAnnotatedTags() {
        return isAnnotatedTags;
    }

    /**
     * Gets the raw text (with tags) for this AnnotatedSentence
     * @return
     */
    public String getRawText() {
        return rawText;
    }

    /**
     * Gets the tags in this AnnotatedSentence
     * @return
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Gets the words in this AnnotatedSentence
     * @return
     */
    public List<String> getWords() {
        return words;
    }

    /**
     * Gets the sentence text (without the tags)
     * @return
     */
    public String getSentenceText() {
        StringBuilder sentenceText = new StringBuilder();
        for (String word : words) {
            sentenceText.append(" ").append(word);
        }
        if (sentenceText.length() == 0) {
            return "";
        }
        return sentenceText.substring(1);
    }
}
