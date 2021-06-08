/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.ae.opennlp.postag;

import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSSample;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
			Set<String> expectedTagsLower = new HashSet<>(Arrays.asList("TEST2", "JJ"));
			Set<String> expectedTagsUpper = new HashSet<>(Arrays.asList("TEST", "JJ"));
			assertEquals(expectedTagsLower, new HashSet<String>(Arrays.asList(loadedPOSDictionary.getTags("small"))));
			assertEquals(expectedTagsUpper, new HashSet<String>(Arrays.asList(loadedPOSDictionary.getTags("Small"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCreatePOSTagDict() {
		POSDictionary posDictionary =
				POSTagDictCreator.createPOSTagDict(new File("src/test/resources/posSamples.opennlp"), false);
		Set<String> expectedTags = new HashSet<>(Arrays.asList("TEST2", "TEST", "JJ"));
		assertEquals(expectedTags, new HashSet<String>(Arrays.asList(posDictionary.getTags("Small"))));
		assertEquals(expectedTags, new HashSet<String>(Arrays.asList(posDictionary.getTags("small"))));
	}

	@Test
	public void testCreatePOSTagDictCaseSensitive() {
		POSDictionary posDictionary =
				POSTagDictCreator.createPOSTagDict(new File("src/test/resources/posSamples.opennlp"), true);
		Set<String> expectedTagsLower = new HashSet<>(Arrays.asList("TEST2", "JJ"));
		Set<String> expectedTagsUpper = new HashSet<>(Arrays.asList("TEST", "JJ"));
		assertEquals(expectedTagsLower, new HashSet<String>(Arrays.asList(posDictionary.getTags("small"))));
		assertEquals(expectedTagsUpper, new HashSet<String>(Arrays.asList(posDictionary.getTags("Small"))));
	}
}
