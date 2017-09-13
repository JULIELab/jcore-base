package de.julielab.jcore.consumer.entityevaluator;

import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.DOCUMENT_ID_COLUMN;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_COLUMN_DEFINITIONS;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_OUTPUT_COLUMNS;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_OUTPUT_FILE;
import static de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.PARAM_TYPE_PREFIX;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

public class DescriptorGenerator {
	public static void main(String[] args)
			throws ResourceInitializationException, FileNotFoundException, IOException, SAXException {
		AnalysisEngineDescription desc = AnalysisEngineFactory.createEngineDescription(EntityEvaluatorConsumer.class,
				PARAM_COLUMN_DEFINITIONS,
				new String[] { DOCUMENT_ID_COLUMN + ": Header = /docId", "geneid:Gene=/resourceEntryList[0]/entryId",
						"name:/:coveredText()" },
				PARAM_OUTPUT_COLUMNS, new String[] { DOCUMENT_ID_COLUMN, "geneid", "name" }, PARAM_TYPE_PREFIX,
				"de.julielab.jcore.types", PARAM_OUTPUT_FILE, "entities.tsv");
		String dest = "src/main/resources/de/julielab/jcore/consumer/entityevaluator/desc/jcore-julielab-entity-evaluator-consumer.xml";
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(
				dest))) {
			desc.toXML(os);
		}
		System.out.println("Descriptor written to " + dest);
	}
}
