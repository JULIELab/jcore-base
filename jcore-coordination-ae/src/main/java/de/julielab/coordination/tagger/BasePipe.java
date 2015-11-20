/** 
 * BasePipe.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: buyko
 * 
 * Current version: 1.4	
 * Since version:   0.1
 *
 * Creation date: July 8, 2008 
 * 
 * This is the BasePipe to be used in the SerialPipe for
 * feature extraction.
 * 
 * As input, it expects the data field to be filled with a Sentence object. All the 
 * other fields (source, target, name) are ignored and/or overwritten.
 **/
package de.julielab.coordination.tagger;

import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.uea.stemmer.UEALite;
import com.uea.stemmer.Word;

import de.julielab.jcore.ae.coordbaseline.main.Baseline;
import de.julielab.jcore.ae.coordbaseline.types.CoordinationToken;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.LabelAlphabet;
import edu.umass.cs.mallet.base.types.LabelSequence;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.util.PropertyList;

class BasePipe extends Pipe {

	private static final long serialVersionUID = 2L;
	UEALite stemmer;
	Properties featureConfig;
	boolean pluralFeature = false;
	boolean lowerCaseFeature = false;
	boolean wcFeature = false;
	boolean bwcFeature = false;
	Pattern UpperCaseStart;

	public BasePipe(Properties featureConfig) {
		super(null, LabelAlphabet.class);
		this.featureConfig = featureConfig;
		stemmer = new UEALite();
		UpperCaseStart = Pattern.compile("[" + FeatureGenerator.CAPS + "][^" + FeatureGenerator.CAPS + "]*");
		// ---- set the features to be used by this pipe ----
		// to lowercase
		if (FeatureConfiguration.featureActive(featureConfig, "feat_lowercase_enabled")) {
			lowerCaseFeature = true;
		}
		// is plural
		if (FeatureConfiguration.featureActive(featureConfig, "feat_plural_enabled")) {
			pluralFeature = true;
		}
		// (brief) word class
		if (FeatureConfiguration.featureActive(featureConfig, "feat_wc_enabled")) {
			wcFeature = true;
		}
		if (FeatureConfiguration.featureActive(featureConfig, "feat_bwc_enabled")) {
			bwcFeature = true;
		}
	}

	public Instance pipe(Instance carrier) {
		ArrayList<CoordinationToken> symmetryBaselineTokens = new ArrayList<CoordinationToken>();
		ArrayList<CoordinationToken> nATokens = new ArrayList<CoordinationToken>();
		Sentence sentence = (Sentence) carrier.getData();
		ArrayList<Unit> sentenceUnits = sentence.getUnits();
		StringBuffer source = new StringBuffer();
		TokenSequence data = new TokenSequence(sentenceUnits.size());
		LabelSequence target = new LabelSequence((LabelAlphabet) getTargetAlphabet(), sentenceUnits.size());
		String currWord, meta, wc, bwc;
		String[] trueMetas = FeatureConfiguration.getTrueMetas(featureConfig);
		String[] stemmedWords = new String[sentenceUnits.size()];
		String[] unstemmedWords = new String[sentenceUnits.size()];
		for (int i = 0; i < sentenceUnits.size(); i++) {
			String myWord = sentenceUnits.get(i).getRep();
			if (lowerCaseFeature) {
				Matcher m = UpperCaseStart.matcher(myWord);
				if (m.matches()) {
					myWord = myWord.toLowerCase();
				}
			}
			unstemmedWords[i] = myWord;
			stemmedWords[i] = ((Word) stemmer.stem(unstemmedWords[i])).getWord();
		}
		try {
			// GENERATE THE FEATURES
			for (int i = 0; i < sentenceUnits.size(); i++) {
				currWord = stemmedWords[i];
				Token token = new Token(currWord);
				// word
				token.setFeatureValue("W=" + currWord, 1);
				// was plural
				if (pluralFeature) {
					if (unstemmedWords[i].equals(stemmedWords[i] + "s")) {
						token.setFeatureValue("PLURAL", 1.0);
					}
				}
				// meta tags
				for (String key : trueMetas) {
					String metaName = featureConfig.getProperty(key + "_feat_unit");
					if ((meta = sentenceUnits.get(i).getMetaInfo(metaName)) != null) {
						token.setFeatureValue(metaName + "=" + meta, 1);
					}
				}
				token.setText(currWord);
				// word class long
				if (wcFeature) {
					wc = currWord;
					wc = wc.replaceAll("[A-Z]", "A");
					wc = wc.replaceAll("[a-z]", "a");
					wc = wc.replaceAll("[0-9]", "0");
					wc = wc.replaceAll("[^A-Za-z0-9]", "x");
					token.setFeatureValue("WC=" + wc, 1);
				}
				// word class short
				if (bwcFeature) {
					bwc = currWord;
					bwc = bwc.replaceAll("[A-Z]+", "A");
					bwc = bwc.replaceAll("[a-z]+", "a");
					bwc = bwc.replaceAll("[0-9]+", "0");
					bwc = bwc.replaceAll("[^A-Za-z0-9]+", "x");
					token.setFeatureValue("BWC=" + bwc, 1);
				}
				source.append(token.getText());
				source.append(" ");
				data.add(token);
				target.add(sentenceUnits.get(i).getLabel());
				CoordinationToken coordToken = new CoordinationToken();
				coordToken.setWord(currWord);
				coordToken.setPosTag(sentenceUnits.get(i).getMetaInfo("pos"));
				System.out.println(sentenceUnits.get(i).getMetaInfo("pos"));
				symmetryBaselineTokens.add(coordToken);
				nATokens.add(coordToken);
			}
			/*
			 * Number Agreement Similarity Baseline
			 */
			if (featureConfig.getProperty("na_baseline_enabled").equals("true")) {
				try {
					NABaseline sim = new NABaseline();
					sim.predictConjuncts(nATokens);
					for (int i = 0; i < data.size(); i++) {
						Token token = data.getToken(i);
						CoordinationToken current = nATokens.get(i);
						String label = current.getCoordinationLabel();
						if (label.equals(NABaseline.CONJ)) {
							token.setFeatureValue("na_baseline=" + label, 1);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
			/**
			 * symmetry baseline 2 features (maximal symmetry for pos tags)
			 */
			try {
				if (featureConfig.getProperty("symmetry_baseline_enabled").equals("true")) {
					Baseline baseline2 = new Baseline();
					baseline2.predictConjuncts(symmetryBaselineTokens);
					for (int i = 0; i < data.size(); i++) {
						Token token = data.getToken(i);
						CoordinationToken current = symmetryBaselineTokens.get(i);
						token.setFeatureValue("symmetry_baseline=" + current.getCoordinationLabel(), 1);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// produce an error, if a label was not found
			// this has to be done due to an insuffency in
			// the mallet code
			if (target.size() != data.size()) {
				throw new CoordinationException("Label not found... check your label definition file.");
			}
			carrier.setData(data);
			carrier.setTarget(target);
			carrier.setSource(source);
			return carrier;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
}
