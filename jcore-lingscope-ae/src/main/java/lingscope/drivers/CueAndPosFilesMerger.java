package lingscope.drivers;

import generalutils.FileOperations;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lingscope.io.AnnotatedSentencesIO;
import lingscope.structures.AnnotatedSentence;

/**
 * Merges two files, one containing pos tags and the other containing
 * annotated cues
 * @author shashank
 */
public class CueAndPosFilesMerger {

    public static AnnotatedSentence merge(AnnotatedSentence cueSentence, String posSentence, boolean replaceTags) {
        String[] posTags = posSentence.split("\\s+");
        List<String> crfTags = cueSentence.getTags();
        List<String> words = cueSentence.getWords();
        List<Boolean> tagStatusList = cueSentence.getIsAnnotatedTags();
        StringBuilder mergedSentence = new StringBuilder();
        int numWords = posTags.length;
        for (int j = 0; j < numWords; ++j) {
            mergedSentence.append(" ");
            String posTag = posTags[j];
            String word = words.get(j);
            String crfTag = crfTags.get(j);
            boolean tagStatus = tagStatusList.get(j);
            if (tagStatus) {
                if (replaceTags) {
                    mergedSentence.append("CUE|");
                } else {
                    mergedSentence.append(word).append("|");
                }
            } else {
                mergedSentence.append(posTag).append("|");
            }
            mergedSentence.append(crfTag);
        }

        AnnotatedSentence mergedAnnotatedSentence = new AnnotatedSentence(mergedSentence.substring(1));
        return mergedAnnotatedSentence;
    }

    /**
     * Merges the cueSentences and posSentences
     * @param cueSentences
     * @param posSentences
     * @param replaceTags
     * @return
     */
    public static List<AnnotatedSentence> merge(List<AnnotatedSentence> cueSentences, List<String> posSentences, boolean replaceTags) {
        List<AnnotatedSentence> mergedSentences = new ArrayList<AnnotatedSentence>(cueSentences.size());
        int numSentences = posSentences.size();
        for (int i = 0; i < numSentences; ++i) {
            AnnotatedSentence cueSentence = cueSentences.get(i);
            String posSentence = posSentences.get(i);
            AnnotatedSentence mergedAnnotatedSentence = merge(cueSentence, posSentence, replaceTags);
            mergedSentences.add(mergedAnnotatedSentence);
        }
        return mergedSentences;
    }

    /**
     * 
     * @param args
     * 0 - cue input file
     * 1 - pos input file
     * 2 - replace cue with custom tag 'CUE' (true) or leave it as it is (false)
     * 3 - merged file output path
     */
    public static void main(String[] args) {
        boolean replaceTags = Boolean.parseBoolean(args[2]);
        List<AnnotatedSentence> cueSentences = AnnotatedSentencesIO.read(args[0]);
        List<String> posSentences = null;
        try {
            posSentences = FileOperations.readFile(args[1]);
        } catch (Exception ex) {
            Logger.getLogger(CueAndPosFilesMerger.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        AnnotatedSentencesIO.write(args[3], merge(cueSentences, posSentences, replaceTags));
    }
}
