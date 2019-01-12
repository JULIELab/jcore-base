package lingscope.drivers;

import java.util.List;
import lingscope.algorithms.Annotator;
import lingscope.io.AnnotatedSentencesIO;
import lingscope.structures.AnnotatedSentence;

/**
 * Driver to mark scope or cue in a file
 * @author shashank
 */
public class CrfDriver {
    /**
     *
     * @param args
     * 0 - Annotator type ("cue" or "scope")
     * 1 - Serialized annotator file
     * 2 - Input file
     * 3 - Output file
     * 4 - (boolean) does the input file contain annotated sentence (true) or
     * not (false)
     */
    public static void main(String[] args) {
        Annotator annotator = SentenceTagger.getAnnotator("crf", args[0]);
        annotator.loadAnnotator(args[1]);
        boolean isAnnotated = Boolean.parseBoolean(args[4]);
        List<String> inputSentences = SentenceTagger.getStringList(args[2], isAnnotated);
        List<AnnotatedSentence> outputSentences = SentenceTagger.annotateSentences(annotator, inputSentences, isAnnotated);
        AnnotatedSentencesIO.write(args[3], outputSentences);
    }
}
