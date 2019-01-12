package lingscope.drivers;

import generalutils.FileOperations;
import java.util.List;
import lingscope.algorithms.Annotator;
import lingscope.structures.AnnotatedSentence;

/**
 * Use this sentence tagger when using a model that tags POS
 * @author shashank
 */
public class SentencePosTagger {

    public static void usage() {
        System.out.println("java lingscope.drivers.SentencePosTagger cue_tagging_model "
                + "cue_tagger_type(baseline|crf|negex) "
                + "replace_cue_with_custom_tag(true|false) scope_tagging_model "
                + "pos_model_file sentence_to_tag");
        System.out.println("\tSaved model for negation can be obtained from http://negscope.askhermes.org/");
        System.out.println("\tSaved model for speculation can be obtained from http://hedgescope.askhermes.org/");
        System.out.println("\tSaved model for NegEx can be obtained from http://code.google.com/p/negex/downloads/list");
        System.out.println("\tSaved pos_model_file can be obtained from http://hedgescope.askhermes.org/");
    }

    /**
     * 
     * @param args
     * 0 - cue tagging model
     * 1 - cue tagger type (baseline, crf or negex)
     * 2 - replace cue words with custom tag CUE (true) or not (false)
     * 3 - crf pos-based scope tagging model
     * 4 - POS model file
     * 5 - sentence to tag
     */
    public static void main(String[] args) {
        if (args[0].equalsIgnoreCase("help")) {
            usage();
            System.exit(0);
        } else if (args.length < 6) {
            usage();
            System.exit(1);
        }
        Annotator cueAnnotator = SentenceTagger.getAnnotator(args[1], "cue");
        cueAnnotator.loadAnnotator(args[0]);
        Annotator scopeAnnotator = SentenceTagger.getAnnotator("crf", "scope");
        scopeAnnotator.loadAnnotator(args[3]);
        String sentence = args[5];
        String grammarFile = args[4];
        
        if ("file".equalsIgnoreCase(sentence)) {
            String sentencesFile = args[6];
            try {
                List<String> sentences = FileOperations.readFile(sentencesFile);
                for (String sentenceText : sentences) {
                    tagSentence(sentenceText, grammarFile, 
                            Boolean.parseBoolean(args[2]), cueAnnotator, 
                            scopeAnnotator);
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        } else {
            tagSentence(sentence, grammarFile, Boolean.parseBoolean(args[2]), 
                    cueAnnotator, scopeAnnotator);
        }
    }

    /**
     * Tags the given sentence
     * @param sentence the text of the sentence to tag
     * @param grammarFile path to the Stanford part of speech model file
     * @param replaceCueWords if true, cue words will be replaced with custom 
     * tag 'CUE'
     * @param cueAnnotator the {@link Annotator} object to identify negation or 
     * hedge cue in the sentence
     * @param scopeAnnotator the {@link Annotator} object to identify negation 
     * or hedge scope in the sentence
     */
    public static void tagSentence(String sentence, String grammarFile, 
            boolean replaceCueWords, Annotator cueAnnotator, Annotator scopeAnnotator) {
        String posSentence = PosTaggerDriver.getTaggedSentence(grammarFile, sentence, false);
        AnnotatedSentence cueTaggedSentence = cueAnnotator.annotateSentence(sentence, false);
        AnnotatedSentence posCueMerged = CueAndPosFilesMerger.merge(cueTaggedSentence, posSentence, replaceCueWords);
        AnnotatedSentence scopeMarkedSentence = scopeAnnotator.annotateSentence(posCueMerged.getSentenceText(), true);
        AnnotatedSentence scopeWordsMarkedSentence = AnnotatedFilesMerger.merge(cueTaggedSentence, scopeMarkedSentence);
        System.out.println(scopeWordsMarkedSentence.getRawText());
        
    }
}
