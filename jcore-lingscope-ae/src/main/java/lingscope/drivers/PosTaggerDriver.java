package lingscope.drivers;

import generalutils.FileOperations;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lingscope.algorithms.PosTagger;

/**
 * Driver for the Part of Speech tagger. Use this to tag all sentences in a
 * given file with part of speech tags
 * @author shashank
 */
public class PosTaggerDriver {

    private static PosTagger posTagger = null;

    /**
     * Gets the equivalent POS sentence for the given sentenceToTag
     * @param grammerFile file containing POS model
     * @param sentenceToTag sentence for which POS tags will be returned
     * @param is
     * @return a string where the words from sentenceToTag are replaced with
     * corresponding part of speech tags
     */
    public static String getTaggedSentence(String grammerFile, String sentenceToTag, boolean isTokenized) {
        if (posTagger == null) {
            posTagger = new PosTagger(grammerFile);
        }
        List<String> posTags = posTagger.replaceWordsWithPos(sentenceToTag, isTokenized);
        StringBuilder posSentence = new StringBuilder();
        for (String posTag : posTags) {
            posSentence.append(" ").append(posTag);
        }
        return posSentence.substring(1);
    }

    /**
     *
     * @param args
     * 0 - file containing the part of speech model
     * 1 - input file
     * 2 - output file
     * 3 - (boolean) does the input file contain annotated sentence (true) or
     * not (false)
     */
    public static void main(String[] args) {
        String grammarFile = args[0];
        List<String> inputSentences = SentenceTagger.getStringList(args[1], Boolean.parseBoolean(args[3]));
        List<String> outputSentences = new ArrayList<String>(inputSentences.size());
        for (String inputSentence : inputSentences) {
            String outputSentence = getTaggedSentence(grammarFile, inputSentence, Boolean.parseBoolean(args[3]));
            outputSentences.add(outputSentence);
        }
        try {
            FileOperations.writeFile(args[2], outputSentences);
        } catch (Exception ex) {
            Logger.getLogger(PosTaggerDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
