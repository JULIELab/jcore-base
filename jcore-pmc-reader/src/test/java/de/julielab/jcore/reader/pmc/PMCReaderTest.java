package de.julielab.jcore.reader.pmc;

import static org.junit.Assert.*;

import java.io.FileOutputStream;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.types.Header;

public class PMCReaderTest {
	@Test
	public void testPmcReader1() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
				"src/test/resources/documents/PMC2847692.nxml.gz");
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		XmiCasSerializer.serialize(cas.getCas(), new FileOutputStream("test.xmi"));
	}

	@Test
	public void testHeader() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
				"src/test/resources/documents/PMC2847692.nxml.gz");
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		Header header = (Header) CasUtil.selectSingle(cas.getCas(), CasUtil.getAnnotationType(cas.getCas(), Header.class));
		assertNotNull(header);
		assertEquals("2847692", header.getDocId());
		assertNotNull(header.getPubTypeList());
		assertTrue(header.getPubTypeList().size() > 0);
		assertNotNull(header.getAuthors());
		assertEquals("© The Author(s) 2010", header.getCopyright());
	}
}
