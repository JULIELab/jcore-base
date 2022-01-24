/**
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author:
 * <p>
 * Description:
 **/
package de.julielab.jcore.reader.pmc;

import de.julielab.jcore.types.*;
import de.julielab.jcore.types.pubmed.InternalReference;
import de.julielab.jcore.types.pubmed.ManualDescriptor;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;

public class PMCReaderTest {
    @Test
    public void testPmcReader1() throws Exception {
        // read a single file, parse it and right it to XMI for manual review
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
        assertTrue(reader.hasNext());
        int count = 0;
        while (reader.hasNext()) {
            reader.getNext(cas.getCas());
            ++count;
        }
        assertEquals(1, count);
    }

    @Test
    public void testPmcReader2() throws Exception {
        // read a whole directory with subdirectories
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive", PMCReader.PARAM_RECURSIVELY, true);
        assertTrue(reader.hasNext());
        Set<String> foundDocuments = new HashSet<>();
        while (reader.hasNext()) {
            reader.getNext(cas.getCas());

            Header header = (Header) CasUtil.selectSingle(cas.getCas(),
                    CasUtil.getAnnotationType(cas.getCas(), Header.class));
            assertNotNull(header);
            foundDocuments.add(header.getDocId());
            assertNotNull(header.getPubTypeList());
            assertTrue(header.getPubTypeList().size() > 0);
            assertNotNull(((Journal) header.getPubTypeList(0)).getTitle());
            assertNotNull(((Journal) header.getPubTypeList(0)).getIssue());
            assertNotNull(((Journal) header.getPubTypeList(0)).getVolume());
            assertNotNull(((Journal) header.getPubTypeList(0)).getPages());
            assertTrue(((Journal) header.getPubTypeList(0)).getTitle().length() > 0);
            assertNotNull(header.getAuthors());
            assertTrue(header.getAuthors().size() > 0);
            assertNotNull(header.getAuthors(0));

            Collection<Caption> captions = JCasUtil.select(cas, Caption.class);
            for (Caption c : captions)
                assertNotNull(c.getCaptionType());
            Collection<Title> titles = JCasUtil.select(cas, Title.class);
            for (Title t : titles)
                assertNotNull(t.getTitleType());

            cas.reset();
        }
        assertThat(foundDocuments).containsExactlyInAnyOrder("PMC2847692", "PMC3201365", "PMC4257438", "PMC2758189", "PMC2970367");
    }

    @Test
    public void testPmcReaderRecursiveZip() throws Exception {
        // read a whole directory with subdirectories
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-zip", PMCReader.PARAM_RECURSIVELY, true, PMCReader.PARAM_SEARCH_ZIP, true);
        assertTrue(reader.hasNext());
        Set<String> foundDocuments = new HashSet<>();
        while (reader.hasNext()) {
            reader.getNext(cas.getCas());

            Header header = (Header) CasUtil.selectSingle(cas.getCas(),
                    CasUtil.getAnnotationType(cas.getCas(), Header.class));
            assertNotNull(header);
            foundDocuments.add(header.getDocId());
            assertNotNull(header.getPubTypeList());
            assertTrue(header.getPubTypeList().size() > 0);
            assertNotNull(((Journal) header.getPubTypeList(0)).getTitle());
            assertNotNull(((Journal) header.getPubTypeList(0)).getIssue());
            assertNotNull(((Journal) header.getPubTypeList(0)).getVolume());
            assertNotNull(((Journal) header.getPubTypeList(0)).getPages());
            assertTrue(((Journal) header.getPubTypeList(0)).getTitle().length() > 0);
            assertNotNull(header.getAuthors());
            assertTrue(header.getAuthors().size() > 0);
            assertNotNull(header.getAuthors(0));

            Collection<Caption> captions = JCasUtil.select(cas, Caption.class);
            for (Caption c : captions)
                assertNotNull(c.getCaptionType());
            Collection<Title> titles = JCasUtil.select(cas, Title.class);
            for (Title t : titles)
                assertNotNull(t.getTitleType());

            cas.reset();
        }
        assertThat(foundDocuments).containsExactlyInAnyOrder("PMC2847692", "PMC3201365", "PMC4257438", "PMC2758189", "PMC2970367");
    }

    @Test
    public void testPmcReaderWhitelist() throws Exception {
        // read a whole directory with subdirectories
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-zip",
                PMCReader.PARAM_RECURSIVELY, true,
                PMCReader.PARAM_SEARCH_ZIP, true,
                PMCReader.PARAM_WHITELIST, "src/test/resources/whitelist.txt");
        assertTrue(reader.hasNext());
        Set<String> foundDocuments = new HashSet<>();
        while (reader.hasNext()) {
            reader.getNext(cas.getCas());

            Header header = (Header) CasUtil.selectSingle(cas.getCas(),
                    CasUtil.getAnnotationType(cas.getCas(), Header.class));
            assertNotNull(header);
            foundDocuments.add(header.getDocId());
            cas.reset();
        }
        assertThat(foundDocuments).containsExactlyInAnyOrder("PMC2847692", "PMC2758189");
    }

    @Test
    public void testTitle() throws Exception {
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
        assertTrue(reader.hasNext());
        reader.getNext(cas.getCas());
        Collection<Title> titles = JCasUtil.select(cas, Title.class);
        for (Title t : titles) {
            if (t.getClass().equals(SectionTitle.class)) {
                assertEquals("section", t.getTitleType());
            }
        }
    }

    @Test
    public void testHeader() throws Exception {
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
        assertTrue(reader.hasNext());
        reader.getNext(cas.getCas());
        Header header = (Header) CasUtil.selectSingle(cas.getCas(),
                CasUtil.getAnnotationType(cas.getCas(), Header.class));
        assertNotNull(header);
        assertEquals("PMC2847692", header.getDocId());
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
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
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
            if (sec.getSectionId().equals("Sec3"))
                assertEquals(1, sec.getDepth());

            ++secnum;
        }
    }

    @Test
    public void testTables() throws Exception {
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
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
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
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
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
        assertTrue(reader.hasNext());
        reader.getNext(cas.getCas());
        ManualDescriptor md = (ManualDescriptor) CasUtil.selectSingle(cas.getCas(),
                CasUtil.getAnnotationType(cas.getCas(), ManualDescriptor.class));
        assertNotNull(md);
        assertNotNull(md.getKeywordList());
        assertEquals(5, md.getKeywordList().size());
        assertNotNull(md.getKeywordList(0));

        Set<String> expectedKeywords = new HashSet<>(Arrays.asList("Baltic Sea Action Plan (BSAP)", "Costs", "Review",
                "Eutrophication", "Hazardous substances"));
        IntStream.range(0, md.getKeywordList().size())
                .forEach(i -> assertTrue(expectedKeywords.remove(md.getKeywordList(i).getName()),
                        "The keyword \"" + md.getKeywordList(i).getName() + "\" was not expected"));
        assertTrue(expectedKeywords.isEmpty());
    }

    @Test
    public void testSectionTitlesWithLabels() throws Exception {
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-misc/PMC3098455.nxml.gz");
        reader.getNext(cas.getCas());
        Iterator<AnnotationFS> secIt = CasUtil.iterator(cas.getCas(),
                CasUtil.getAnnotationType(cas.getCas(), Section.class));
        assertTrue(secIt.hasNext());
        int i = 0;
        while (secIt.hasNext()) {
            Section sec = (Section) secIt.next();
            if (i == 1) {
                assertEquals("Materials and methods", sec.getSectionHeading().getCoveredText());
                assertEquals("2", sec.getLabel());
            }
            ++i;
        }
    }

    @Test
    public void testEmptyWrappingAbstractSection() throws Exception {
        // This document wraps its abstract sections in one completely empty
        // section. We choose to not realize this empty wrapping section at all
        // because it would mess up easy access to abstract sections.
        // Thus we test each abstract section we come across and check that it
        // is the one we expect, i.e. not the wrapper with no title.
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-misc/PMC2836310.nxml.gz");
        reader.getNext(cas.getCas());
        Iterator<AnnotationFS> secIt = CasUtil.iterator(cas.getCas(),
                CasUtil.getAnnotationType(cas.getCas(), AbstractSection.class));
        assertTrue(secIt.hasNext());
        int i = 0;
        while (secIt.hasNext()) {
            AbstractSection sec = (AbstractSection) secIt.next();
            if (i == 1) {
                assertEquals("Background", sec.getAbstractSectionHeading().getCoveredText());
            }
            if (i == 2) {
                assertEquals("Results", sec.getAbstractSectionHeading().getCoveredText());
            }
            if (i == 3) {
                assertEquals("Conclusions", sec.getAbstractSectionHeading().getCoveredText());
            }
            assertTrue(i < 4);
            ++i;
        }
    }

    @Test
    public void testFigureReferencesAnnotated() throws Exception {
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
        reader.getNext(cas.getCas());
        Collection<InternalReference> refs = JCasUtil.select(cas, InternalReference.class);
        List<InternalReference> figRefs = refs.stream().filter(r -> r.getReftype().equalsIgnoreCase("figure")).collect(Collectors.toList());
        assertThat(figRefs).hasSize(2);
        assertThat(figRefs).extracting("refid").containsExactly("Fig1", "Fig2");
    }

    @Test
    public void testBibliographyReferencesAnnotated() throws Exception {
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz");
        reader.getNext(cas.getCas());
        Collection<InternalReference> refs = JCasUtil.select(cas, InternalReference.class);
        // Without a filter on bibliographic references, there should 76 references to bibliography
        List<InternalReference> bibliography = refs.stream().filter(r -> r.getReftype().equalsIgnoreCase("bibliography")).collect(Collectors.toList());
        assertThat(bibliography).hasSize(76);

        // RegEx for something like "2004a"
        Matcher yearReferenceMatcher = Pattern.compile("[0-9]{4}[ab]?").matcher(cas.getDocumentText());
        int numReferencePatternsInText = 0;
        while (yearReferenceMatcher.find()) {
            ++numReferencePatternsInText;
        }
        // Some found patterns are no references, thus the number is higher than that of the references.
        assertThat(numReferencePatternsInText).isEqualTo(84);
    }

    @Test
    public void testBibliographyReferencesOmitted() throws Exception {
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz",
                PMCMultiplierReader.PARAM_OMIT_BIB_REFERENCES, true);
        reader.getNext(cas.getCas());
        Collection<InternalReference> refs = JCasUtil.select(cas, InternalReference.class);
        // Since we set the omission parameter to true, there should be no bibliographic references
        List<InternalReference> bibliography = refs.stream().filter(r -> r.getReftype().equalsIgnoreCase("bibliography")).collect(Collectors.toList());
        assertThat(bibliography).isEmpty();

        // RegEx for something like "2004a"
        Matcher yearReferenceMatcher = Pattern.compile("[0-9]{4}[ab]?").matcher(cas.getDocumentText());
        int numReferencePatternsInText = 0;
        while (yearReferenceMatcher.find()) {
            ++numReferencePatternsInText;
        }
        // In the test above, where we have the same document but with bib. references, there were 84 occurrences
        // of the pattern. 76 of those were actual references. Thus, after removing the references, 8 pattern
        // occurrences should remain.
        assertThat(numReferencePatternsInText).isEqualTo(8);
    }

    @Test
    public void testPmcReaderDescriptor() throws Exception {
        // read a whole directory with subdirectories
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.pmc.desc.jcore-pmc-reader", PMCReader.PARAM_INPUT,
                "src/test/resources/documents-zip", PMCReader.PARAM_RECURSIVELY, true, PMCReader.PARAM_SEARCH_ZIP, true);
        assertTrue(reader.hasNext());
        Set<String> foundDocuments = new HashSet<>();
        while (reader.hasNext()) {
            reader.getNext(cas.getCas());

            Header header = (Header) CasUtil.selectSingle(cas.getCas(),
                    CasUtil.getAnnotationType(cas.getCas(), Header.class));
            assertNotNull(header);
            foundDocuments.add(header.getDocId());

            cas.reset();
        }
        assertThat(foundDocuments).containsExactlyInAnyOrder("PMC2847692", "PMC3201365", "PMC4257438", "PMC2758189", "PMC2970367");
    }

    @Test
    public void testDocWithSubarticleFront() throws Exception {
        // In this test we have a document that is actually a list of poster references. Each poster
        // is listed in a sub-article element which has its own front element.
        // In a previous version of the reader, for each of those front elements a new Header was
        // created. This shouldn't be done, at least not the way it was done back then. Because
        // the FrontParser assumes only one front element and specifies absolute XPath expression
        // to retrieve the Header feature values which are then the wrong ones. Also, most
        // AEs expect only one Header to be present.
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.pmc.desc.jcore-pmc-reader",
                PMCReader.PARAM_INPUT,
                "src/test/resources/documents-errorcauses/PMC3747765.nxml.gz", PMCReader.PARAM_RECURSIVELY, false, PMCReader.PARAM_SEARCH_ZIP, false);
        assertTrue(reader.hasNext());
        while (reader.hasNext()) {
            reader.getNext(cas.getCas());

            assertThatCode(() -> CasUtil.selectSingle(cas.getCas(),
                    CasUtil.getAnnotationType(cas.getCas(), Header.class))).doesNotThrowAnyException();

            cas.reset();
        }
    }

    @Test
    public void testExtractIdFromFilename() throws Exception {
        // read a single file, parse it and right it to XMI for manual review
        JCas cas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types");
        CollectionReader reader = CollectionReaderFactory.createReader(PMCReader.class, PMCReader.PARAM_INPUT,
                "src/test/resources/documents-recursive/PMC2847692.nxml.gz", PMCReader.PARAM_EXTRACT_ID_FROM_FILENAME, true);
        assertTrue(reader.hasNext());
        reader.getNext(cas.getCas());
		String docId = ((Header)cas.getAnnotationIndex(Header.type).iterator().next()).getDocId();
		assertEquals(docId, "PMC2847692");
    }
}
