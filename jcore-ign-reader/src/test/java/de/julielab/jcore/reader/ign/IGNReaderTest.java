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
package de.julielab.jcore.reader.ign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.Test;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.GeneResourceEntry;
import de.julielab.jcore.types.pubmed.Header;

public class IGNReaderTest {
	private static final String READER_DESCRIPTOR = "de.julielab.jcore.reader.ign.desc.jcore-ign-reader";
	private static final String TEST_TEXT = "src/test/resources/ign-train-snippet-text";
	private static final String TEST_ANNO = "src/test/resources/ign-train-snippet-annotations";
	private static final String[] typeSystems = new String[] {
			"de.julielab.jcore.types.jcore-document-meta-pubmed-types",
			"de.julielab.jcore.types.jcore-semantics-biology-types" };

	@Test
	public void testReader() throws Exception {
		CollectionReader reader = CollectionReaderFactory.createReader(READER_DESCRIPTOR, IGNReader.PARAM_INPUTDIR_TEXT,
				TEST_TEXT, IGNReader.PARAM_INPUTDIR_ANNO, TEST_ANNO);
		JCas jCas = JCasFactory.createJCas(typeSystems);
		assertTrue(reader.hasNext());
		reader.getNext(jCas.getCas());
		assertNotNull(jCas.getDocumentText());
		Header header = (Header) jCas.getAnnotationIndex(Header.type).iterator().next();
		String pubmedId = header.getDocId();
		assertNotNull(pubmedId);

		if (pubmedId.equals("10064899")) {
			assertEquals(1999, header.getPubTypeList(0).getPubDate().getYear());
			assertEquals(2, header.getPubTypeList(0).getPubDate().getMonth());
			Collection<Gene> genes = JCasUtil.select(jCas, Gene.class);
			assertEquals(7, genes.size());
			for (Gene gene : genes) {
				assertNotNull(gene.getResourceEntryList());
				FSArray resourceEntryList = gene.getResourceEntryList();
				assertEquals(1, resourceEntryList.size());
				GeneResourceEntry entry = (GeneResourceEntry) resourceEntryList.get(0);
				assertEquals("9606", entry.getTaxonomyId());
				assertEquals("10434", entry.getEntryId());
			}
		}
		if (pubmedId.equals("10072425")) {
			assertEquals(1999, header.getPubTypeList(0).getPubDate().getYear());
			assertEquals(4, header.getPubTypeList(0).getPubDate().getMonth());
		}
	}
}
