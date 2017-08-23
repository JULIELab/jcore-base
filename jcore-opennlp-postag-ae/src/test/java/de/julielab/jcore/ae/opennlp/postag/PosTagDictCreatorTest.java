/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.opennlp.postag;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSSample;

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
