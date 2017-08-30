package de.julielab.jcore.reader.xmi;

import static org.junit.Assert.assertTrue;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.reader.xmi.XmiCollectionReader;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.Gene;

public class XmiCollectionReaderTest {
	@Test
	public void testUimaFitIntegration() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		
		CollectionReader reader = CollectionReaderFactory.createReader(XmiCollectionReader.class, XmiCollectionReader.PARAM_INPUTDIR, "src/test/resources/input");
		
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		
		// Check that we have got something in the XMI.
		assertTrue(cas.getDocumentText().length() > 100);
		assertTrue(cas.getAnnotationIndex(Gene.type).iterator().hasNext());
		assertTrue(cas.getAnnotationIndex(EventMention.type).iterator().hasNext());
	}
}
