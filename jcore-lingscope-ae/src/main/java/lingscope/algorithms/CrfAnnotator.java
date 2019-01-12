package lingscope.algorithms;

import abner.Tagger;
import abner.Trainer;
import java.io.File;
import lingscope.structures.AnnotatedSentence;

/**
 * A CRF based annotator
 * @author shashank
 */
public class CrfAnnotator extends Annotator {

    private Tagger tagger;

    public CrfAnnotator(String beginTag, String interTag, String otherTag) {
        super(beginTag, interTag, otherTag);
        tagger = null;
    }

    @Override
    public void serializeAnnotator(String trainingFile, String modelFile) {
        Trainer trainer = new Trainer();
        trainer.train(trainingFile, modelFile);
        loadAnnotator(modelFile);
    }

    @Override
    public AnnotatedSentence annotateSentence(String sentence, boolean isTokenized) {
        if (tagger == null) {
            throw new RuntimeException("Tagger has not been loaded");
        }
        if (!isTokenized) {
            sentence = AbnerTokenizer.splitTermsByPunctuation(sentence);
        }
        String raw = tagger.tagABNER(sentence).trim();
        return new AnnotatedSentence(raw);
    }

    @Override
    public void loadAnnotator(String modelFile) {
        tagger = new Tagger(new File(modelFile));
        tagger.setTokenization(false);
    }
}
