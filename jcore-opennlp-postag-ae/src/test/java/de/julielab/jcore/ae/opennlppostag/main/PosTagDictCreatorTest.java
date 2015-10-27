package de.julielab.jcore.ae.opennlppostag.main;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSSample;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.julielab.jcore.ae.opennlppostag.main.POSTagDictCreator;

public class PosTagDictCreatorTest {
	@Test
	public void testReadPOSSamples() {
		List<POSSample> posSamples =
				POSTagDictCreator.readPOSSamples(new File("src/test/resources/posSamples.opennlp"));
		assertEquals(4, posSamples.size());
		POSSample posSample = posSamples.get(0);
		assertEquals(20, posSample.getSentence().length);
		assertEquals(20, posSample.getTags().length);
		assertEquals("Small", posSample.getSentence()[0]);
		assertEquals("cell", posSample.getSentence()[1]);
		assertEquals("carcinoma", posSample.getSentence()[2]);

		assertEquals("JJ", posSample.getTags()[0]);
		assertEquals("NN", posSample.getTags()[1]);
		assertEquals("NN", posSample.getTags()[2]);
	}

	@Test
	public void testMain() {
		POSTagDictCreator.main(new String[] { "src/test/resources/posSamples.opennlp",
				"src/test/resources/tagDict.test.delete.xml", "true" });
		File dictFile = new File("src/test/resources/tagDict.test.delete.xml");
		assertTrue(dictFile.exists());
		assertTrue(dictFile.length() > 100);

		// Now load the created dictionary file. 
		try (InputStream is = FileUtils.openInputStream(dictFile)) {
			POSDictionary loadedPOSDictionary = POSDictionary.create(is);
			assertTrue(loadedPOSDictionary.isCaseSensitive());
			String[] expectedTagsLower = new String[] { "TEST2", "JJ" };
			String[] expectedTagsUpper = new String[] { "TEST", "JJ" };
			assertArrayEquals(expectedTagsLower, loadedPOSDictionary.getTags("small"));
			assertArrayEquals(expectedTagsUpper, loadedPOSDictionary.getTags("Small"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCreatePOSTagDict() {
		POSDictionary posDictionary =
				POSTagDictCreator.createPOSTagDict(new File("src/test/resources/posSamples.opennlp"), false);
		String[] expectedTags = new String[] { "TEST2", "TEST", "JJ" };
		assertArrayEquals(expectedTags, posDictionary.getTags("Small"));
		assertArrayEquals(expectedTags, posDictionary.getTags("small"));
	}

	@Test
	public void testCreatePOSTagDictCaseSensitive() {
		POSDictionary posDictionary =
				POSTagDictCreator.createPOSTagDict(new File("src/test/resources/posSamples.opennlp"), true);
		String[] expectedTagsLower = new String[] { "TEST2", "JJ" };
		String[] expectedTagsUpper = new String[] { "TEST", "JJ" };
		assertArrayEquals(expectedTagsLower, posDictionary.getTags("small"));
		assertArrayEquals(expectedTagsUpper, posDictionary.getTags("Small"));
	}
}
