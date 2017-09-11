package de.julielab.jcore.consumer.entityevaluator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.utility.JCoReFeaturePath;

public class EntityEvaluatorConsumer extends JCasAnnotator_ImplBase {

	private enum OffsetMode {
		CHARACTER_SPAN, NON_WS_CHARACTERS
	}

	public final static String PARAM_ENTITY_TYPE = "EntityType";
	public final static String PARAM_ID_FEATURE_PATH = "IdFeaturePath";
	public final static String PARAM_DOC_INFORMATION_TYPE = "DocumentInformationType";
	public final static String PARAM_DOC_ID_FEATURE_PATH = "DocumentIdFeaturePath";
	public final static String PARAM_OUTPUT_FILE = "OutputFile";
	public final static String PARAM_DISCARD_ENTITIES_WO_ID = "DiscardEntitiesWithoutId";
	public final static String PARAM_OFFSET_MODE = "OffsetMode";
	public final static String PARAM_ADDITIONAL_FEATURE_PATHS = "AdditionalFeaturePaths";

	@ConfigurationParameter(name = PARAM_ENTITY_TYPE, mandatory = true)
	private String entityTypeString;
	@ConfigurationParameter(name = PARAM_ID_FEATURE_PATH, mandatory = true)
	private String idFeaturePath;
	@ConfigurationParameter(name = PARAM_DOC_INFORMATION_TYPE, mandatory = true)
	private String docInfoTypeString;
	@ConfigurationParameter(name = PARAM_DOC_ID_FEATURE_PATH, mandatory = true)
	private String docIdFeaturePath;
	@ConfigurationParameter(name = PARAM_OUTPUT_FILE, mandatory = true)
	private String outputFilePath;
	@ConfigurationParameter(name = PARAM_DISCARD_ENTITIES_WO_ID, mandatory = false)
	private Boolean discardWOId;
	@ConfigurationParameter(name = PARAM_OFFSET_MODE, mandatory = false)
	private OffsetMode offsetMode;
	@ConfigurationParameter(name = PARAM_ADDITIONAL_FEATURE_PATHS, mandatory = false)
	private String[] additionalFeaturePaths;

	private File outputFile;

	private List<String[]> entityRecords = new ArrayList<>();
	private BufferedWriter bw;

	private static final Logger log = LoggerFactory.getLogger(EntityEvaluatorConsumer.class);

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		entityTypeString = (String) aContext.getConfigParameterValue(PARAM_ENTITY_TYPE);
		idFeaturePath = (String) aContext.getConfigParameterValue(PARAM_ID_FEATURE_PATH);
		docInfoTypeString = (String) aContext.getConfigParameterValue(PARAM_DOC_INFORMATION_TYPE);
		docIdFeaturePath = (String) aContext.getConfigParameterValue(PARAM_DOC_ID_FEATURE_PATH);
		discardWOId = (Boolean) aContext.getConfigParameterValue(PARAM_DISCARD_ENTITIES_WO_ID);
		outputFilePath = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_FILE);
		String offsetModeStr = (String) aContext.getConfigParameterValue(PARAM_OFFSET_MODE);
		additionalFeaturePaths = (String[]) aContext.getConfigParameterValue(PARAM_ADDITIONAL_FEATURE_PATHS);

		offsetMode = null == offsetModeStr ? OffsetMode.CHARACTER_SPAN
				: OffsetMode.valueOf(offsetModeStr.toUpperCase());

		outputFile = new File(outputFilePath);
		if (outputFile.exists()) {
			log.warn("File \"{}\" is overridden.", outputFile.getAbsolutePath());
			outputFile.delete();
		}
		try {
			bw = FileUtilities.getWriterToFile(outputFile);
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

		log.info("{}: {}", PARAM_ENTITY_TYPE, entityTypeString);
		log.info("{}: {}", PARAM_ID_FEATURE_PATH, idFeaturePath);
		log.info("{}: {}", PARAM_DOC_INFORMATION_TYPE, docInfoTypeString);
		log.info("{}: {}", PARAM_DOC_ID_FEATURE_PATH, docIdFeaturePath);
		log.info("{}: {}", PARAM_ADDITIONAL_FEATURE_PATHS, additionalFeaturePaths);
		log.info("{}: {}", PARAM_DISCARD_ENTITIES_WO_ID, discardWOId);
		log.info("{}: {}", PARAM_OUTPUT_FILE, outputFilePath);
		log.info("{}: {}", PARAM_OFFSET_MODE, offsetMode);
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		try {
			TypeSystem ts = aJCas.getTypeSystem();
			Type documentInformationType = ts.getType(docInfoTypeString);
			if (null == documentInformationType)
				throw new AnalysisEngineProcessException(new IllegalArgumentException("The type \"" + docInfoTypeString
						+ "\" was not found in the type system. Cannot proceed without valid document IDs."));

			FSIterator<FeatureStructure> documentInformationTypeIterator = aJCas.getIndexRepository()
					.getAllIndexedFS(documentInformationType);
			if (!documentInformationTypeIterator.isValid())
				throw new AnalysisEngineProcessException(
						new IllegalStateException("The index for the document information type \"" + docInfoTypeString
								+ "\" is empty. Cannot proceed without valid document IDs."));
			FeatureStructure documentInformationAnnotation = documentInformationTypeIterator.get();

			FeaturePath docIdFp = aJCas.createFeaturePath();
			docIdFp.initialize(docIdFeaturePath);
			String docId = docIdFp.getValueAsString(documentInformationAnnotation);

			Type entityType = ts.getType(entityTypeString);
			if (null == entityType)
				throw new AnalysisEngineProcessException(new IllegalStateException(
						"Type " + entityTypeString + " could not be found in the type system."));

			JCoReFeaturePath idfp = new JCoReFeaturePath();
			List<JCoReFeaturePath> additionalFps = new ArrayList<>();

			for (int i = 0; additionalFeaturePaths != null && i < additionalFeaturePaths.length; i++) {
				String fp = additionalFeaturePaths[i];
				JCoReFeaturePath jfp = new JCoReFeaturePath();
				jfp.initialize(fp);
				additionalFps.add(jfp);
			}

			TreeMap<Integer, Integer> numWsMap = null;

			idfp.initialize(idFeaturePath);
			FSIterator<Annotation> entityIterator = aJCas.getAnnotationIndex(entityType).iterator();
			if (offsetMode == OffsetMode.NON_WS_CHARACTERS && entityIterator.hasNext())
				numWsMap = createNumWsMap(aJCas.getDocumentText());
			while (entityIterator.hasNext()) {
				Annotation entity = entityIterator.next();
				String[] entityIds = idfp.getValueAsStringArray(entity);
				int beginOffset;
				int endOffset;
				switch (offsetMode) {
				case CHARACTER_SPAN:
					beginOffset = entity.getBegin();
					endOffset = entity.getEnd();
					break;
				case NON_WS_CHARACTERS:
					// for both offsets, subtract the number of preceding white
					// spaces up to the respective offset
					beginOffset = entity.getBegin() - numWsMap.floorEntry(entity.getBegin()).getValue();
					// we even have to subtract one more because we count actual
					// characters while UIMA counts spans
					endOffset = entity.getEnd() - numWsMap.floorEntry(entity.getEnd()).getValue() - 1;
					break;
				default:
					throw new IllegalArgumentException("Offset mode \"" + offsetMode + "\" is currently unsupported.");
				}
				String begin = String.valueOf(beginOffset);
				String end = String.valueOf(endOffset);

				List<String[]> additionalFpValues = new ArrayList<>();
				for (JCoReFeaturePath jfp : additionalFps) {
					String[] fpValue = jfp.getValueAsStringArray(entity);
					additionalFpValues.add(fpValue);
				}
				if (entityIds == null)
					entityIds = new String[] { null };
				for (int i = 0; i < entityIds.length; i++) {
					// the 5 fields docId, entityId, begin, end and coveredText
					// are fixed, additional fields may be appended
					String[] entityRecord = new String[5 + additionalFps.size()];
					String entityId = entityIds[i];

					entityRecord[0] = docId;
					entityRecord[1] = entityId;
					entityRecord[2] = begin;
					entityRecord[3] = end;
					entityRecord[4] = removeLineBreak(entity.getCoveredText());
					for (int j = 0; j < additionalFpValues.size(); ++j) {
						String finalValue;
						String[] values = additionalFpValues.get(j);
						if (values == null)
							values = new String[0];
						// if there is no value, we leave the field empty
						if (values.length == 0)
							finalValue = "";
						// if there is only a single value, we assume that this
						// value should always be used
						else if (values.length == 1)
							finalValue = values[0];
						// if the values are of the same length as the IDs, we
						// assume both arrays to be parallel
						else if (values.length == entityIds.length)
							finalValue = values[i];
						else
							throw new IllegalArgumentException(
									"Feature path " + additionalFeaturePaths[j] + " has " + values.length
											+ " values but there are " + entityIds.length + " entity IDs. Values: "
											+ Arrays.toString(values) + ", IDs: " + Arrays.toString(entityIds));
						entityRecord[j + 5] = finalValue;
					}

					if (null != discardWOId && discardWOId && StringUtils.isBlank(entityId)) {
						log.debug("Discarding entity {} with because it has no value of the ID feature path.", entity);
						continue;
					}

					entityRecords.add(entityRecord);
				}
			}
		} catch (CASException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a map where for each white space position, the number of
	 * preceding non-whitespace characters from the beginning of <tt>input</tt>
	 * is returned.<br/>
	 * Thus, for each character-based offset <tt>o</tt>, the non-whitespace
	 * offset may be retrieved using the floor entry for <tt>o</tt>, retrieving
	 * its value and subtracting it from <tt>o</tt>.
	 * 
	 * @param input
	 * @return
	 */
	private static TreeMap<Integer, Integer> createNumWsMap(String input) {
		TreeMap<Integer, Integer> map = new TreeMap<>();
		map.put(0, 0);
		int numWs = 0;
		boolean lastCharWasWs = false;
		for (int i = 0; i < input.length(); ++i) {
			if (lastCharWasWs)
				map.put(i, numWs);
			char c = input.charAt(i);
			if (Character.isWhitespace(c)) {
				++numWs;
				lastCharWasWs = true;
			} else {
				lastCharWasWs = false;
			}
		}
		return map;
	}

	/**
	 * Primitive removal of line breaks within entity text by replacing newlines
	 * by white spaces. May go wrong if the line break is after a dash, for
	 * example.
	 * 
	 * @param text
	 * @return
	 */
	private String removeLineBreak(String text) {
		String ret = text.replaceAll("\n", " ");
		return ret;
	}

	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		super.batchProcessComplete();
		log.debug("Batch completed. Writing {} entity records to file {}.", entityRecords.size(), outputFile.getName());
		appendEntityRecordsToFile();
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		log.info("Collection completed. Writing {} entity records to file {}.", entityRecords.size(),
				outputFile.getName());
		appendEntityRecordsToFile();
		try {
			bw.close();
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	protected void appendEntityRecordsToFile() {
		for (String[] entityRecord : entityRecords) {
			try {
				bw.write(Stream.of(entityRecord).collect(Collectors.joining("\t")) + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		entityRecords.clear();
	}

}
