package de.julielab.jcore.consumer.entityevaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.TreeMap;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

public class EntityEvaluatorConsumerTest {

	public static final File DOC_DIR_FILE = new File("src/test/resources/documents");
	public static final File DOC_FILE_ENTITIES_WO_ID = new File("src/test/resources/entitiesWithoutId/7520377.xmi");
	public static final File OUTPUT_FILE = new File("src/test/resources/entityRecord.tsv");

	@Test
	public void testEntityEvaluatorConverter() throws Exception {
		AnalysisEngine converter = AnalysisEngineFactory.createEngine(EntityEvaluatorConsumer.class,
				EntityEvaluatorConsumer.PARAM_ENTITY_TYPE, "de.julielab.jcore.types.Gene",
				EntityEvaluatorConsumer.PARAM_ID_FEATURE_PATH, "/resourceEntryList[0]/entryId",
				EntityEvaluatorConsumer.PARAM_DOC_INFORMATION_TYPE, "de.julielab.jcore.types.pubmed.Header",
				EntityEvaluatorConsumer.PARAM_DOC_ID_FEATURE_PATH, "docId", EntityEvaluatorConsumer.PARAM_OUTPUT_FILE,
				OUTPUT_FILE.getAbsolutePath(), EntityEvaluatorConsumer.PARAM_ADDITIONAL_FEATURE_PATHS,
				new String[] { "/confidence", "/componentId" });
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");

		String[] documents = DOC_DIR_FILE.list();
		for (int i = 0; i < documents.length; i++) {
			String document = documents[i];
			XmiCasDeserializer.deserialize(
					new FileInputStream(new File(DOC_DIR_FILE.getAbsolutePath() + File.separator + document)),
					jcas.getCas());
			converter.process(jcas);
			if (i == 5)
				converter.batchProcessComplete();
		}
		converter.collectionProcessComplete();

		List<String> entityRecords = Files.readAllLines(OUTPUT_FILE.toPath());
		assertEquals(73, entityRecords.size());

		for (String record : entityRecords) {
			String[] fields = record.split("\\t");
			// In the test data, not all Genes have a confidence value set.
			assertTrue(fields.length == 6 || fields.length == 7);
		}

	}

	@Test
	public void testDiscardEntitiesWithoutId() throws Exception {
		// Check whether entities without IDs are indeed discarded when the
		// respective option is set. First check
		// without discarding, then with.
		AnalysisEngine converter = AnalysisEngineFactory.createEngine(EntityEvaluatorConsumer.class,
				EntityEvaluatorConsumer.PARAM_ENTITY_TYPE, "de.julielab.jcore.types.Gene",
				EntityEvaluatorConsumer.PARAM_ID_FEATURE_PATH, "/resourceEntryList[0]/entryId",
				// EntityEvaluatorConverter.PARAM_CONF_FEATURE_PATH,
				// "/confidence",
				// EntityEvaluatorConverter.PARAM_SYSTEM_FEATURE_PATH,
				// "/componentId",
				EntityEvaluatorConsumer.PARAM_DOC_INFORMATION_TYPE, "de.julielab.jcore.types.pubmed.Header",
				EntityEvaluatorConsumer.PARAM_DOC_ID_FEATURE_PATH, "docId",
				EntityEvaluatorConsumer.PARAM_DISCARD_ENTITIES_WO_ID, false, // <<<--------
																				// !!
				EntityEvaluatorConsumer.PARAM_OUTPUT_FILE, OUTPUT_FILE.getAbsolutePath(),
				EntityEvaluatorConsumer.PARAM_ADDITIONAL_FEATURE_PATHS, new String[] { "/confidence", "/componentId" });
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");

		XmiCasDeserializer.deserialize(new FileInputStream(new File(DOC_FILE_ENTITIES_WO_ID.getAbsolutePath())),
				jcas.getCas());
		converter.process(jcas);
		converter.batchProcessComplete();
		converter.collectionProcessComplete();

		List<String> entityRecords = Files.readAllLines(OUTPUT_FILE.toPath());
		assertEquals(6, entityRecords.size());

		for (String record : entityRecords) {
			String[] fields = record.split("\\t");
			// In the test data, not all Genes have a confidence value set.
			assertTrue(fields.length == 5);
		}

		converter = AnalysisEngineFactory.createEngine(EntityEvaluatorConsumer.class,
				EntityEvaluatorConsumer.PARAM_ENTITY_TYPE, "de.julielab.jcore.types.Gene",
				EntityEvaluatorConsumer.PARAM_ID_FEATURE_PATH, "/resourceEntryList[0]/entryId",
				EntityEvaluatorConsumer.PARAM_DOC_INFORMATION_TYPE, "de.julielab.jcore.types.pubmed.Header",
				EntityEvaluatorConsumer.PARAM_DOC_ID_FEATURE_PATH, "docId",
				EntityEvaluatorConsumer.PARAM_DISCARD_ENTITIES_WO_ID, true, // <<<------
																			// !!
				EntityEvaluatorConsumer.PARAM_OUTPUT_FILE, OUTPUT_FILE.getAbsolutePath(),
				EntityEvaluatorConsumer.PARAM_ADDITIONAL_FEATURE_PATHS, new String[] { "/confidence", "/componentId" });
		jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");

		XmiCasDeserializer.deserialize(new FileInputStream(new File(DOC_FILE_ENTITIES_WO_ID.getAbsolutePath())),
				jcas.getCas());
		converter.process(jcas);
		converter.batchProcessComplete();
		converter.collectionProcessComplete();

		entityRecords = Files.readAllLines(OUTPUT_FILE.toPath());
		assertEquals(5, entityRecords.size());

		for (String record : entityRecords) {
			String[] fields = record.split("\\t");
			// In the test data, not all Genes have a confidence value set.
			assertTrue(fields.length == 5);
		}
	}

	@Test
	public void testCreateNonWsOffsetMap() throws Exception {
		Method method = EntityEvaluatorConsumer.class.getDeclaredMethod("createNumWsMap", String.class);
		method.setAccessible(true);
		@SuppressWarnings("unchecked")
		TreeMap<Integer, Integer> numWsMap = (TreeMap<Integer, Integer>) method.invoke(null, "one two three");
		// first check the actual map entries (after each white space position
		// there should be an entry)
		assertEquals(new Integer(0), numWsMap.get(0));
		assertEquals(new Integer(1), numWsMap.get(4));
		assertEquals(new Integer(2), numWsMap.get(8));

		// now check the intended use; using the floor element, we should be
		// able to the correct value even for those positions we don't have an
		// explicit mapping for
		assertEquals(new Integer(0), numWsMap.floorEntry(2).getValue());
		assertEquals(new Integer(1), numWsMap.floorEntry(5).getValue());
		assertEquals(new Integer(2), numWsMap.floorEntry(11).getValue());
	}
}
