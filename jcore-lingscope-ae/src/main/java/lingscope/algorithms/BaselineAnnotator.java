package lingscope.algorithms;

import generalutils.FileOperations;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The baseline annotator
 * @author shashank
 */
public abstract class BaselineAnnotator extends Annotator {

    protected Set<String> phrases;

    public BaselineAnnotator(String beginTag, String interTag, String otherTag) {
        super(beginTag, interTag, otherTag);
        phrases = null;
    }

    @Override
    public void serializeAnnotator(String trainingFile, String modelFile) {
        try {
            phrases = new HashSet<String>();
            List<String> taggedSentences = FileOperations.readFile(trainingFile);
            for (String taggedSentence : taggedSentences) {
                phrases.addAll(getCueWords(taggedSentence, beginTag, interTag, otherTag));
            }
            FileOperations.writeFile(modelFile, new ArrayList<String>(phrases));
        } catch (Exception ex) {
            Logger.getLogger(BaselineAnnotator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void loadAnnotator(String modelFile) {
        try {
            phrases = new HashSet<String>();
            phrases.addAll(FileOperations.readFile(modelFile));
        } catch (Exception ex) {
            Logger.getLogger(BaselineAnnotator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gets the set of cue word phrases in the given sentence. The given
     * sentence is tagged according to Abner's specifications
     * @param abnerTaggedSentence sentence tagged by abner's specification.
     * @param beginTag the tag to mark the beginning of the cue
     * @param intermediateTag the tag to mark intermediate portions
     * @param otherTag the other tag
     * @return the set of cue words or phrases in the given sentence
     */
    public static Set<String> getCueWords(String abnerTaggedSentence, String beginTag, String intermediateTag, String otherTag) {
        Set<String> cueWordsPhrases = new HashSet<String>(1);
        String[] elements = abnerTaggedSentence.split(" +");
        boolean collect = false;
        StringBuilder collectedPhrase = new StringBuilder();
        for (String element : elements) {
            String[] elementTokens = element.split("\\|");
            String word = elementTokens[0];
            String tag = elementTokens[1];
            if (tag.equalsIgnoreCase(beginTag)) {
                collect = true;
                collectedPhrase.append(word).append(" ");
            } else if (tag.equalsIgnoreCase(intermediateTag)) {
                collectedPhrase.append(word).append(" ");
            } else if (tag.equalsIgnoreCase(otherTag) && collect) {
                collect = false;
                cueWordsPhrases.add(collectedPhrase.toString().trim().toLowerCase());
                collectedPhrase.delete(0, collectedPhrase.length() - 1);
            }
        }
        return cueWordsPhrases;
    }

}
