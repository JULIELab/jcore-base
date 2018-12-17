package de.julielab.jcore.consumer.es;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.consumer.es.preanalyzed.Document;

public class JsonWriter extends AbstractCasToJsonConsumer {

	private static final Logger log = LoggerFactory.getLogger(AbstractCasToJsonConsumer.class);

	public static final String PARAM_OUTPUT_DIR = "OutputDir";
	public static final String PARAM_GZIP = "GZIP";
	private List<Document> documentBatch = new ArrayList<>();
	@ConfigurationParameter(name = PARAM_OUTPUT_DIR, mandatory = true, description = "The directory where the JSON files will be put to. If does not exist, it will be created, including all parent directories.")
	private File outputDir;
	@ConfigurationParameter(name = PARAM_GZIP, mandatory = false)
	private Boolean gzip;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		outputDir = new File((String) aContext.getConfigParameterValue(PARAM_OUTPUT_DIR));
		if (!outputDir.exists())
			outputDir.mkdirs();
		gzip = (Boolean) aContext.getConfigParameterValue(PARAM_GZIP);
		log.info("{}: {}", PARAM_OUTPUT_DIR, outputDir.getAbsolutePath());
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		Document singleDocument = convertCasToDocument(aJCas);
		if (singleDocument != null && !singleDocument.isEmpty())
			documentBatch.add(singleDocument);

		List<Document> documents = convertCasToDocuments(aJCas);
		if (documents != null) {
			for (Document document : documents)
				documentBatch.add(document);
		}
	}

	private void writeDocumentBatch() throws AnalysisEngineProcessException {
		log.info("Writing current batch of {} JSONized documents to output directory {}", documentBatch.size(),
				outputDir.getAbsolutePath());
		try {
			for (Document document : documentBatch) {
				String id = document.getId();
				String filepath = outputDir.getAbsolutePath() + File.separator + id + ".json";
				if (gzip)
					filepath += ".gz";
				try (OutputStream os = gzip ? new GZIPOutputStream(new FileOutputStream(filepath))
						: new FileOutputStream(filepath)) {
					String json = gson.toJson(document);
					IOUtils.write(json, os, "UTF-8");
				}
			}
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
		documentBatch.clear();
	}

	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		writeDocumentBatch();
		super.batchProcessComplete();
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		writeDocumentBatch();
		super.collectionProcessComplete();
	}

}
