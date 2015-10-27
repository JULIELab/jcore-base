/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Affero General Public License (LGPL) v3.0
 */

package de.julielab.jcore.ae.lingpipegazetteer.chunking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Ignore;
import org.junit.Test;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import de.julielab.jcore.ae.lingpipegazetteer.utils.StringNormalizerForChunking;
import de.julielab.jcore.ae.lingpipegazetteer.utils.StringNormalizerForChunking.NormalizedString;

public class StringNormalizerForChunkingTest {
	@Test
	public void testTextNormalization() {
		String term;
		NormalizedString ns;
		
		term = "\"Call\" - postponed";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals("Term normalization was not correct", "Call  postponed", ns.string);
		
		term = "\"Light-for-dates\" with signs of fetal malnutrition, 1,000-1,249 grams";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals("Term normalization was not correct", "Lightfordates with signs of fetal malnutrition 10001249 grams", ns.string);
		
		term = "#Tarsal &/or metatarsal bones";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals("Term normalization was not correct", "Tarsal or metatarsal bones", ns.string);
		
		term = "% <poverty line Neighborhood PhenX";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals("Term normalization was not correct", " poverty line Neighborhood PhenX", ns.string);
		
		term = "'DP-1:E2F1 complex [nucleoplasm]' positively regulates 'Transactivation of NOXA by E2F1'";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals("Term normalization was not correct", "DP1E2F1 complex nucleoplasm positively regulates Transactivation of NOXA by E2F1", ns.string);
	}
	
	@Test
	public void testNormalizedOffsets() {
		String text;
		NormalizedString ns;
		
		text = "-aa :+bb";
		// Outcome: "aabb";
		ns = StringNormalizerForChunking.normalizeString(text);
		assertEquals("The original offset is computed wrong", new Integer(0), ns.getOriginalOffset(0));
		assertEquals("The original offset is computed wrong", new Integer(2), ns.getOriginalOffset(1));
		assertEquals("The original offset is computed wrong", new Integer(3), ns.getOriginalOffset(2));
		assertEquals("The original offset is computed wrong", new Integer(6), ns.getOriginalOffset(3));
		assertEquals("The original offset is computed wrong", new Integer(7), ns.getOriginalOffset(4));
		assertNull("There are more offset mappings than should be", ns.getOriginalOffset(5));
		
		text = "((2-n-butyl-6,7-dichloro-2-cyclopentyl-2,3-dihydro-1-oxo-1H-inden-5-yl)oxy)acetic acid";
		// Outcome: "2nbutyl67dichloro2cyclopentyl23dihydro1oxo1Hinden5yloxyacetic acid";
		ns = StringNormalizerForChunking.normalizeString(text);
		assertEquals("The original offset is computed wrong", new Integer(0), ns.getOriginalOffset(0));
		assertEquals("The original offset is computed wrong", new Integer(4), ns.getOriginalOffset(1));
		assertEquals("The original offset is computed wrong", new Integer(6), ns.getOriginalOffset(2));
		assertEquals("The original offset is computed wrong", new Integer(16), ns.getOriginalOffset(9));
		assertEquals("The original offset is computed wrong", new Integer(82), ns.getOriginalOffset(62));
		assertNull("There are more offset mappings than should be", ns.getOriginalOffset(66));
	}
	
	@Test
	public void testNormalizeWithTokenizer() {
		String str;
		NormalizedString ns;
		str = "We saw Parkinson's Disease and S(H)P 1 in a sadly-formed circumvention of applicance.";
		PorterStemmerTokenizerFactory tokenizerFactory = new PorterStemmerTokenizerFactory(IndoEuropeanTokenizerFactory.INSTANCE);
		ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory);
		assertEquals("Normalization was wrong: ", "We saw Parkinson Diseas and S(H)P 1 in a sadli-form circumvent of applic.", ns.string);
		assertEquals("Offset wrong: ", new Integer(0), ns.getOriginalOffset(new Integer(0)));
		assertEquals("Offset wrong: ", new Integer(16), ns.getOriginalOffset(new Integer(16)));
		assertEquals("Offset wrong: ", new Integer(19), ns.getOriginalOffset(new Integer(17)));
		assertEquals("Offset wrong: ", new Integer(26), ns.getOriginalOffset(new Integer(23)));
		assertEquals("Offset wrong: ", new Integer(49), ns.getOriginalOffset(new Integer(46)));
		assertEquals("Offset wrong: ", new Integer(50), ns.getOriginalOffset(new Integer(47)));
		assertEquals("Offset wrong: ", new Integer(56), ns.getOriginalOffset(new Integer(51)));
		str = "We go to James' to have some coffee'ses.";
		ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory);
		assertEquals("Normalization was wrong: ", "We go to Jame' to have some coffe'se.", ns.string);
		assertEquals("Offset wrong: ", new Integer(0), ns.getOriginalOffset(new Integer(0)));
		assertEquals("Offset wrong: ", new Integer(9), ns.getOriginalOffset(new Integer(9)));
		assertEquals("Offset wrong: ", new Integer(14), ns.getOriginalOffset(new Integer(13)));
		assertEquals("Offset wrong: ", new Integer(35), ns.getOriginalOffset(new Integer(33)));
		str = "We have some 'serious things' to talk about.";
		ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory);
		assertEquals("Normalization was wrong: ", "We have some 'seriou thing' to talk about.", ns.string);
		assertEquals("Offset wrong: ", new Integer(0), ns.getOriginalOffset(new Integer(0)));
		assertEquals("Offset wrong: ", new Integer(12), ns.getOriginalOffset(new Integer(12)));
		assertEquals("Offset wrong: ", new Integer(13), ns.getOriginalOffset(new Integer(13)));
		assertEquals("Offset wrong: ", new Integer(28), ns.getOriginalOffset(new Integer(26)));
		assertEquals("Offset wrong: ", new Integer(29), ns.getOriginalOffset(new Integer(27)));
		assertEquals("Offset wrong: ", new Integer(30), ns.getOriginalOffset(new Integer(28)));
		
		str = "test dosing unit KLRg1 killer cell lectin like receptor G2 Parkinson's Disease";
		ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory);
		System.out.println(ns.string);
		
	}
	
	@Test
	@Ignore
	/**
	 * Ignored because the plural ignore introduced too much errors on test data so it was removed from the algorithm.
	 */
	public void testNormalizePlural() {
		String str;
		str = "glutathione transferases are evil";
		TokenizerFactory tokenizerFactory = new IndoEuropeanTokenizerFactory();
		NormalizedString ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory);
		assertEquals("glutathione transferase are evil", ns.string);
	}
}
