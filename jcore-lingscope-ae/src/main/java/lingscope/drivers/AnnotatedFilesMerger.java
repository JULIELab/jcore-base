package lingscope.drivers;

import java.util.ArrayList;
import java.util.List;
import lingscope.io.AnnotatedSentencesIO;
import lingscope.structures.AnnotatedSentence;

/**
 * Merges two annotated files. Useful to merge a words scope file with a POS cue
 * file
 * @author shashank
 */
public class AnnotatedFilesMerger {

    /**
     * Merges the given wordsSentence and the given tagsSentence
     * @param wordsSentence
     * @param tagsSentence
     * @return
     */
    public static AnnotatedSentence merge(AnnotatedSentence wordsSentence, AnnotatedSentence tagsSentence) {
        List<String> words = wordsSentence.getWords();
        List<String> tags = tagsSentence.getTags();
        int numTokens = words.size();
        if (tags.size() != numTokens) {
            System.err.println("Skipping non-equal length sentences");
            System.err.println("\tSentence 1: " + wordsSentence.getRawText());
            System.err.println("\tSentence 2: " + tagsSentence.getRawText());
            return null;
        }
        StringBuilder mergedSentence = new StringBuilder();
        for (int j = 0; j < numTokens; ++j) {
            mergedSentence.append(" ").append(words.get(j)).append("|").append(tags.get(j));
        }
        return new AnnotatedSentence(mergedSentence.substring(1));
    }

    /**
     *
     * @param args
     * 0 - file 1: the file from which words will be taken
     * 1 - file 2: the file from which tags will be taken
     * 2 - output file path
     */
    public static void main(String[] args) {
        List<AnnotatedSentence> wordsSentences = AnnotatedSentencesIO.read(args[0]);
        List<AnnotatedSentence> tagsSentences = AnnotatedSentencesIO.read(args[1]);
        int numSentences = tagsSentences.size();
        List<AnnotatedSentence> mergedSentences = new ArrayList<AnnotatedSentence>(numSentences);
        for (int i = 0; i < numSentences; ++i) {
            AnnotatedSentence wordsSentence = wordsSentences.get(i);
            AnnotatedSentence tagsSentence = tagsSentences.get(i);
            AnnotatedSentence mergedSentence = merge(wordsSentence, tagsSentence);
            if (mergedSentence == null) {
                continue;
            }
            mergedSentences.add(mergedSentence);
        }
        AnnotatedSentencesIO.write(args[2], mergedSentences);
    }
}
