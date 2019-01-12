package lingscope.algorithms;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part of speech tagger
 * @author shashank
 */
public class PosTagger {

    private MaxentTagger posTagger;

    /**
     * Creates an instance of POS tagger by loading the given grammar file
     * @param grammarFile
     */
    public PosTagger(String grammarFile) {
        try {
            posTagger = new MaxentTagger(grammarFile);
        } catch (Exception ex) {
            Logger.getLogger(PosTagger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Takes a sentence as input and returns list of POS tags associated with
     * each word in the sentence
     * @param sentence
     * @param isTokenized
     * @return
     */
    public List<String> replaceWordsWithPos(String sentence, boolean isTokenized) {
        if (!isTokenized) {
            sentence = AbnerTokenizer.splitTermsByPunctuation(sentence);
        }
        List<String> ret = new ArrayList<String>();
        String tagged = "";
        try {
            tagged = posTagger.tagString(sentence);
        } catch (Exception ex) {
            Logger.getLogger(PosTagger.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (String wordTag : tagged.split(" +")) {
            String[] tags = wordTag.split("/");
            String tag = tags[tags.length - 1];
            ret.add(tag);
        }
        return ret;
    }
}
