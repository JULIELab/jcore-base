package de.julielab.jcore.reader.pubtator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Chemical;
import de.julielab.jcore.types.Disease;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Title;

public class PubtatorReaderTest {
	@Test
	public void testDocumentDirectory() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-mention-types",
				"de.julielab.jcore.types.jcore-semantics-biology-types",
				"de.julielab.jcore.types.jcore-document-structure-types",
				"de.julielab.jcore.types.jcore-document-meta-types");
		CollectionReader reader = CollectionReaderFactory.createReader(PubtatorReader.class, PubtatorReader.PARAM_INPUT,
				"src/test/resources/documents");
		assertTrue(reader.hasNext());
		
		Set<String> expectedDocIds = new HashSet<>(Arrays.asList("14656948", "17317680", "16158176"));
		while (reader.hasNext()) {
			reader.getNext(jcas.getCas());
			assertFalse(JCasUtil.select(jcas, Title.class).isEmpty());
			assertFalse(JCasUtil.select(jcas, AbstractText.class).isEmpty());
			assertFalse(JCasUtil.select(jcas, Disease.class).isEmpty());
			assertFalse(JCasUtil.select(jcas, Chemical.class).isEmpty());
			Header header = JCasUtil.selectSingle(jcas, Header.class);
			assertNotNull(header);
			String docId = header.getDocId();
			assertNotNull(docId);
			assertTrue(expectedDocIds.remove(docId));

			if (docId.equals("14656948")) {
				Collection<Gene> genes = JCasUtil.select(jcas, Gene.class);
				assertEquals(4, genes.size());
				// there should be 4 gene mentions
				int i = 0;
				for (Gene g : genes) {
					if (i < 3) {
						assertEquals("GST-P", g.getCoveredText());
						assertNotNull(g.getResourceEntryList());
						assertEquals("24426", g.getResourceEntryList(0).getEntryId());
					} else {
						assertEquals("gamma-glutamylcysteine synthetase", g.getCoveredText());
						assertNotNull(g.getResourceEntryList());
						assertEquals("25283", g.getResourceEntryList(0).getEntryId());
					}
					++i;
				}
			}
			jcas.reset();
		}
		assertTrue("The following IDs have not been read: " + expectedDocIds, expectedDocIds.isEmpty());
	}
}
