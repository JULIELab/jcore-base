package de.julielab.jcore.reader.pmc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.util.Iterator;

import org.apache.uima.cas.impl.TypeSystemUtils;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.TypeSystemUtil;
import org.junit.Test;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.Title;

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
		assertEquals("Ambio", ((Journal)header.getPubTypeList(0)).getTitle());
		assertNotNull(header.getAuthors());
		assertTrue(header.getAuthors().size() > 0);
		assertEquals("Katarina", header.getAuthors(0).getForeName());
		assertEquals("© The Author(s) 2010", header.getCopyright());
	}
	
	@Test
	public void testSections() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
				"src/test/resources/documents/PMC2847692.nxml.gz");
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		Iterator<AnnotationFS> secIt = CasUtil.iterator(cas.getCas(), CasUtil.getAnnotationType(cas.getCas(), Section.class));
		assertTrue(secIt.hasNext());
		int secnum = 0;
		while (secIt.hasNext()) {
			Section sec = (Section) secIt.next();
			assertNotNull(sec.getSectionHeading());
			Title sectionHeading = sec.getSectionHeading();
			if (secnum == 0) {
				assertEquals("Introduction", sectionHeading.getCoveredText());
			}
			++secnum;
		}
	}
}
