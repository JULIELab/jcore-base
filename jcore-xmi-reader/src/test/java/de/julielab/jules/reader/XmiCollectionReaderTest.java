package de.julielab.jules.reader;

import static org.junit.Assert.assertTrue;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jules.types.EventMention;
import de.julielab.jules.types.Gene;

public class XmiCollectionReaderTest {
	@Test
	public void testUimaFitIntegration() throws Exception {
		JCas cas = JCasFactory.createJCas("julie-all-types");
		
		CollectionReader reader = CollectionReaderFactory.createReader(XmiCollectionReader.class, XmiCollectionReader.PARAM_INPUTDIR, "src/test/resources/input");
		
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		
		// Check that we have got something in the XMI.
		assertTrue(cas.getDocumentText().length() > 100);
		assertTrue(cas.getAnnotationIndex(Gene.type).iterator().hasNext());
		assertTrue(cas.getAnnotationIndex(EventMention.type).iterator().hasNext());
	}
}
