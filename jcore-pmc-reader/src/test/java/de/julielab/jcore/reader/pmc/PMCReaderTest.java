package de.julielab.jcore.reader.pmc;

import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;

import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

public class PMCReaderTest {
	@Test
	public void testPmcReader1() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT, "src/test/resources/documents/PMC2847692.nxml.gz");
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		XmiCasSerializer.serialize(cas.getCas(), new FileOutputStream("test.xmi"));
	}
}
