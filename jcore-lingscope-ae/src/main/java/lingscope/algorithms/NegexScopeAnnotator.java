package lingscope.algorithms;

import lingscope.structures.AnnotatedSentence;

/**
 *
 * @author shashank
 */
public class NegexScopeAnnotator extends NegexAnnotator {

    public NegexScopeAnnotator(String beginTag, String interTag, String otherTag) {
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
        String raw = negex.getScope(sentence, rules, beginTag, interTag, otherTag);
        return new AnnotatedSentence(raw);
    }
}
