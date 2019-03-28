package lingscope.algorithms;

import abner.Tagger;
import abner.Trainer;
import lingscope.structures.AnnotatedSentence;

import java.io.InputStream;

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
    public void loadAnnotator(InputStream is) {
        tagger = new Tagger(is);
        tagger.setTokenization(false);

    }
}
