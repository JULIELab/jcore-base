package de.julielab.jcore.reader.pmc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.types.Figure;
import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Journal;
import de.julielab.jcore.types.Section;
import de.julielab.jcore.types.Table;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.ManualDescriptor;

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
		Header header = (Header) CasUtil.selectSingle(cas.getCas(),
				CasUtil.getAnnotationType(cas.getCas(), Header.class));
		assertNotNull(header);
		assertEquals("2847692", header.getDocId());
		assertNotNull(header.getPubTypeList());
		assertTrue(header.getPubTypeList().size() > 0);
		assertEquals("Ambio", ((Journal) header.getPubTypeList(0)).getTitle());
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
		Iterator<AnnotationFS> secIt = CasUtil.iterator(cas.getCas(),
				CasUtil.getAnnotationType(cas.getCas(), Section.class));
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

	@Test
	public void testTables() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
				"src/test/resources/documents/PMC2847692.nxml.gz");
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		Iterator<AnnotationFS> tableIt = CasUtil.iterator(cas.getCas(),
				CasUtil.getAnnotationType(cas.getCas(), Table.class));
		assertTrue(tableIt.hasNext());
		int tablenum = 0;
		while (tableIt.hasNext()) {
			Table table = (Table) tableIt.next();
			assertNotNull(table.getObjectCaption());
			assertNotNull(table.getObjectTitle());
			Title tabelTitle = table.getObjectTitle();
			if (tablenum == 0) {
				assertEquals("Table 1", tabelTitle.getCoveredText());
				// the whitespace is actually a no-break space; note that the
				// last '1' is actually the digit 1 and not a part of the
				// codepoint
				assertEquals("Table\u00A01", table.getObjectLabel());
				assertTrue(table.getObjectCaption().getCoveredText().startsWith("Comparison of"));
				assertEquals("Tab1", table.getObjectId());
			}
			++tablenum;
		}
	}

	@Test
	public void testFigures() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
				"src/test/resources/documents/PMC2847692.nxml.gz");
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		Iterator<AnnotationFS> figureIt = CasUtil.iterator(cas.getCas(),
				CasUtil.getAnnotationType(cas.getCas(), Figure.class));
		assertTrue(figureIt.hasNext());
		int tablenum = 0;
		while (figureIt.hasNext()) {
			Figure figure = (Figure) figureIt.next();
			assertNotNull(figure.getObjectCaption());
			assertNotNull(figure.getObjectTitle());
			Title tabelTitle = figure.getObjectTitle();
			if (tablenum == 0) {
				assertEquals("Fig. 1", tabelTitle.getCoveredText());
				// the whitespace is actually a no-break space; note that the
				// last '1' is actually the digit 1 and not a part of the
				// codepoint
				assertEquals("Fig.\u00A01", figure.getObjectLabel());
				assertTrue(figure.getObjectCaption().getCoveredText().startsWith("The Baltic Sea"));
				assertEquals("Fig1", figure.getObjectId());
			}
			++tablenum;
		}
	}

	@Test
	public void testKeywords() throws Exception {
		JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
				"src/test/resources/documents/PMC2847692.nxml.gz");
		assertTrue(reader.hasNext());
		reader.getNext(cas.getCas());
		ManualDescriptor md = (ManualDescriptor) CasUtil.selectSingle(cas.getCas(),
				CasUtil.getAnnotationType(cas.getCas(), ManualDescriptor.class));
		assertNotNull(md);
		assertNotNull(md.getKeywordList());
		assertEquals(5, md.getKeywordList().size());

		Set<String> expectedKeywords = new HashSet<>(Arrays.asList("Baltic Sea Action Plan (BSAP)", "Costs", "Review",
				"Eutrophication", "Hazardous substances"));
		IntStream.range(0, md.getKeywordList().size())
				.forEach(i -> assertTrue("The keyword \"" + md.getKeywordList(i).getName() +"\" was not expected", expectedKeywords.remove(md.getKeywordList(i).getName())));
		assertTrue(expectedKeywords.isEmpty());
	}
}
