/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.tagging.pipe;

import banner.types.Sentence;
import banner.types.Token;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;
import dragon.nlp.Word;
import dragon.nlp.tool.Lemmatiser;
import dragon.nlp.tool.Tagger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used by the CRFTagger as the base for the feature set.
 *
 * @author Bob
 */
public class LemmaPOS extends Pipe {
    private static final long serialVersionUID = 1L;

    private boolean expectLemmatiser;
    private boolean expectPOSTagger;

    private transient Map<Thread, Resources> resourcesByThread;

    public LemmaPOS(Lemmatiser lemmatiser, Tagger posTagger) {
        initResourcesMap();
        getResources().lemmatiser = lemmatiser;
        expectLemmatiser = lemmatiser != null;
        getResources().posTagger = posTagger;
        expectPOSTagger = posTagger != null;
    }

    public void setLemmatiser(Lemmatiser lemmatiser) {
        initResourcesMap();
        getResources().lemmatiser = lemmatiser;
        System.out.println("Setting lemmatiser to " + Thread.currentThread());
    }

    public void setPosTagger(Tagger posTagger) {
        initResourcesMap();
        getResources().posTagger = posTagger;
        System.out.println("Setting PoS Tagger to " + Thread.currentThread());
    }

    synchronized private void initResourcesMap() {
        if (resourcesByThread == null)
            resourcesByThread = new HashMap<>();
    }

    private Resources getResources() {
        return resourcesByThread.compute(Thread.currentThread(), (t, r) -> {
            Resources ret = r;
            if (ret == null)
                ret = new Resources();
            return ret;
        });
    }

    @Override
    public Instance pipe(Instance carrier) {
        if (expectLemmatiser != (getResources().lemmatiser != null))
            throw new IllegalStateException("Model was trained with lemmatiser; not present in current config; resource map: " + resourcesByThread + ", current thread: " + Thread.currentThread());
        if (expectPOSTagger != (getResources().posTagger != null))
            throw new IllegalStateException("Model was trained with POS tagger; not present in current config");
        // TODO Add prefix ability
        Sentence sentence = (Sentence) carrier.getSource();
        List<Token> tokens = sentence.getTokens();
        dragon.nlp.Sentence posSentence = null;
        if (getResources().posTagger != null) {
            int size = tokens.size();
            posSentence = new dragon.nlp.Sentence();
            for (int i = 0; i < size; i++) {
                posSentence.addWord(new Word(tokens.get(i).getText()));
            }
            getResources().posTagger.tag(posSentence);

        }

        TokenSequence ts = (TokenSequence) carrier.getData();
        for (int i = 0; i < ts.size(); i++) {
            Token bannerToken = tokens.get(i);
            String text = bannerToken.getText();
            cc.mallet.types.Token token = ts.get(i);

            // Add features to token
            if (posSentence != null) {
                String featureName = "POS=" + posSentence.getWord(i).getPOSIndex();
                token.setFeatureValue(featureName, 1);
            }
            if (getResources().lemmatiser != null) {
                String lemma;
                if (posSentence == null)
                    lemma = getResources().lemmatiser.lemmatize(text);
                else
                    lemma = getResources().lemmatiser.lemmatize(text, posSentence.getWord(i).getPOSIndex());
                String featureName = "LEMMA=" + lemma;
                token.setFeatureValue(featureName, 1);
                token.setProperty("LEMMA", lemma);
            }
        }
        return carrier;
    }

    private class Resources {
        public Lemmatiser lemmatiser;
        public Tagger posTagger;

        @Override
        public String toString() {
            return "Resources{" +
                    "lemmatiser=" + lemmatiser +
                    ", posTagger=" + posTagger +
                    '}';
        }
    }
}
