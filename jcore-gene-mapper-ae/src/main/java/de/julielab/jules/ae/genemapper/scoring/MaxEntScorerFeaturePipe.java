/** 
 * MaxEntScorerFeaturePipe.java
 * 
 * Copyright (c) 2007, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek, wermter
 * 
 * Current version: 2.2
 * Since version:   1.4.2
 *
 * Creation date: Jun 16, 2007
 * 
 * Pipe that uses similarity of Strings to learn & predict. This pipe does not use 
 * too many lexical features (i.e. word itself)
 **/
package de.julielab.jules.ae.genemapper.scoring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Token;

public class MaxEntScorerFeaturePipe extends Pipe implements Serializable {

	private boolean lexicalize = true;
	private boolean debug = false;

	// Serialization
	private static final long serialVersionUID = 1;

	private static final Logger LOGGER = LoggerFactory.getLogger(MaxEntScorerFeaturePipe.class);

	private final String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";
	
	private final String GREEK_ALPHA = "alpha";

	private final String NUMBER = "[0-9]+";
	
	private final String ONE = "1";

	private final String CHAR = "[a-z]";

	private final String ALPHA = "[a-z]+";
	
	private final String MOL_WEIGHT = "p [0-9][0-9]?"; 

	/*
	 * idea taken from SCAI paper: "playing biology's name game..."
	 */
	private String MODIFIER = "(receptor|tranporter|regulator|inhibitor|activator|suppressor|enhancer|repressor|adaptor|interactor|modulator|mediator|inducer|effector|coactivator|supressor|integrator|facilitator|binder|terminator|acceptor|proactivator|exchanger|enhancer|adapter|responder|modifier|ligand|cofactor|tranporting|regulating|inhibiting|activating|suppressing|enhancing|repressing|adapting|interacting|modulating|mediating|inducing|effecting|coactivating|supressing|integrating|facilitating|binding|terminating|accepting|responding|proactivating|exchanging|enhancing|adapting|modifying|coreceptor|cotranporter|coregulator|coinhibitor|coactivator|cosuppressor|coenhancer|corepressor|coadaptor|cointeractor|comodulator|comediator|coinducer|coeffector|coactivator|cointegrator|cofacilitator|cobinder|coterminator|coacceptor|proactivator|coexchanger|coenhancer|coadapter|coresponder|comodifier|coligand|cofactor)";
	//private String MODIFIER = "(coreceptor|cotranporter|coregulator|coinhibitor|coactivator|cosuppressor|coenhancer|corepressor|coadaptor|cointeractor|comodulator|comediator|coinducer|coeffector|coactivator|cointegrator|cofacilitator|cobinder|coterminator|coacceptor|proactivator|coexchanger|coenhancer|coadapter|coresponder|comodifier|coligand|cofactor)";
	
	private String NON_DESCRIPTIVE = "(fragment|antigen|precursor|protein|chain|domain|gene|homolog|homologue|isoform|isolog|isotype|motif|ortholog|precursor|precursors|product|sequence|subtype|subunit)";	

	
	private TokenJaroSimilarity jaroSim=null;

	
	public MaxEntScorerFeaturePipe() {
		super (new Alphabet(), new LabelAlphabet());
	}


	/**
	 * the main function of a pipe used to acutally build the features
	 */
	public Instance pipe(Instance carrier) {
		if (jaroSim==null) {
			jaroSim = new TokenJaroSimilarity();
		}

		//Example example = (Example)carrier.getData();
		//String term1 = example.term1;
		//String term2 = example.term2;
		//String label = example.label;

		String[] pair = (String[]) carrier.getData();
		String term1 = pair[0];
		String term2 = pair[1];
		String label = pair[2];
		
		//if(debug) {
			//System.out.println(term1 + "\t" + term2 +"\t" + label);
		//}
		
		MaxEntScorerPairExtractor ext = new MaxEntScorerPairExtractor();

		Label target = ((LabelAlphabet) this.getTargetAlphabet())
				.lookupLabel(label);

		String[][] results = ext.compareStrings(term1, term2);
		
		String[] allBigramsTerm1 = allBigrams(term1);
		String[] allBigramsTerm2 = allBigrams(term2);
		String[] diffBigrams = differentBigrams(term1, term2);
		String[] commonBigrams = commonBigrams(term1, term2);
		String[] diffTrigrams = differentTrigrams(term1, term2);
		String[] commonTrigrams = commonTrigrams(term1, term2);
		//String[] commonCharTrigrams = commonCharTrigrams(term1, term2);
		
		
		
		Token token = new Token(term1);
		token.setText(term1);

		/*
		 * 
		 * the features:
		 */
		
		/*
		 * Common and different bigrams and trigrams,
		 * including molecular weight features (e.g. p32)
		 */
		
		
		boolean term1HasMolWeight = false;
		boolean term2HasMolWeight = false;
		for(String bigram1: allBigramsTerm1) {
			if(bigram1.matches(MOL_WEIGHT)) {
				term1HasMolWeight = true;
			}
		}
		for(String bigram2: allBigramsTerm2) {
			if(bigram2.matches(MOL_WEIGHT)) {
				term2HasMolWeight = true;
			}
		}
		
		for(String bigram: diffBigrams) {
			//token.setFeatureValue("DIFF_BIGRAM=" + bigram, 1.0);
			if(bigram.matches(MOL_WEIGHT) && term1HasMolWeight && term2HasMolWeight) {
				token.setFeatureValue("DIFF_MOL_WEIGHT", 1.0);
			}
			//System.out.println("DIFF_BIGRAM="+bigram);
		}
		
		/*
		// hurts sligthly
		for(String trigram: diffTrigrams) {
			//token.setFeatureValue("DIFF_TRIGRAM=" + trigram, 1.0);
			//System.out.println("DIFF_TRIGRAM="+trigram);
		}
		*/
		
		for(String bigram: commonBigrams) {
			token.setFeatureValue("COMMON_BIGRAM=" + bigram, 1.0);
			if(bigram.matches(MOL_WEIGHT)) {
				token.setFeatureValue("SAME_MOL_WEIGHT", 1.0);
			}
			//System.out.println("COMMON_BIGRAM="+bigram);
		}
		for(String trigram: commonTrigrams) {
			token.setFeatureValue("COMMON_TRIGRAM=" + trigram, 1.0);
			//System.out.println("COMMON_TRIGRAM="+trigram);
		}
		
		// char ngrams hurt performance
		//for(String trigram: commonCharTrigrams) {
			//token.setFeatureValue("COMMON_CHAR_TRIGRAM=" + trigram, 1.0);
			//System.out.println("COMMON_TRIGRAM="+trigram);
		//}
		
		
		/*
		 * overlap feature equals the simple score
		 */
		double simpleScore = (new SimpleScorer()).getScore(term1, term2);
		
		if (simpleScore == 1) {
			token.setFeatureValue("SIMPLESCORE=1", 1);
		} else if (simpleScore >= 0.9) {
			token.setFeatureValue("SIMPLESCORE>=0.9", 1.0);
		} else if (simpleScore >= 0.8) {
			token.setFeatureValue("SIMPLESCORE>=0.8", 1.0);
		} else if (simpleScore >= 0.7) {
			token.setFeatureValue("SIMPLESCORE>=0.7", 1.0);
		} else if (simpleScore >= 0.6) {
			token.setFeatureValue("SIMPLESCORE>=0.6", 1.0);
		} else if (simpleScore >= 0.5) {
			token.setFeatureValue("SIMPLESCORE>=0.5", 1.0);
		} else if (simpleScore >= 0.3) {
			token.setFeatureValue("SIMPLESCORE>=0.3", 1.0);
		} else {
			//token.setFeatureValue("SIMPLESCORE<0.3", 1);
		}
			
		
		/*
		 * common string
		 * whether one is substring of the other
		 */
		if (term1.indexOf(term2)>-1 || term2.indexOf(term1)>-1) {
			token.setFeatureValue("SUBSTRING", 1.0);
		}
		
		/*
		 * number of transpositions (for word ordering needed)
		 */
		int transpositions = jaroSim.getTokenTranspositions(term1,term2);
		token.setFeatureValue("TRANSPOSITIONS=" + transpositions, 1.0);
		
		/*
		 * features on same tokens
		 */

		HashMap<String, Integer> sames = new HashMap<String, Integer>();
		for (int j = 0; j < results[0].length; ++j) {

			String sameToken = results[0][j];
			if (sameToken.matches("[0-9]+")) { // count only 
				// number
				add2HashMap(sames, "SAME_NUM");
			} else { // count and add string
				if (sameToken.matches(GREEK)) { 
					// greek letter
					add2HashMap(sames, "SAME_GREEK");
				} else if (sameToken.matches(CHAR)) {
					// single character (count and add string)
					//add2HashMap(sames, "SAME_CHAR");
				} else if (sameToken.matches(ALPHA)) {
					// single character (count and add string)
					add2HashMap(sames, "SAME_ALPHA");
				} else if (sameToken.matches(MODIFIER)) {
					// single character (count and add string)
					add2HashMap(sames, "SAME_MODIFIER");
				} else if (sameToken.matches(NON_DESCRIPTIVE)) {
					// single character (count and add string)
					//add2HashMap(sames, "SAME_NON_DESCRIPTIVE="+sameToken);
				} else {
					if (lexicalize) {
						sames.put("SAME_STRING=" + sameToken, 1);
					}
				}
			}
		}

		for (Iterator iter = sames.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			int count = sames.get(key).intValue();
			token.setFeatureValue(key + "=" + count, 1.0);
		}

		int numOfSames = results[0].length;
		token.setFeatureValue("NUM_OF_SAMES=" + numOfSames, 1.0);
		
		// one term is contained in the other (doesn't seem to help
		/*
		if(numOfSames == lengthTerm1 || numOfSames == lengthTerm2) {
			token.setFeatureValue("IS_CONTAINED", 1.0);	
		}
		*/
		
		if (results[0].length == 1) {
			String onlySame = results[0][0];
			if (onlySame.matches(NUMBER)) {
				// syns have only a number in common
				token.setFeatureValue("ONLY_SAME_NUMBER", 1.0);
			} else if (onlySame.matches(GREEK)) {
				// syns have only a greek letter in common
				token.setFeatureValue("ONLY_SAME_GREEK", 1.0);
			} else if (onlySame.matches(CHAR)) {
				// syns have only a character in common
				token.setFeatureValue("ONLY_SAME_CHAR", 1.0);
			} else if (onlySame.matches(ALPHA)) {
				// syns have only a alpha token in common
				token.setFeatureValue("ONLY_SAME_ALPHA", 1.0);
			} else if (onlySame.matches(MODIFIER)) {
				// syns have only a modifier in common
				//token.setFeatureValue("ONLY_SAME_MODIFIER", 1.0);
			} else if (onlySame.matches(NON_DESCRIPTIVE)) {
				// syns have only a non-descriptive in common
				//token.setFeatureValue("ONLY_SAME_NON_DESCRIPTIVE="+onlySame, 1.0);
			} else {
				if (lexicalize) {
					sames.put("ONLY_SAME_STRING=" + onlySame, 1);
				}
			}
		}

		/*
		 * features on different tokens
		 */
		HashMap<String, Integer> diffs = new HashMap<String, Integer>();

		for (int j = 0; j < results[1].length; ++j) {
			String diffToken = results[1][j];
			if (diffToken.matches("[0-9]+")) { // count only 
				// number
				add2HashMap(diffs, "DIFF_NUM");
			} else { // count and add string
				if (diffToken.matches(GREEK)) { 
					// greek letter
					add2HashMap(diffs, "DIFF_GREEK");
					
				} else if (diffToken.matches(CHAR)) {
					// single character (count and add string)
					add2HashMap(diffs, "DIFF_CHAR");
				} else if (diffToken.matches(ALPHA)) {
					// single character (count and add string)
					add2HashMap(diffs, "DIFF_ALPHA");
				} else if (diffToken.matches(MODIFIER)) {
					// single character (count and add string)
					add2HashMap(diffs, "DIFF_MODIFIER");
					
				} else if (diffToken.matches(NON_DESCRIPTIVE)) {
					// single character (count and add string)
					//add2HashMap(diffs, "DIFF_NON_DESCRIPTIVE="+diffToken);
				} else {
					if (lexicalize) {
						diffs.put("DIFF_STRING=" + diffToken, 1);
					}
				}
			}
		}

		for (Iterator iter = diffs.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			int count = diffs.get(key).intValue();
			token.setFeatureValue(key + "=" + count, 1.0);
			
		}

		token.setFeatureValue("NUM_OF_DIFFS=" + results[1].length, 1.0);


		if (results[1].length == 1) {
			String onlyDiff = results[1][0];
			if (onlyDiff.matches(ONE)) {
				// syns have only a number not in common
				token.setFeatureValue("ONLY_DIFF_ONE", 1.0);
				
			} else if (onlyDiff.matches(NUMBER)) {
				// syns have only a number not in common
				token.setFeatureValue("ONLY_DIFF_NUMBER", 1.0);
			
			} else if (onlyDiff.matches(GREEK_ALPHA)) {
				// syns have only a greek letter not in common
				token.setFeatureValue("ONLY_DIFF_GREEK_ALPHA", 1.0);
				
			} else if (onlyDiff.matches(GREEK)) {
				// syns have only a greek letter not in common
				token.setFeatureValue("ONLY_DIFF_GREEK", 1.0);
				
			} else if (onlyDiff.matches(ALPHA)) {
				// syns have only a alpha token not in common
				token.setFeatureValue("ONLY_DIFF_ALPHA", 1.0);
				
			} else if (onlyDiff.matches(MODIFIER)) {
				// syns have only a modifier not in common
				token.setFeatureValue("ONLY_DIFF_MODIFIER", 1.0);
			} else if (onlyDiff.matches(NON_DESCRIPTIVE)) {
				// syns have only a modifier not in common
				token.setFeatureValue("ONLY_DIFF_NON_DESCRIPTIVE", 1.0);
			} else {
				if (lexicalize) {
					sames.put("ONLY_DIFF_STRING=" + onlyDiff, 1);
				}
			}
		}
		
		
		/*
		 * length of both terms
		 */
		int lenDiff = Math.abs(term1.split(" ").length
				- term2.split(" ").length);
		token.setFeatureValue("LENGTHDIFF=" + lenDiff, 1);
		// relative lendiff
		int maxLen = Math.max(term1.split(" ").length, term2.split(" ").length);
		double relLenDiff = 1-lenDiff/(double) maxLen;
		
		if (relLenDiff>=0.9) {
			token.setFeatureValue("RELLENGTHDIFF>=0.9", 1);
		} else if (relLenDiff>=0.7) {
			token.setFeatureValue("RELLENGTHDIFF>=0.7", 1);
		} else if (relLenDiff>=0.5) {
			token.setFeatureValue("RELLENGTHDIFF>=0.5", 1);
		} else {
			token.setFeatureValue("RELLENGTHDIFF<0.5", 1);
		}
		
		

		if (debug) {
			System.out
					.println("\n--------------------------------------------\n"
							+ "Features for: " + term1 + "\t" + term2 + "\t" + label
							+ "\n" + token.toString());
		}

		// add data to instance
		carrier.setData(token);
		carrier.setTarget(target);
		carrier.setSource(term1 + " <-> " + term2);
		carrier.setName(target.toString());

		return carrier;
	}

	
	/**
	 * Helper class for ngrams
	 * 
	 */
	
	private ArrayList<String> makeBigrams(String term) {
		
		String[] split = term.split(" ");
		ArrayList<String> bigrams = new ArrayList<String>();
		
		for (int i=1; i < split.length; i++) {
			String bigram = split[i-1] + " " + split[i];
			//bigram = bigram.replaceAll("[0-9]+", "NUM");
			bigram = bigram.trim();
			bigrams.add(bigram);
		}
		
		
		return bigrams;
	}
	
	/**
	 * Helper class for ngrams
	 * 
	 */
	
	private String[] allBigrams(String term) {
		ArrayList<String> bigrams = makeBigrams(term);
		String[] bigramArray = bigrams.toArray(new String[]{});
		return bigramArray;
	}
	
	/**
	 * Helper method for ngrams
	 * 
	 */
	
	private String[] commonBigrams(String term1, String term2) {
		
		ArrayList<String> commons = new ArrayList<String>();
		
		ArrayList<String> bigrams1 = makeBigrams(term1);
		//System.out.println(term1 + ": " + bigrams1);
		String[] bigramList1 = bigrams1.toArray(new String[] {});
		
		ArrayList<String> bigrams2 = makeBigrams(term2);
		//System.out.println(term2 + ": " + bigrams2);
		String[] bigramList2 = bigrams2.toArray(new String[] {});
		
		for(String bigram1: bigramList1) {
			if(bigrams2.contains(bigram1)) {
				commons.add(bigram1);
			}
		}
		for(String bigram2: bigramList2) {
			if(bigrams1.contains(bigram2) && !commons.contains(bigram2)) {
				commons.add(bigram2);
			}
		}
		
		return commons.toArray(new String[] {}); 
	}
	
	/**
	 * Helper method for ngrams
	 * 
	 */
	
	private ArrayList<String> makeCharTrigrams(String term) {
		
	    StringBuilder sb = new StringBuilder(term);
		ArrayList<String> trigrams = new ArrayList<String>();
		
		for (int i=2; i < sb.length(); i++) {
			String trigram = sb.charAt(i-2) + "" + sb.charAt(i-1) + "" + sb.charAt(i);
			//bigram = bigram.replaceAll("[0-9]+", "NUM");
			//trigram = trigram.trim();
			trigrams.add(trigram);
		}
		
		
		return trigrams;
	}
	
	/**
	 * Helper method for ngrams
	 * 
	 */
	
	private String[] commonCharTrigrams(String term1, String term2) {
		
		ArrayList<String> commons = new ArrayList<String>();
		
		ArrayList<String> trigrams1 = makeCharTrigrams(term1);
		//System.out.println(term1 + ": " + bigrams1);
		String[] trigramList1 = trigrams1.toArray(new String[] {});
		
		ArrayList<String> trigrams2 = makeCharTrigrams(term2);
		//System.out.println(term2 + ": " + bigrams2);
		String[] trigramList2 = trigrams2.toArray(new String[] {});
		
		for(String trigram1: trigramList1) {
			if(trigrams2.contains(trigram1)) {
				commons.add(trigram1);
			}
		}
		for(String trigram2: trigramList2) {
			if(trigrams1.contains(trigram2) && !commons.contains(trigram2)) {
				commons.add(trigram2);
			}
		}
		
		return commons.toArray(new String[] {}); 
	}
	
	/**
	 * Helper method for ngrams
	 * 
	 */
	
	private ArrayList<String> makeTrigrams(String term) {
		
		String[] split = term.split(" ");
		ArrayList<String> trigrams = new ArrayList<String>();
		
		for (int i=2; i < split.length; i++) {
			String trigram = split[i-2] + " " + split[i-1] + " " + split[i];
			//bigram = bigram.replaceAll("[0-9]+", "NUM");
			trigram = trigram.trim();
			trigrams.add(trigram);
		}
		
		
		return trigrams;
	}
	
	/**
	 * Helper method for ngrams
	 * 
	 */
	
   private String[] commonTrigrams(String term1, String term2) {
		
		ArrayList<String> commons = new ArrayList<String>();
		
		ArrayList<String> trigrams1 = makeTrigrams(term1);
		//System.out.println(term1 + ": " + bigrams1);
		String[] trigramList1 = trigrams1.toArray(new String[] {});
		
		ArrayList<String> trigrams2 = makeTrigrams(term2);
		//System.out.println(term2 + ": " + bigrams2);
		String[] trigramList2 = trigrams2.toArray(new String[] {});
		
		for(String trigram1: trigramList1) {
			if(trigrams2.contains(trigram1)) {
				commons.add(trigram1);
			}
		}
		for(String trigram2: trigramList2) {
			if(trigrams1.contains(trigram2) && !commons.contains(trigram2)) {
				commons.add(trigram2);
			}
		}
		
		return commons.toArray(new String[] {}); 
	}
	
	
   /**
	 * Helper method for ngrams
	 * 
	 */
	
	private String[] differentBigrams(String term1, String term2) {
		
		ArrayList<String> differents = new ArrayList<String>();
		ArrayList<String> bigrams1 = makeBigrams(term1);
		String[] bigramList1 = bigrams1.toArray(new String[] {});
		
		ArrayList<String> bigrams2 = makeBigrams(term2);
		String[] bigramList2 = bigrams2.toArray(new String[] {});
		
		for(String bigram1: bigramList1) {
			if(!bigrams2.contains(bigram1)) {
				differents.add(bigram1);
			}
		}
		for(String bigram2: bigramList2) {
			if(!bigrams1.contains(bigram2) && !differents.contains(bigram2)) {
				differents.add(bigram2);
			}
		}
		
		return differents.toArray(new String[] {}); 
	}
	
	/**
	 * Helper method for ngrams
	 * 
	 */
	
	private String[] differentTrigrams(String term1, String term2) {
		
		ArrayList<String> differents = new ArrayList<String>();
		ArrayList<String> trigrams1 = makeTrigrams(term1);
		String[] trigramList1 = trigrams1.toArray(new String[] {});
		
		ArrayList<String> trigrams2 = makeTrigrams(term2);
		String[] trigramList2 = trigrams2.toArray(new String[] {});
		
		for(String trigram1: trigramList1) {
			if(!trigrams2.contains(trigram1)) {
				differents.add(trigram1);
			}
		}
		for(String trigram2: trigramList2) {
			if(!trigrams1.contains(trigram2) && !differents.contains(trigram2)) {
				differents.add(trigram2);
			}
		}
		
		return differents.toArray(new String[] {}); 
	}

	

	/**
	 * adds a key to a hasmap and counts how often key was inserted
	 * 
	 * @param map
	 * @param key
	 */
	private void add2HashMap(HashMap<String, Integer> map, String key) {
		int count = 0;
		if (map.containsKey(key)) {
			count = map.get(key).intValue();
		}
		count++;
		map.put(key, count);
	}

	
}