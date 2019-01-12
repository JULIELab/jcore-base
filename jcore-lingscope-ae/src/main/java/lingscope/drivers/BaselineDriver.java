package lingscope.drivers;

import java.util.List;
import lingscope.algorithms.Annotator;
import lingscope.algorithms.BaselineScopeAnnotator;
import lingscope.io.AnnotatedSentencesIO;
import lingscope.structures.AnnotatedSentence;

/**
 *
 * @author shashank
 */
public class BaselineDriver {

    /**
     *
     * @param args
     * 0 - Annotator type ("cue" or "scope")
     * 1 - Serialized annotator file
     * 2 - Input file
     * 3 - Output file
     * 4 - if annotator type is "scope", then should scope be limited by commas
     * 5 - if annotator type is "scope", then should scope be limited by periods
     * 6 - (boolean) does the input file contain annotated sentence (true) or
     * not (false)
     */
    public static void main(String[] args) {
        Annotator annotator;
        
        if (args[0].equalsIgnoreCase("scope")) {
            annotator = new BaselineScopeAnnotator(SentenceTagger.SCOPE_START,
                    SentenceTagger.SCOPE_INTER, SentenceTagger.OTHER,
                    Boolean.parseBoolean(args[4]), Boolean.parseBoolean(args[5]));
        } else {
            annotator = SentenceTagger.getAnnotator("baseline", args[0]);
        }
        annotator.loadAnnotator(args[1]);

        boolean isAnnotated = Boolean.parseBoolean(args[6]);
        List<String> inputSentences = SentenceTagger.getStringList(args[2], isAnnotated);
        List<AnnotatedSentence> outputSentences = SentenceTagger.annotateSentences(annotator, inputSentences, isAnnotated);
        AnnotatedSentencesIO.write(args[3], outputSentences);

    }
}
