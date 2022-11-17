
package de.julielab.jcore.ae.lingpipegazetteer.chunking;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.ibm.icu.text.Transliterator;
import de.julielab.java.utilities.spanutils.OffsetSet;
import de.julielab.jcore.ae.lingpipegazetteer.utils.StringNormalizerForChunking;
import de.julielab.jcore.ae.lingpipegazetteer.utils.StringNormalizerForChunking.NormalizedString;
import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StringNormalizerForChunkingTest {

	private Transliterator transliterator = Transliterator.getInstance("NFD; [:Nonspacing Mark:] Remove; NFC");
	@Test
	public void testTextNormalization() {
		String term;
		NormalizedString ns;

		term = "\"Call\" - postponed";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals( "Call  postponed",  ns.string, "Term normalization was not correct");

		term = "\"Light-for-dates\" with signs of fetal malnutrition, 1,000-1,249 grams";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals("Lightfordates with signs of fetal malnutrition 10001249 grams",
				ns.string, "Term normalization was not correct");

		term = "#Tarsal &/or metatarsal bones";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals( "Tarsal or metatarsal bones",  ns.string, "Term normalization was not correct");

		term = "% <poverty line Neighborhood PhenX";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals( " poverty line Neighborhood PhenX",  ns.string, "Term normalization was not correct");

		term = "'DP-1:E2F1 complex [nucleoplasm]' positively regulates 'Transactivation of NOXA by E2F1'";
		ns = StringNormalizerForChunking.normalizeString(term);
		assertEquals("DP1E2F1 complex nucleoplasm positively regulates Transactivation of NOXA by E2F1",
				ns.string, "Term normalization was not correct");
	}

	@Test
	public void testNormalizedOffsets() {
		String text;
		NormalizedString ns;

		text = "-aa :+bb";
		// Outcome: "aabb";
		ns = StringNormalizerForChunking.normalizeString(text);
		assertEquals( Integer.valueOf(0),  ns.getOriginalOffset(0), "The original offset is computed wrong");
		assertEquals( Integer.valueOf(2),  ns.getOriginalOffset(1), "The original offset is computed wrong");
		assertEquals( Integer.valueOf(3),  ns.getOriginalOffset(2), "The original offset is computed wrong");
		assertEquals( Integer.valueOf(6),  ns.getOriginalOffset(3), "The original offset is computed wrong");
		assertEquals( Integer.valueOf(7),  ns.getOriginalOffset(4), "The original offset is computed wrong");
		assertNull(ns.getOffsetMap().get(5), "There are more offset mappings than should be");

		text = "((2-n-butyl-6,7-dichloro-2-cyclopentyl-2,3-dihydro-1-oxo-1H-inden-5-yl)oxy)acetic acid";
		// Outcome:
		// "2nbutyl67dichloro2cyclopentyl23dihydro1oxo1Hinden5yloxyacetic acid";
		ns = StringNormalizerForChunking.normalizeString(text);
		assertEquals( Integer.valueOf(0),  ns.getOriginalOffset(0), "The original offset is computed wrong");
		assertEquals( Integer.valueOf(4),  ns.getOriginalOffset(1), "The original offset is computed wrong");
		assertEquals( Integer.valueOf(6),  ns.getOriginalOffset(2), "The original offset is computed wrong");
		assertEquals( Integer.valueOf(16),  ns.getOriginalOffset(9), "The original offset is computed wrong");
		assertEquals( Integer.valueOf(82),  ns.getOriginalOffset(62), "The original offset is computed wrong");
		assertNull(ns.getOffsetMap().get(66), "There are more offset mappings than should be");
	}

	@Test
	public void testNormalizedOffsetsTransliterate() {
		// Some characters, for example "V⃗" which appears twice in this test's
		// text, are actually composed of two character. With transliteration,
		// we remove the additional character. We have to count that in with the
		// normalization.
		String text;
		NormalizedString ns;

		text = "Each node either a sensor or a beacon is noted as Nodep, p ∈ 𝕊 ∪ 𝔹, and vector V⃗p is used to represent the coordinate of Nodep. Beacons are placed onto the map with fixed coordinates V⃗j, where j ∈ 𝔹. We assume that each beacon is aware of its own absolute location.";
		Transliterator t = Transliterator.getInstance("NFD; [:Nonspacing Mark:] Remove; NFC; Lower");
		assertTrue(t.transform(text).length() == text.length() - 2);
		ns = StringNormalizerForChunking.normalizeString(text, new IndoEuropeanTokenizerFactory(), t);
		assertEquals("each node either a sensor or a beacon is noted as nodep, p ∈ 𝕊 ∪ 𝔹, and vector vp is used to represent the coordinate of nodep. beacons are placed onto the map with fixed coordinates vj, where j ∈ 𝔹. we assume that each beacon is aware of its own absolute location.",
				ns.string,
				"Transliteration wasn't done correctly");
		assertEquals(Integer.valueOf(83), ns.getOriginalOffset(82));
		assertEquals(Integer.valueOf(188), ns.getOriginalOffset(186));
	}

	@Test
	public void testNormalizedOffetsTransliterator2() {
		// Here we test for "derived offsets". The issue is, the offsetMap in
		// the NormalizedString class only has entries for each token, by
		// default. However, it might happen that an offset is asked for that
		// lies between tokens. In this case, the original offset must be
		// "derived" be computing the offset shift. This is tested here.
		String text = "b⃗1,2, respectively.";
		Transliterator t = Transliterator.getInstance("NFD; [:Nonspacing Mark:] Remove; NFC; Lower");
		NormalizedString ns = StringNormalizerForChunking.normalizeString(text, new IndoEuropeanTokenizerFactory(), t);
		// in the original string, the substring from 0 to 10 would be "b⃗1,2,
		// res", since the first character is a two-byte character. In the
		// (transliterated) normalized string, it is only one byte. This is why we expect to get one character more.
		assertEquals( text.substring(0,  ns.getOriginalOffset(10)), "b⃗1,2, resp");
	}

	@Test
	public void testNormalizeWithTokenizer() {
		String str;
		NormalizedString ns;
		str = "We saw Parkinson's Disease and S(H)P 1 in a sadly-formed circumvention of applicance.";
		PorterStemmerTokenizerFactory tokenizerFactory = new PorterStemmerTokenizerFactory(
				IndoEuropeanTokenizerFactory.INSTANCE);
		ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory, transliterator);
		assertEquals("We saw Parkinson Diseas and S(H)P 1 in a sadli-form circumvent of applic.",
				ns.string, "Normalization was wrong: ");
		assertEquals( Integer.valueOf(0),  ns.getOriginalOffset(Integer.valueOf(0)), "Offset wrong: ");
		assertEquals( Integer.valueOf(16),  ns.getOriginalOffset(Integer.valueOf(16)), "Offset wrong: ");
		assertEquals( Integer.valueOf(19),  ns.getOriginalOffset(Integer.valueOf(17)), "Offset wrong: ");
		assertEquals( Integer.valueOf(26),  ns.getOriginalOffset(Integer.valueOf(23)), "Offset wrong: ");
		assertEquals( Integer.valueOf(49),  ns.getOriginalOffset(Integer.valueOf(46)), "Offset wrong: ");
		assertEquals( Integer.valueOf(50),  ns.getOriginalOffset(Integer.valueOf(47)), "Offset wrong: ");
		assertEquals( Integer.valueOf(56),  ns.getOriginalOffset(Integer.valueOf(51)), "Offset wrong: ");
		str = "We go to James' to have some coffee'ses.";
		ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory, transliterator);
		assertEquals( "We go to Jame' to have some coffe'se.",  ns.string, "Normalization was wrong: ");
		assertEquals( Integer.valueOf(0),  ns.getOriginalOffset(Integer.valueOf(0)), "Offset wrong: ");
		assertEquals( Integer.valueOf(9),  ns.getOriginalOffset(Integer.valueOf(9)), "Offset wrong: ");
		assertEquals( Integer.valueOf(14),  ns.getOriginalOffset(Integer.valueOf(13)), "Offset wrong: ");
		assertEquals( Integer.valueOf(35),  ns.getOriginalOffset(Integer.valueOf(33)), "Offset wrong: ");
		str = "We have some 'serious things' to talk about.";
		ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory, transliterator);
		assertEquals( "We have some 'seriou thing' to talk about.",  ns.string, "Normalization was wrong: ");
		assertEquals( Integer.valueOf(0),  ns.getOriginalOffset(Integer.valueOf(0)), "Offset wrong: ");
		assertEquals( Integer.valueOf(12),  ns.getOriginalOffset(Integer.valueOf(12)), "Offset wrong: ");
		assertEquals( Integer.valueOf(13),  ns.getOriginalOffset(Integer.valueOf(13)), "Offset wrong: ");
		assertEquals( Integer.valueOf(28),  ns.getOriginalOffset(Integer.valueOf(26)), "Offset wrong: ");
		assertEquals( Integer.valueOf(29),  ns.getOriginalOffset(Integer.valueOf(27)), "Offset wrong: ");
		assertEquals( Integer.valueOf(30),  ns.getOriginalOffset(Integer.valueOf(28)), "Offset wrong: ");

		str = "test dosing unit KLRg1 killer cell lectin like receptor G2 Parkinson's Disease";
		ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory, transliterator);
		System.out.println(ns.string);

	}

	@Test
	public void testNewlines() {
		String str = "Clinical Features and Course of Patients with Peripheral Exudative Hemorrhagic Chorioretinopathy.\n" +
				"To evaluate the clinical characteristics of patients who were followed in our clinic with the diagnosis of peripheral exudative hemorrhagic chorioretinopathy (PEHC).\n" +
				"Medical records of 12 patients who were diagnosed with PEHC in İstanbul University İstanbul Faculty of Medicine, Department of Ophthalmology between July 2006 and June 2014 were reviewed retrospectively.";
		NormalizedString normalizedString = StringNormalizerForChunking.normalizeString(str, new IndoEuropeanTokenizerFactory(), Transliterator.getInstance("NFD; [:Nonspacing Mark:] Remove; NFC; Lower"));
		System.out.println(normalizedString.getOffsetMap());
	}

	@Test
	public void testNormalizePlural() {
		String str;
		str = "glutathione transferases are evil";
		TokenizerFactory tokenizerFactory = new IndoEuropeanTokenizerFactory();
		NormalizedString ns = StringNormalizerForChunking.normalizeString(str, tokenizerFactory, true, new OffsetSet(List.of(Range.between(12, 24))), transliterator);
		assertEquals("glutathione transferase are evil", ns.string);
	}
}
