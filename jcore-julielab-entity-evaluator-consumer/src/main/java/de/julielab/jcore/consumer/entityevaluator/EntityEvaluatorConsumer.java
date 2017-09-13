package de.julielab.jcore.consumer.entityevaluator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.utility.JCoReAnnotationIndexMerger;
import de.julielab.jcore.utility.index.Comparators;
import de.julielab.jcore.utility.index.JCoReTreeMapAnnotationIndex;
import de.julielab.jcore.utility.index.TermGenerators;

public class EntityEvaluatorConsumer extends JCasAnnotator_ImplBase {

	private enum OffsetMode {
		CHARACTER_SPAN, NON_WS_CHARACTERS
	}

	// If you add a new built-in column, don't forget to add its name to the
	// "predefinedColumnNames" set!
	public static final String DOCUMENT_ID_COLUMN = "DocumentId";
	public static final String SENTENCE_ID_COLUMN = "SentenceId";
	public static final String OFFSETS_COLUMN = "Offsets";

	public static final String PARAM_ADD_DOC_ID = "AddDocId";
	public static final String PARAM_ADD_SENT_ID = "AddSentenceId";
	public static final String PARAM_ADD_ENTITY_OFFSETS = "AddEntityOffsets";
	public final static String PARAM_DOC_INFORMATION_TYPE = "DocumentInformationType";
	public final static String PARAM_DOC_ID_FEATURE_PATH = "DocumentIdFeaturePath";
	// TODO this is now an array!
	public final static String PARAM_ENTITY_TYPES = "EntityTypes";
	public final static String PARAM_ENTITY_ID_FEATURE_PATHS = "EntityIdFeaturePaths";
	// TODO implement
	public static final String PARAM_FEATURE_FILTERS = "FeatureFilters";
	public final static String PARAM_DISCARD_ENTITIES_WO_ID = "DiscardEntitiesWithoutId";
	public final static String PARAM_OFFSET_MODE = "OffsetMode";
	@Deprecated
	public final static String PARAM_ADDITIONAL_FEATURE_PATHS = "AdditionalFeaturePaths";
	public final static String PARAM_OUTPUT_FILE = "OutputFile";

	public static final String PARAM_OUTPUT_COLUMNS = "OutputColumns";
	public static final String PARAM_COLUMN_DEFINITIONS = "ColumnDefinitions";
	public static final String PARAM_TYPE_PREFIX = "TypePrefix";
	public static final String PARAM_SENTENCE_TYPE_NAME = "SentenceTypeName";

	@Deprecated
	@ConfigurationParameter(name = PARAM_ADD_DOC_ID, mandatory = false, defaultValue = "true")
	private boolean addDocId;
	@Deprecated
	@ConfigurationParameter(name = PARAM_ADD_SENT_ID, mandatory = false, defaultValue = "false")
	private boolean addSentId;
	@Deprecated
	@ConfigurationParameter(name = PARAM_ADD_ENTITY_OFFSETS, mandatory = false, defaultValue = "true")
	private boolean addEntityOffsets;

	@Deprecated
	@ConfigurationParameter(name = PARAM_ENTITY_ID_FEATURE_PATHS, mandatory = false)
	private String[] idFeaturePaths;
	@Deprecated
	@ConfigurationParameter(name = PARAM_DOC_INFORMATION_TYPE, mandatory = false)
	private String docInfoTypeString;
	@Deprecated
	@ConfigurationParameter(name = PARAM_DOC_ID_FEATURE_PATH, mandatory = false)
	private String docIdFeaturePath;
	@Deprecated
	@ConfigurationParameter(name = PARAM_DISCARD_ENTITIES_WO_ID, mandatory = false)
	private Boolean discardWOId;
	@Deprecated
	@ConfigurationParameter(name = PARAM_ADDITIONAL_FEATURE_PATHS, mandatory = false)
	private String[] additionalFeaturePaths;

	@ConfigurationParameter(name = PARAM_OUTPUT_COLUMNS, mandatory = true)
	private String[] outputColumnNamesArray;
	@ConfigurationParameter(name = PARAM_COLUMN_DEFINITIONS, mandatory = true)
	private String[] columnDefinitionDescriptions;
	@ConfigurationParameter(name = PARAM_ENTITY_TYPES, mandatory = false)
	private String[] entityTypeStrings;
	@ConfigurationParameter(name = PARAM_OFFSET_MODE, mandatory = false)
	private OffsetMode offsetMode;
	@ConfigurationParameter(name = PARAM_TYPE_PREFIX, mandatory = true)
	private String typePrefix;
	@ConfigurationParameter(name = PARAM_FEATURE_FILTERS, mandatory = false)
	private String[] featureFilters;
	@ConfigurationParameter(name = PARAM_SENTENCE_TYPE_NAME, mandatory = false)
	private String asdf;
	@ConfigurationParameter(name = PARAM_OUTPUT_FILE, mandatory = true)
	private String outputFilePath;

	private Set<String> predefinedColumnNames = new HashSet<>();
	private LinkedHashSet<String> outputColumnNames;
	private LinkedHashMap<String, Column> columns;
	private LinkedHashSet<Object> entityTypes = new LinkedHashSet<>();

	private File outputFile;

	private List<String[]> entityRecords = new ArrayList<>();
	private BufferedWriter bw;

	private static final Logger log = LoggerFactory.getLogger(EntityEvaluatorConsumer.class);

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		outputColumnNamesArray = (String[]) aContext.getConfigParameterValue(PARAM_OUTPUT_COLUMNS);
		columnDefinitionDescriptions = (String[]) aContext.getConfigParameterValue(PARAM_COLUMN_DEFINITIONS);
		typePrefix = (String) aContext.getConfigParameterValue(PARAM_TYPE_PREFIX);

		featureFilters = (String[]) aContext.getConfigParameterValue(PARAM_FEATURE_FILTERS);
		outputFilePath = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_FILE);
		addDocId = (Boolean) aContext.getConfigParameterValue(PARAM_ADD_DOC_ID);
		addSentId = (Boolean) aContext.getConfigParameterValue(PARAM_ADD_SENT_ID);
		addEntityOffsets = (Boolean) aContext.getConfigParameterValue(PARAM_ADD_ENTITY_OFFSETS);
		entityTypeStrings = (String[]) aContext.getConfigParameterValue(PARAM_ENTITY_TYPES);
		idFeaturePaths = (String[]) aContext.getConfigParameterValue(PARAM_ENTITY_ID_FEATURE_PATHS);
		docInfoTypeString = (String) aContext.getConfigParameterValue(PARAM_DOC_INFORMATION_TYPE);
		docIdFeaturePath = (String) aContext.getConfigParameterValue(PARAM_DOC_ID_FEATURE_PATH);
		discardWOId = (Boolean) aContext.getConfigParameterValue(PARAM_DISCARD_ENTITIES_WO_ID);
		String offsetModeStr = (String) aContext.getConfigParameterValue(PARAM_OFFSET_MODE);
		additionalFeaturePaths = (String[]) aContext.getConfigParameterValue(PARAM_ADDITIONAL_FEATURE_PATHS);

		outputColumnNames = new LinkedHashSet<>(Stream.of(outputColumnNamesArray).collect(Collectors.toList()));

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

		predefinedColumnNames.add(DOCUMENT_ID_COLUMN);
		predefinedColumnNames.add(SENTENCE_ID_COLUMN);
		predefinedColumnNames.add(OFFSETS_COLUMN);

		log.info("{}: {}", PARAM_ENTITY_TYPES, entityTypeStrings);
		log.info("{}: {}", PARAM_ENTITY_ID_FEATURE_PATHS, idFeaturePaths);
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
			if (columns == null) {
				columns = new LinkedHashMap<>();
				for (int i = 0; i < columnDefinitionDescriptions.length; i++) {
					String definition = columnDefinitionDescriptions[i];
					Column c = new Column(definition, typePrefix, ts);
					if (c.getName().equals(DOCUMENT_ID_COLUMN)) {
						c = new DocumentIdColumn(c);
					}
					columns.put(c.getName(), c);
				}
				// collect all entity types from the column definitions and, one
				// step below, the explicitly listed
				entityTypes = new LinkedHashSet<>(
						columns.values().stream().filter(c -> !predefinedColumnNames.contains(c.getName()))
								.flatMap(c -> c.getTypes().stream()).collect(Collectors.toList()));

				if (entityTypeStrings != null)
					Stream.of(entityTypeStrings).map(name -> findType(name, typePrefix, ts)).forEach(entityTypes::add);

			}
			// the sentence column must be created new for each document because
			// it is using a document-specific sentence index
			addSentenceIdColumn(aJCas);
			// we just always add the offsets column, if it is used of not
			columns.put(OFFSETS_COLUMN, new OffsetsColumn());

			JCoReAnnotationIndexMerger indexMerger = new JCoReAnnotationIndexMerger(entityTypes, true, null, aJCas);
			while (indexMerger.incrementAnnotation()) {
				TOP a = indexMerger.getAnnotation();
				int colIndex = 0;
				String[] record = new String[outputColumnNames.size()];
				for (String outputColumnName : outputColumnNames) {
					assertColumnDefined(outputColumnName);
					Column c = columns.get(outputColumnName);
					record[colIndex++] = c.getValue(a);
				}
				entityRecords.add(record);
			}

			// Type documentInformationType = ts.getType(docInfoTypeString);
			// if (null == documentInformationType)
			// throw new AnalysisEngineProcessException(new
			// IllegalArgumentException("The type \"" + docInfoTypeString
			// + "\" was not found in the type system. Cannot proceed without
			// valid document IDs."));
			//
			// FSIterator<FeatureStructure> documentInformationTypeIterator =
			// aJCas.getIndexRepository()
			// .getAllIndexedFS(documentInformationType);
			// if (!documentInformationTypeIterator.isValid())
			// throw new AnalysisEngineProcessException(
			// new IllegalStateException("The index for the document information
			// type \"" + docInfoTypeString
			// + "\" is empty. Cannot proceed without valid document IDs."));
			// FeatureStructure documentInformationAnnotation =
			// documentInformationTypeIterator.get();
			//
			// FeaturePath docIdFp = aJCas.createFeaturePath();
			// docIdFp.initialize(docIdFeaturePath);
			// String docId =
			// docIdFp.getValueAsString(documentInformationAnnotation);
			//
			// for (int typeIndex = 0; typeIndex < entityTypeStrings.length;
			// typeIndex++) {
			// String entityTypeString = entityTypeStrings[typeIndex];
			//
			// Type entityType = ts.getType(entityTypeString);
			// if (null == entityType)
			// throw new AnalysisEngineProcessException(new
			// IllegalStateException(
			// "Type " + entityTypeStrings + " could not be found in the type
			// system."));
			//
			// List<JCoReFeaturePath> additionalFps = new ArrayList<>();
			//
			// for (int i = 0; additionalFeaturePaths != null && i <
			// additionalFeaturePaths.length; i++) {
			// String fp = additionalFeaturePaths[i];
			// JCoReFeaturePath jfp = new JCoReFeaturePath();
			// jfp.initialize(fp);
			// additionalFps.add(jfp);
			// }
			//
			// TreeMap<Integer, Integer> numWsMap = null;
			//
			// FSIterator<Annotation> entityIterator =
			// aJCas.getAnnotationIndex(entityType).iterator();
			// if (offsetMode == OffsetMode.NON_WS_CHARACTERS &&
			// entityIterator.hasNext())
			// numWsMap = createNumWsMap(aJCas.getDocumentText());
			// while (entityIterator.hasNext()) {
			// Annotation entity = entityIterator.next();
			// int beginOffset;
			// int endOffset;
			// switch (offsetMode) {
			// case CHARACTER_SPAN:
			// beginOffset = entity.getBegin();
			// endOffset = entity.getEnd();
			// break;
			// case NON_WS_CHARACTERS:
			// // for both offsets, subtract the number of preceding
			// // white
			// // spaces up to the respective offset
			// beginOffset = entity.getBegin() -
			// numWsMap.floorEntry(entity.getBegin()).getValue();
			// // we even have to subtract one more because we count
			// // actual
			// // characters while UIMA counts spans
			// endOffset = entity.getEnd() -
			// numWsMap.floorEntry(entity.getEnd()).getValue() - 1;
			// break;
			// default:
			// throw new IllegalArgumentException(
			// "Offset mode \"" + offsetMode + "\" is currently unsupported.");
			// }
			// String begin = String.valueOf(beginOffset);
			// String end = String.valueOf(endOffset);
			//
			// List<String[]> additionalFpValues = new ArrayList<>();
			// for (JCoReFeaturePath jfp : additionalFps) {
			// String[] fpValue = jfp.getValueAsStringArray(entity);
			// additionalFpValues.add(fpValue);
			// }
			// List<String> entityRecord = new ArrayList<>();
			// for (int j = 0; j < additionalFpValues.size(); ++j) {
			// String finalValue;
			// String[] values = additionalFpValues.get(j);
			// if (values == null)
			// values = new String[0];
			// // if there is no value, we leave the field empty
			// if (values.length == 0)
			// finalValue = "";
			// // if there is only a single value, we assume that this
			// // value should always be used
			// else if (values.length == 1)
			// finalValue = values[0];
			// // if the values are of the same length as the IDs, we
			// // assume both arrays to be parallel
			// else if (values.length == entityIds.length)
			// finalValue = values[i];
			// }
			//
			// if (null != discardWOId && discardWOId &&
			// StringUtils.isBlank(entityId)) {
			// log.debug("Discarding entity {} with because it has no value of
			// the ID feature path.", entity);
			// continue;
			// }
			//
			// entityRecords.add(entityRecord);
			// }
			// }
		} catch (CASException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void addSentenceIdColumn(JCas aJCas) {
		if (outputColumnNames.contains(SENTENCE_ID_COLUMN)) {
			assertColumnDefined(SENTENCE_ID_COLUMN);
			Column c = columns.get(SENTENCE_ID_COLUMN);
			Column docIdColumn = columns.get(DOCUMENT_ID_COLUMN);
			String documentId = null;
			if (docIdColumn != null)
				documentId = docIdColumn.getValue(aJCas.getDocumentAnnotationFs());
			Type sentenceType = c.getSingleType();
			// put all sentences into an index with an
			// overlap-comparator - this way the index can be
			// queried for entities and the sentence overlapping the
			// entity will be returned
			JCoReTreeMapAnnotationIndex<Long, ? extends Annotation> sentenceIndex = new JCoReTreeMapAnnotationIndex<>(
					Comparators.longOverlapComparator(), TermGenerators.longOffsetTermGenerator(),
					TermGenerators.longOffsetTermGenerator());
			sentenceIndex.index(aJCas, sentenceType);
			c = new SentenceIdColumn(documentId, c, sentenceIndex);
			columns.put(SENTENCE_ID_COLUMN, c);
		}
	}

	private void assertColumnDefined(String columnName) {
		Column c = columns.get(columnName);
		if (c == null)
			throw new IllegalArgumentException(
					"The column \"" + columnName + "\" was set for output but was not defined.");
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

	public static Type findType(String typeName, String typePrefix, TypeSystem ts) {
		String effectiveName = typeName.contains(".") ? typeName : typePrefix + "." + typeName;
		Type type = ts.getType(effectiveName);
		if (type == null)
			type = ts.getType(typePrefix + "." + effectiveName);
		if (type == null)
			throw new IllegalArgumentException(
					"The annotation type " + effectiveName + " was not found in the type system. The prefixed name \""
							+ typePrefix + "." + effectiveName + "\" has also been tried without success.");
		return type;
	}
}
