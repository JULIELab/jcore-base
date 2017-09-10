/**
 * BasePipe.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: tomanek
 *
 * Current version: 2.4
 * Since version:   2.2
 *
 * Creation date: Nov 1, 2006
 *
 * This is the BasePipe to be used in the SerialPipe for
 * feature extraction.
 *
 * As input, it expects the data field to be filled with a Sentence object. All the
 * other fields (source, target, name) are ignored and/or overwritten.
 **/

package de.julielab.jcore.ae.jpos.pipes;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tartarus.snowball.SnowballProgram;

import com.uea.stemmer.UEALite;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import de.julielab.jcore.ae.jpos.tagger.FeatureConfiguration;
import de.julielab.jcore.ae.jpos.tagger.Sentence;
import de.julielab.jcore.ae.jpos.tagger.Unit;

class BasePipe extends Pipe {

    private static final String UNICODE_LOWER = "\\p{Ll}";

    private static final String UNICODE_UPPER = "\\p{Lu}";

    private static final long serialVersionUID = 24;

    static Logger LOGGER = LoggerFactory.getLogger(BasePipe.class);

    Properties featureConfig;

    boolean pluralFeature = false;

    boolean lowerCaseFeature = false;

    boolean wcFeature = false;

    boolean bwcFeature = false;

    String[] customPluralSuffixes;

    Pattern UpperCaseStart;

    // Is null when no snowball should be used.
    String snowballStemmerLanguage;
    // for backward compatibility
    Object stemmer;
    // We either store the snowball language to use or the UEAStemmer object but
    // not the wrappedStemmer itself.
    transient Stemmer wrappedStemmer;

    private final String[] snowballLanguage;

    private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (stemmer instanceof UEALite) {
            wrappedStemmer = new Stemmer((UEALite) stemmer);
            LOGGER.debug("Got UEALite stemmer in loaded model: {}", wrappedStemmer);
        } else if (null != snowballLanguage) {
            wrappedStemmer = new Stemmer(snowballStemmerLanguage);
            LOGGER.debug("Got snowball stemmer for language {} in loaded model", snowballStemmerLanguage);
        } else {
            throw new IllegalStateException("No stemmer was found in the stored model, this is invalid.");
        }
    }

    public BasePipe(final Properties featureConfig) {
        super(new Alphabet(), new LabelAlphabet());
        this.featureConfig = featureConfig;
        UpperCaseStart = Pattern.compile("[" + UNICODE_UPPER + "][^" + UNICODE_UPPER + "]*");

        // ---- set the features to be used by this pipe ----

        final FeatureConfiguration fc = new FeatureConfiguration();
        // to lowercase
        if (fc.featureActive(featureConfig, "feat_lowercase_enabled")) {
            lowerCaseFeature = true;
        }

        // is plural
        if (fc.featureActive(featureConfig, "feat_plural_enabled")) {
            pluralFeature = true;
        }

        // custom plural suffixes
        if (pluralFeature) {
            customPluralSuffixes = fc.getStringArray(featureConfig, "customPluralSuffixes");
        }

        // (brief) word class
        if (fc.featureActive(featureConfig, "feat_wc_enabled")) {
            wcFeature = true;
        }

        if (fc.featureActive(featureConfig, "feat_bwc_enabled")) {
            bwcFeature = true;
        }

        // stemmer
        snowballLanguage = fc.getStringArray(featureConfig, "SnowballStemmerLanguage");
        if (snowballLanguage != null) {
            if (snowballLanguage.length == 1) {
                wrappedStemmer = new Stemmer(snowballLanguage[0]);
                snowballStemmerLanguage = snowballLanguage[0];
            } else {
                throw new IllegalArgumentException("Choose 1 language!");
            }
        } else {
            wrappedStemmer = new Stemmer(new UEALite());
            stemmer = wrappedStemmer.UEAstemmer;
        }
    }

    @Override
    public Instance pipe(final Instance carrier) {

        final Sentence sentence = (Sentence) carrier.getData();
        final ArrayList<Unit> sentenceUnits = sentence.getUnits();

        final StringBuffer source = new StringBuffer();
        final TokenSequence data = new TokenSequence(sentenceUnits.size());
        final LabelSequence target = new LabelSequence((LabelAlphabet) getTargetAlphabet(), sentenceUnits.size());

        String currWord, wc, bwc;

        final String[] stemmedWords = new String[sentenceUnits.size()];
        final String[] unstemmedWords = new String[sentenceUnits.size()];
        for (int i = 0; i < sentenceUnits.size(); i++) {
            String myWord = sentenceUnits.get(i).getRep();
            if (lowerCaseFeature) {
                final Matcher m = UpperCaseStart.matcher(myWord);
                if (m.matches()) {
                    myWord = myWord.toLowerCase();
                }
            }
            unstemmedWords[i] = myWord;
            stemmedWords[i] = wrappedStemmer.stem(unstemmedWords[i]);

        }

        try {
            // GENERATE THE FEATURES
            for (int i = 0; i < sentenceUnits.size(); i++) {

                currWord = stemmedWords[i];

                final Token token = new Token(currWord);

                // word
                token.setFeatureValue("W=" + currWord, 1);

                // (custom) plural
                if (pluralFeature) {
                    if (customPluralSuffixes == null) {
                        if (unstemmedWords[i].equals(stemmedWords[i] + "s")) {
                            token.setFeatureValue("PLURAL", 1.0);
                        }
                    } else {
                        for (final String suffix : customPluralSuffixes) {
                            if (unstemmedWords[i].equals(stemmedWords[i] + suffix)) {
                                token.setFeatureValue("PLURAL", 1.0);
                                break;
                            }
                        }
                    }
                }

                token.setText(currWord);

                // word class long
                if (wcFeature) {
                    wc = currWord;
                    wc = wc.replaceAll(UNICODE_UPPER, "A");
                    wc = wc.replaceAll(UNICODE_LOWER, "a");
                    wc = wc.replaceAll("[0-9]", "0");
                    wc = wc.replaceAll("[^" + UNICODE_UPPER + UNICODE_LOWER + "0-9]", "x");

                    token.setFeatureValue("WC=" + wc, 1);
                }

                // word class short
                if (bwcFeature) {
                    bwc = currWord;
                    bwc = bwc.replaceAll(UNICODE_UPPER + "+", "A");
                    bwc = bwc.replaceAll(UNICODE_LOWER + "+", "a");
                    bwc = bwc.replaceAll("[0-9]+", "0");
                    bwc = bwc.replaceAll("[^" + UNICODE_UPPER + UNICODE_LOWER + "0-9]+", "x");

                    token.setFeatureValue("BWC=" + bwc, 1);
                }

                source.append(token.getText());
                source.append(" ");

                data.add(token);
                try {
                    target.add(sentenceUnits.get(i).getLabel());
                } catch (final Exception e) {
                    System.err.println("Could not process: " + carrier.getData());
                    throw e;
                }
            }

            carrier.setData(data);
            carrier.setTarget(target);
            carrier.setSource(source);
            return carrier;

        } catch (final Exception e) {
            final RuntimeException e1 = new RuntimeException(e);
            e.printStackTrace();
            LOGGER.error("", e1);
            throw e1;
        }

    }

    /**
     * wrapper around stemmer implementation
     *
     * @author hellrich
     *
     */
    private class Stemmer implements Serializable {

        private static final long serialVersionUID = 666999L;
        private UEALite UEAstemmer = null;
        private SnowballProgram snowStemmer = null;

        Stemmer(final UEALite stemmer) {
            UEAstemmer = stemmer;
        }

        Stemmer(final String lang) {
            try {
                final Class<?> stemClass = Class.forName("org.tartarus.snowball.ext." + lang + "Stemmer");
                snowStemmer = (SnowballProgram) stemClass.newInstance();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        String stem(final String word) {
            if (UEAstemmer != null) {
                return UEAstemmer.stem(word).getWord();
            } else if (snowStemmer != null) {
                snowStemmer.setCurrent(word);
                snowStemmer.stem();
                return snowStemmer.getCurrent();
            } else {
                return null;
            }
        }
    }
}
