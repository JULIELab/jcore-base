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
package de.julielab.jcore.utility;

import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
// import de.julielab.jcore.types.Annotation;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XmlCasDeserializer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jcore.types.Abbreviation;
import de.julielab.jcore.types.ConceptMention;
import de.julielab.jcore.types.DocumentAnnotation;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Token;

public class JCoReAnnotationToolsTest extends TestCase {

	/**
	 * Logger for this class
	 */
	private static final Logger LOG = LoggerFactory.getLogger(JCoReAnnotationToolsTest.class);

	JCas jcas;
	public final String DESC_TEST_ANALYSIS_ENGINE = "src/test/resources/AETestDescriptor.xml";

	protected void setUp() throws Exception {

		// get a CAS/JCas
		CAS cas = CasCreationUtils.createCas(UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
				new XMLInputSource(DESC_TEST_ANALYSIS_ENGINE)));
		jcas = cas.getJCas();
		jcas.reset();

		jcas.setDocumentText("ABFCDEAFBABDeeeeeeeeeeeeeeeeee.");
		Annotation e1 = new Annotation(jcas);
		e1.setBegin(3);
		e1.setEnd(5);
		e1.addToIndexes();

		Annotation e2 = new Annotation(jcas);
		e2.setBegin(7);
		e2.setEnd(10);
		e2.addToIndexes();

		Annotation e3 = new Annotation(jcas);
		e3.setBegin(14);
		e3.setEnd(15);
		e3.addToIndexes();

		Annotation e4 = new Annotation(jcas);
		e4.setBegin(20);
		e4.setEnd(25);
		e4.addToIndexes();
	}

	// TODO only Exception werfen
	public void testGetAnnotationAtOffset() throws SecurityException, IllegalArgumentException, ClassNotFoundException,
			NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

		LOG.debug("testGetAnnotationAtOffset() - testing getAnnotationAtOffset(..)");
		Annotation entity = new Annotation(jcas);

		// this annotation does exist
		Annotation anno = JCoReAnnotationTools.getAnnotationAtOffset(jcas, entity.getClass().getCanonicalName(), 3, 5);
		assertTrue((anno != null) && (anno instanceof Annotation));

		// this annotation does not exist
		anno = JCoReAnnotationTools.getAnnotationAtOffset(jcas, entity.getClass().getCanonicalName(), 1, 2);
		assertTrue(anno == null);
	}

	// TODO only Exception werfen
	public void testGetOverlappingAnnotation() throws SecurityException, IllegalArgumentException,
			ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
			InvocationTargetException {

		LOG.debug("testGetOverlappingAnnotation() - testing getOverlappingAnnotation(..)");
		Annotation entity = new Annotation(jcas);

		// this annotation does exist (same offset)
		Annotation anno = JCoReAnnotationTools.getOverlappingAnnotation(jcas, entity.getClass().getCanonicalName(), 3, 5);
		assertTrue((anno != null) && (anno instanceof Annotation));

		// this annotation does exist (annotation larger)
		anno = JCoReAnnotationTools.getOverlappingAnnotation(jcas, entity.getClass().getCanonicalName(), 8, 10);
		assertTrue((anno != null) && (anno instanceof Annotation));

		// this annotation does exist (annotation smaller)
		anno = JCoReAnnotationTools.getOverlappingAnnotation(jcas, entity.getClass().getCanonicalName(), 13, 16);
		assertTrue((anno != null) && (anno instanceof Annotation));

		// this annotation does exist (annotation completely surrounded by the existing annotation)
		anno = JCoReAnnotationTools.getOverlappingAnnotation(jcas, entity.getClass().getCanonicalName(), 21, 24);
		assertTrue((anno != null) && (anno instanceof Annotation));
	}

	// TODO only Exception werfen
	public void testGetAnnotationByClassName() throws SecurityException, IllegalArgumentException,
			ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
			InvocationTargetException {

		LOG.debug("testGetAnnotationByClassName() - testing getAnnotationObject(..)");
		Annotation entity = new Annotation(jcas);
		Annotation anno = JCoReAnnotationTools.getAnnotationByClassName(jcas, entity.getClass().getCanonicalName());
		assertTrue(anno instanceof Annotation);
	}

	public void testGetPartiallyOverlappingAnnotationOtherType() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("wort");
		Token t = new Token(jcas, 0, 4);
		EntityMention em = new EntityMention(jcas, 0, 4);
		t.addToIndexes();
		em.addToIndexes();
		Token overlappingToken = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, em, Token.class);
		assertEquals(t, overlappingToken);
		EntityMention overlappingEm = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, t, EntityMention.class);
		assertEquals(em, overlappingEm);

		jcas.reset();
		jcas.setDocumentText("ouinnerter");
		t = new Token(jcas, 0, 10);
		em = new EntityMention(jcas, 2, 7);
		t.addToIndexes();
		em.addToIndexes();
		overlappingToken = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, em, Token.class);
		assertEquals(t, overlappingToken);
		overlappingEm = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, t, EntityMention.class);
		assertEquals(em, overlappingEm);

		jcas.reset();
		jcas.setDocumentText("overlapping");
		t = new Token(jcas, 0, 7);
		em = new EntityMention(jcas, 3, 11);
		t.addToIndexes();
		em.addToIndexes();
		overlappingToken = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, em, Token.class);
		assertEquals(t, overlappingToken);
		overlappingEm = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, t, EntityMention.class);
		assertEquals(em, overlappingEm);

		jcas.reset();
		jcas.setDocumentText("not overlapping");
		t = new Token(jcas, 0, 3);
		em = new EntityMention(jcas, 4, 15);
		t.addToIndexes();
		em.addToIndexes();
		overlappingToken = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, em, Token.class);
		assertNull(overlappingToken);
		overlappingEm = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, t, EntityMention.class);
		assertNull(overlappingEm);

		jcas.reset();
		jcas.setDocumentText("This is a longer sentence.");
		Token t1 = new Token(jcas, 0, 4);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 5, 7);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 8, 9);
		t3.addToIndexes();
		Token t4 = new Token(jcas, 10, 16);
		t4.addToIndexes();
		Token t5 = new Token(jcas, 17, 25);
		t5.addToIndexes();
		Token t6 = new Token(jcas, 25, 26);
		t6.addToIndexes();
		// Exact
		EntityMention em1 = new EntityMention(jcas, 0, 4);
		em1.addToIndexes();
		// Suffix of first token, prefix of second
		EntityMention em2 = new EntityMention(jcas, 2, 6);
		em2.addToIndexes();
		// Contained in "longer"
		EntityMention em3 = new EntityMention(jcas, 11, 15);
		em3.addToIndexes();
		// Contains sentence and .
		EntityMention em4 = new EntityMention(jcas, 17, 26);
		em4.addToIndexes();

		DocumentAnnotation docAnn = new DocumentAnnotation(jcas, 0, 20);
		docAnn.addToIndexes();

		overlappingToken = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, em1, Token.class);
		assertEquals(t1, overlappingToken);
		overlappingToken = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, em2, Token.class);
		assertEquals(t1, overlappingToken);
		overlappingToken = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, em3, Token.class);
		assertEquals(t4, overlappingToken);
		overlappingToken = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, em4, Token.class);
		assertEquals(t5, overlappingToken);
	}

	@Test
	public void testIncludedAnnotations() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("start-overlap include include include end-overlap");
		// Should not be in the result list because is overlapping (start).
		Token t1 = new Token(jcas, 0, 13);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 14, 21);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 22, 29);
		t3.addToIndexes();
		Token t4 = new Token(jcas, 30, 37);
		t4.addToIndexes();
		// Should not be in the result list because is overlapping (emd).
		Token t5 = new Token(jcas, 38, 49);
		t5.addToIndexes();
		// Should be in the result with the exact same offsets
		Token t6 = new Token(jcas, 6, 41);
		t6.addToIndexes();
		EntityMention em = new EntityMention(jcas, 6, 41);
		em.addToIndexes();

		DocumentAnnotation docAnn = new DocumentAnnotation(jcas, 0, 20);
		docAnn.addToIndexes();

		List<Token> includedAnnotations = JCoReAnnotationTools.getIncludedAnnotations(jcas, em, Token.class);

		assertEquals("Wrong amount of included tokens returned", 4, includedAnnotations.size());

		for (int i = 0; i < includedAnnotations.size(); i++) {
			Token includedToken = includedAnnotations.get(i);
			if (i == 0) {
				assertEquals(t6, includedToken);
			} else if (i == 1) {
				assertEquals(t2, includedToken);
			} else if (i == 2) {
				assertEquals(t3, includedToken);
			} else {
				assertEquals(t4, includedToken);
			}
		}
	}

	@Test
	public void testIncludingAnnotations() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("long form includes short form");
		Token t1 = new Token(jcas, 0, 4);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 5, 9);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 10, 18);
		t3.addToIndexes();
		Token t4 = new Token(jcas, 19, 24);
		t4.addToIndexes();
		Token t5 = new Token(jcas, 25, 29);
		t5.addToIndexes();
		EntityMention em1 = new EntityMention(jcas, 0, 29);
		em1.addToIndexes();
		EntityMention em2 = new EntityMention(jcas, 19, 29);
		em2.addToIndexes();

		DocumentAnnotation docAnn = new DocumentAnnotation(jcas, 0, 29);
		docAnn.addToIndexes();

		EntityMention includedAnnotation = JCoReAnnotationTools.getIncludingAnnotation(jcas, t4, EntityMention.class);

		assertNotNull(includedAnnotation);
		assertEquals(em1, includedAnnotation);
	}
	
	@Test
	public void testNearestIncludingAnnotations() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("long form includes short form");
		Token t1 = new Token(jcas, 0, 4);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 5, 9);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 10, 18);
		t3.addToIndexes();
		Token t4 = new Token(jcas, 19, 24);
		t4.addToIndexes();
		Token t5 = new Token(jcas, 25, 29);
		t5.addToIndexes();
		EntityMention em1 = new EntityMention(jcas, 0, 29);
		em1.addToIndexes();
		EntityMention em2 = new EntityMention(jcas, 19, 29);
		em2.addToIndexes();

		DocumentAnnotation docAnn = new DocumentAnnotation(jcas, 0, 29);
		docAnn.addToIndexes();

		EntityMention includedAnnotation = JCoReAnnotationTools.getNearestIncludingAnnotation(jcas, t4, EntityMention.class);

		assertNotNull(includedAnnotation);
		assertEquals(em2, includedAnnotation);
	}

	@Test
	public void testIncludeAnnotationsOnDocument() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		XmlCasDeserializer.deserialize(new FileInputStream("src/test/resources/10803599.xmi"), jcas.getCas());
		FSIterator<Annotation> it = jcas.getAnnotationIndex(Abbreviation.type).iterator();

		assertTrue(it.hasNext());
		Abbreviation pthrpAbbr = (Abbreviation) it.next();
		// At first, only check we got the abbreviation we were looking for
		assertEquals("PTHrP", pthrpAbbr.getCoveredText());
		Annotation fullform = pthrpAbbr.getTextReference();
		assertEquals("PTH-related peptide", fullform.getCoveredText());

		FSIterator<Annotation> geneIt = jcas.getAnnotationIndex(Gene.type).iterator();
		assertTrue(geneIt.hasNext());
		// The gene we want to get as being "included" in the fullform annotation is the first gene in the document.
		Gene gene = (Gene) geneIt.next();

		// Now we expect to find the gene annotation perfectly matching the offsets of the fullform that is in the CAS.
		List<ConceptMention> includedAnnotations = JCoReAnnotationTools.getIncludedAnnotations(jcas, fullform,
				ConceptMention.class);
		assertEquals(1, includedAnnotations.size());
		Gene geneAnnotation = (Gene) includedAnnotations.get(0);
		assertEquals(gene, geneAnnotation);
	}

	@Test
	public void testGetPartiallyOverlappingAnnotationOnDocument() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		XmlCasDeserializer.deserialize(new FileInputStream("src/test/resources/10803599.xmi"), jcas.getCas());

		Abbreviation abbrev = (Abbreviation) JCoReAnnotationTools.getAnnotationAtOffset(jcas,
				Abbreviation.class.getCanonicalName(), 247, 252);
		assertNotNull(abbrev);

		ConceptMention emAcronym = JCoReAnnotationTools
				.getPartiallyOverlappingAnnotation(jcas, abbrev, ConceptMention.class);

		assertNotNull(emAcronym);
		// This annotation is actually annotation we would have liked to reject because a longer annotation of the same
		// type (Gene) is already covering the whole abbreviation. This did not work correctly, thus this test. Delete
		// this annotation so we should find the longer annotation then.
		emAcronym.removeFromIndexes();

		// Now we should find the overlapping Gene.
		emAcronym = JCoReAnnotationTools.getPartiallyOverlappingAnnotation(jcas, abbrev, ConceptMention.class);
		assertNotNull(emAcronym);
	}

	@Test
	public void testGetAnnotationAtMatchingOffsets() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("davor darin dahinter");
		Token t1 = new Token(jcas, 0, 5);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 6, 11);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 12, 20);
		t3.addToIndexes();

		EntityMention em = new EntityMention(jcas, 6, 11);
		em.addToIndexes();

		DocumentAnnotation docAnn = new DocumentAnnotation(jcas, 0, 20);
		docAnn.addToIndexes();

		EntityMention result = JCoReAnnotationTools.getAnnotationAtMatchingOffsets(jcas, t2, EntityMention.class);
		assertEquals(em, result);

	}
	
	@Test
	public void testGetNearestOverlappingAnnotations1() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("aaa aabb bbbb b");
		Token t1 = new Token(jcas, 0, 3);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 4, 8);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 9, 13);
		t3.addToIndexes();
		Token t4 = new Token(jcas, 14, 15);
		t4.addToIndexes();

		EntityMention em = new EntityMention(jcas, 6, 15);
		em.addToIndexes();

		List<Token> result = JCoReAnnotationTools.getNearestOverlappingAnnotations(jcas, em, Token.class);
		assertEquals(result.get(0), t2);
		assertEquals(result.get(1), t3);
		assertEquals(result.get(2), t4);
	}
	
	@Test
	public void testGetNearestOverlappingAnnotations2() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("aaa aabb bbbb baa aa");
		Token t1 = new Token(jcas, 0, 3);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 4, 8);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 9, 13);
		t3.addToIndexes();
		Token t4 = new Token(jcas, 14, 18);
		t4.addToIndexes();
		Token t5 = new Token(jcas, 19, 21);
		t5.addToIndexes();

		EntityMention em = new EntityMention(jcas, 6, 15);
		em.addToIndexes();

		List<Token> result = JCoReAnnotationTools.getNearestOverlappingAnnotations(jcas, em, Token.class);
		assertEquals(t2, result.get(0));
		assertEquals(t3, result.get(1));
		assertEquals(t4, result.get(2));
		assertEquals(3, result.size());
	}
	
	@Test
	public void testGetNearestOverlappingAnnotations3() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("(bbb)");
		Token t1 = new Token(jcas, 0, 1);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 1, 4);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 4, 5);
		t3.addToIndexes();

		EntityMention em = new EntityMention(jcas, 1, 4);
		em.addToIndexes();

		List<Token> result = JCoReAnnotationTools.getNearestOverlappingAnnotations(jcas, em, Token.class);
		assertEquals(t2, result.get(0));
		assertEquals(1, result.size());
	}
	
	@Test
	public void testGetLastOverlappingAnnotation() throws Exception {
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		jcas.setDocumentText("aaa aabb bbbb baa aa");
		Token t1 = new Token(jcas, 0, 3);
		t1.addToIndexes();
		Token t2 = new Token(jcas, 4, 8);
		t2.addToIndexes();
		Token t3 = new Token(jcas, 9, 13);
		t3.addToIndexes();
		Token t4 = new Token(jcas, 14, 18);
		t4.addToIndexes();
		Token t5 = new Token(jcas, 19, 21);
		t5.addToIndexes();

		EntityMention em = new EntityMention(jcas, 6, 15);
		em.addToIndexes();

		Token result = JCoReAnnotationTools.getLastOverlappingAnnotation(jcas, em, Token.class);
		assertEquals(t4, result);
	}
}
