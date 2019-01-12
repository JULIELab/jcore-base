package lingscope.algorithms;

import java.util.HashSet;
import java.util.Set;
import lingscope.structures.AnnotatedSentence;

/**
 *
 * @author shashank
 */
public class BaselineCueAnnotator extends BaselineAnnotator {

    public BaselineCueAnnotator(String beginTag, String interTag, String otherTag) {
        super(beginTag, interTag, otherTag);
    }

    @Override
    public AnnotatedSentence annotateSentence(String sentence, boolean isTokenized) {
        if (phrases == null) {
            throw new RuntimeException("Annotator has not been loaded");
        }
        if (!isTokenized) {
            sentence = AbnerTokenizer.splitTermsByPunctuation(sentence);
        }
        String lcSentence = sentence.toLowerCase();
        String[] words = sentence.split(" +");
        int wordsLength = words.length;

        Set<Integer> addITag = new HashSet<Integer>();
        Set<Integer> addBTag = new HashSet<Integer>();

        // Collect all indices where beginning and intermediate tags should
        // be added
        for (String phrase : phrases) {
            if (!lcSentence.contains(phrase)) {
                continue;
            }
            String[] phraseWords = phrase.split(" +");

            for (int wordCounter = 0; wordCounter < wordsLength; ++wordCounter) {
                String word = words[wordCounter];
                if (word.equalsIgnoreCase(phraseWords[0])) {
                    boolean phraseMatches = true;
                    for (int j = 0; j < phraseWords.length; ++j) {
                        int i = j + wordCounter;
                        if (i == wordsLength) {
                            phraseMatches = false;
                            break;
                        }
                        if (!phraseWords[j].equalsIgnoreCase(words[i])) {
                            phraseMatches = false;
                            break;
                        }
                    }

                    if (phraseMatches) {
                        addBTag.add(wordCounter);
                        for (int j = 1; j < phraseWords.length; ++j) {
                            addITag.add(j + wordCounter);
                        }
                    }
                }
            }
        }

        // Create a tagged sentence. Give preference to beginning tag over
        // intermediate tag in case they clash
        StringBuilder taggedSentence = new StringBuilder();
        for (int i = 0; i < wordsLength; ++i) {
            String word = words[i];
            if (addBTag.contains(i)) {
                taggedSentence.append(" ").append(word).append("|").append(beginTag);
            } else if (addITag.contains(i)) {
                taggedSentence.append(" ").append(word).append("|").append(interTag);
            } else {
                taggedSentence.append(" ").append(word).append("|").append(otherTag);
            }
        }

        String raw = taggedSentence.substring(1);
        return new AnnotatedSentence(raw);
    }

}
