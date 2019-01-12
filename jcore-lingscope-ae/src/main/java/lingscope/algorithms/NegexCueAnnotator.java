package lingscope.algorithms;

import lingscope.structures.AnnotatedSentence;

/**
 *
 * @author shashank
 */
public class NegexCueAnnotator extends NegexAnnotator {

    public NegexCueAnnotator(String beginTag, String interTag, String otherTag) {
        super(beginTag, interTag, otherTag);
    }

    @Override
    public AnnotatedSentence annotateSentence(String sentence, boolean isTokenized) {
        if (negex == null) {
            throw new RuntimeException("Annotator has not been loaded");
        }
        if (!isTokenized) {
            sentence = AbnerTokenizer.splitTermsByPunctuation(sentence);
        }
        String raw = negex.getCue(sentence, rules, beginTag, interTag, otherTag);
        return new AnnotatedSentence(raw);
    }
}
