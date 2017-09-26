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
