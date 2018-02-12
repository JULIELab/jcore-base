package de.julielab.jules.ae;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.julielab.jcore.ae.eventflattener.EventFlattener;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.ext.FlattenedRelation;

public class EventFlattenerTest {

	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory
			.getLogger(EventFlattenerTest.class);

	@Test
	public void testProcess() throws Exception, SecurityException {
		JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
		XmiCasDeserializer.deserialize(new FileInputStream(
				"src/test/resources/21499307.xmi"), jCas
				.getCas());

		AnalysisEngine flattener = AnalysisEngineFactory
				.createEngine(EventFlattener.class);
		flattener.process(jCas);

		FSIterator<Annotation> sentit = jCas.getAnnotationIndex(Sentence.type)
				.iterator();
		int sentenceCounter = 1;
		// we are interested in the 8th sentence because there is the only complex event structure there
		Sentence interestingSent = null;
		while (sentit.hasNext()) {
			Sentence s = (Sentence) sentit.next();
			switch (sentenceCounter) {
			case 3:
				assertEquals("Wrong number of flattened events in sentence "
						+ s.getCoveredText(), 2, countEventsInSentence(s));
				break;
			case 5:
				assertEquals("Wrong number of flattened events in sentence "
						+ s.getCoveredText(), 1, countEventsInSentence(s));
				break;
			case 6:
				assertEquals("Wrong number of flattened events in sentence "
						+ s.getCoveredText(), 2, countEventsInSentence(s));
				break;
			case 7:
				assertEquals("Wrong number of flattened events in sentence "
						+ s.getCoveredText(), 4, countEventsInSentence(s));
				break;
			case 8:
				assertEquals("Wrong number of flattened events in sentence "
						+ s.getCoveredText(), 6, countEventsInSentence(s));
				interestingSent = s;
				break;
			case 9:
				assertEquals("Wrong number of flattened events in sentence "
						+ s.getCoveredText(), 1, countEventsInSentence(s));
				break;
			default:
				assertEquals("Wrong number of flattened events in sentence "
						+ s.getCoveredText(), 0, countEventsInSentence(s));
			}
			sentenceCounter++;
		}
		FSIterator<Annotation> flateventit = jCas
				.getAnnotationIndex(FlattenedRelation.type).subiterator(interestingSent);
		while (flateventit.hasNext()) {
			FlattenedRelation fr = (FlattenedRelation) flateventit.next();
			if (fr.getId().equals("FE" + 13)) {
				// All arguments there?
				Set<String> expectedArguments = Sets.newHashSet("anti-apoptotic Bcl-2", "CSN5");
				for (int i = 0; i < fr.getArguments().size(); ++i)
					assertTrue("Unexpected argument: " + fr.getArguments(i).getCoveredText(), expectedArguments.remove(fr.getArguments(i).getCoveredText()));
				assertTrue("Expected arguments not found in relation: " + expectedArguments, expectedArguments.isEmpty());
				// Arguments correctly divided into agents and patients?
				assertEquals(1, fr.getAgents().size());
				assertEquals(1, fr.getPatients().size());
				assertEquals("CSN5", fr.getAgents(0).getCoveredText());
				assertEquals("anti-apoptotic Bcl-2", fr.getPatients(0).getCoveredText());
				// All participating (sub-)events there?
				assertEquals(3, fr.getRelations().size());
				Set<String> expectedRelations = Sets.newHashSet("depletion", "caused", "expression");
				for (int i = 0; i < fr.getRelations().size(); ++i)
					assertTrue("Unexpected relation: " + fr.getRelations(i).getCoveredText(), expectedRelations.remove(fr.getRelations(i).getCoveredText()));
				assertTrue(expectedRelations.isEmpty());
			}
		}
		
	}

	private int countEventsInSentence(Sentence s) throws CASRuntimeException,
			CASException {
		FSIterator<Annotation> flateventit = s.getCAS().getJCas()
				.getAnnotationIndex(FlattenedRelation.type).subiterator(s);
		int count = 0;
		while (flateventit.hasNext()) {
			@SuppressWarnings("unused")
			Annotation annotation = (Annotation) flateventit.next();
			count++;
		}
		return count;
	}
}
