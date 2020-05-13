
package de.julielab.jcore.consumer.ppd;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReAnnotationTools;
import de.julielab.jcore.utility.JCoReFeaturePath;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ResourceMetaData(name = "JCoRe PPD Writer", description = "This component writes CAS annotation data to the pipe-separated format. For example, writing tokens with their PoS would result in text like 'The|DET tree|NN is|VBZ green|ADJ'. The component can be configured for an arbitrary number of annotations to be added to each token.")
@TypeCapability(inputs = { "de.julielab.jcore.types.Token", "de.julielab.jcore.types.Sentence" })
public class PPDWriter extends JCasAnnotator_ImplBase {

	private static final Logger log = LoggerFactory.getLogger(PPDWriter.class);

	public static final String PARAM_TYPE_LABEL_MAPPINGS = "TypeToLabelMappings";
	public static final String PARAM_META_DATA_TYPE_MAPPINGS = "MetaDataTypesMapping";
	public static final String PARAM_OUTSIDE_LABEL = "OutsideLabel";
	public static final String PARAM_OUTPUT_FILE = "OutputFile";

	@ConfigurationParameter(name = PARAM_TYPE_LABEL_MAPPINGS, mandatory = true, description = "A parameter to define one or multiple mappings from a UIMA type to token labels/classes. A token that is completely overlapped by one of the UIMA types defined in the mapping will be given the mapped label in the PPD output. The format is [qualified type]=[label string / feature path]. I.e. you may map a type to a simple label string or you can read the actual label value from within the type. Examples: \"de.julielab.jcore.types.Gene=GENE\". This would give all tokens that are complete covered by a Gene annotation the label \"GENE\" in the PPD output. The mapping \"de.julielab.jcore.types.Gene=/specificType\" would use the value of the \"specificType\" feature of a Gene annotation as the label for the covered tokens in the PPD output.")
	private String[] typeToLabelMappings;
	@ConfigurationParameter(name = PARAM_META_DATA_TYPE_MAPPINGS, mandatory = false, description = "A parameter to define one or multiple mappings from a UIMA type to token meta data in the PPD output. The minimal form of the PPD output is \"token|label\", e.g. \"il-2|Gene\". Additionally, you may deliver as much information as desired, e.g. the part of speech tag: \"il-2|NN|Gene\". This is done by defining meta data mappings with this parameter. The mapping has the form \"[qualified type]=[feature path]\", for example \"de.julielab.jcore.types.PennBioIEPOSTag=/value\". This will use the feature \"value\" to fill in the respective meta data slot in the PPD output. The order in which multiple meta data information is written into the PPD is the order you specify in this mapping array.")
	private String[] metaDataTypeMappings;
	@ConfigurationParameter(name = PARAM_OUTSIDE_LABEL, mandatory = true, defaultValue = "O", description = "The label for all tokens that do not belong to a class of interest. All tokens not covered by at least one UIMA type defined in the TypeToLabelMappings parameter will get this outside label in the PPD output. The default value is \"O\".")
	private String outsideLabel;
	@ConfigurationParameter(name = PARAM_OUTPUT_FILE, mandatory = true, description = "The path where the output PPD file should be written to.")
	private String outputFileString;

	private Map<Class<? extends Annotation>, String> typeToLabelMap;
	private Map<Class<? extends Annotation>, JCoReFeaturePath> typeToFeaturePathMap;
	private LinkedHashMap<Class<? extends Annotation>, JCoReFeaturePath> metaDataFeaturePathMap;

	private List<String> ppdSentences;

	private File outputFile;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);

		// For non-mandatory parameters to easily check null values.
		Object configValue;
		typeToLabelMappings = (String[]) aContext.getConfigParameterValue(PARAM_TYPE_LABEL_MAPPINGS);
		configValue = aContext.getConfigParameterValue(PARAM_META_DATA_TYPE_MAPPINGS);
		if (null != configValue)
			metaDataTypeMappings = (String[]) configValue;
		outsideLabel = (String) aContext.getConfigParameterValue(PARAM_OUTSIDE_LABEL);
		outputFileString = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_FILE);

		try {
			initializeLabelMaps(typeToLabelMappings);
			initializeMetaDataMap(metaDataTypeMappings);
		} catch (CASException e) {
			throw new ResourceInitializationException(e);
		}
		ppdSentences = new ArrayList<>();
		outputFile = new File(outputFileString);
		if (outputFile.exists()) {
			log.warn("PPD output file {} exists and will be overwritten.", outputFile.getAbsolutePath());
			outputFile.delete();
		}
	}

	@SuppressWarnings("unchecked")
	private void initializeMetaDataMap(String[] metaDataTypeMappings) throws CASException {
		metaDataFeaturePathMap = new LinkedHashMap<>();

		for (String metaDataFeaturePathMapping : metaDataTypeMappings) {
			String[] split = metaDataFeaturePathMapping.split("=");

			if (split.length != 2)
				throw new IllegalArgumentException(
						"The meta data mapping \""
								+ metaDataFeaturePathMapping
								+ "\" is not consistent with the expected format \"[qualified type]=[feature path]\" because it does not contain a single equal sign.");

			String typeString = split[0].trim();
			String featurePathString = split[1].trim();

			Class<? extends Annotation> type;
			try {
				type = (Class<? extends Annotation>) Class.forName(typeString);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("The type \"" + typeString
						+ "\" that was given in the meta data type mapping does not exist.");
			}

			if (!featurePathString.startsWith("/"))
				throw new IllegalArgumentException("The value of the meta data feature path mapping \""
						+ metaDataFeaturePathMapping
						+ "\" is not accepted as a feature path because it does not start with a slash.");

			JCoReFeaturePath featurePath = new JCoReFeaturePath();
			featurePath.initialize(featurePathString);
			metaDataFeaturePathMap.put(type, featurePath);
		}
	}

	/**
	 * Parses type to label mappings. The format is
	 * <p>
	 *
	 * <pre>
	 * [qualified UIMA type]=[label string / feature path]
	 * </pre>
	 *
	 * Examples:
	 *
	 * <pre>
	 * de.julielab.jcore.Gene=GENE
	 * de.julielab.jcore.Gene=/specificType
	 * de.julielab.jcore.Gene=/resourceEntryList[0]/entryId
	 * </pre>
	 *
	 * </p>
	 * <p>
	 * Simple label strings are differentiated from feature paths by the leading slash of feature paths.
	 * </p>
	 *
	 * @param typeToLabelMappings
	 * @throws ClassNotFoundException
	 * @throws CASException
	 */
	@SuppressWarnings("unchecked")
	private void initializeLabelMaps(String[] typeToLabelMappings) throws CASException {
		typeToLabelMap = new HashMap<>();
		typeToFeaturePathMap = new HashMap<>();

		for (String typeLabelMapping : typeToLabelMappings) {
			String[] split = typeLabelMapping.split("=");

			if (split.length != 2)
				throw new IllegalArgumentException(
						"The type to label mapping \""
								+ typeLabelMapping
								+ "\" is not consistent with the expected format \"[qualified type]=[label string / feature path]\" because it does not contain a single equal sign.");

			String typeString = split[0].trim();
			String value = split[1].trim();

			Class<? extends Annotation> type;
			try {
				type = (Class<? extends Annotation>) Class.forName(typeString);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("The type \"" + typeString
						+ "\" that was given in the type to label mapping does not exist.");
			}

			if (value.startsWith("/")) {
				JCoReFeaturePath featurePath = new JCoReFeaturePath();
				featurePath.initialize(value);
				typeToFeaturePathMap.put(type, featurePath);
			} else {
				typeToLabelMap.put(type, value);
			}
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> sentenceIt = aJCas.getAnnotationIndex(Sentence.type).iterator();

		while (sentenceIt.hasNext()) {
			StringBuilder sb = new StringBuilder();

			Sentence sentence = (Sentence) sentenceIt.next();
			Iterator<Token> tokens = JCoReAnnotationTools.getIncludedAnnotations(aJCas, sentence, Token.class).iterator();

			while (tokens.hasNext()) {
				Token token = (Token) tokens.next();
				sb.append(token.getCoveredText());
				sb.append("|");
				for (Map.Entry<Class<? extends Annotation>, JCoReFeaturePath> entry : metaDataFeaturePathMap.entrySet()) {
					Class<? extends Annotation> metaDataType = entry.getKey();
					JCoReFeaturePath featurePath = entry.getValue();

					Annotation metaDataAnnotation = JCoReAnnotationTools.getAnnotationAtMatchingOffsets(aJCas, token,
							metaDataType);
					String metaData = "<N/A>";
					if (null != metaDataAnnotation) {
						metaData = featurePath.getValueAsString(metaDataAnnotation);
					} else {
						log.warn("MetaData annotation for type \"{}\" for token {} does not exist.",
								metaDataType.getCanonicalName(), token);
					}
					sb.append(metaData);
					sb.append("|");
				}
				// We expect at most one label annotation type per token but we just get all and throw an error if there
				// are more.
				List<Class<? extends Annotation>> labelAnnotations = new ArrayList<>(1);
				// Handling label types for which the label string was given explicitly.
				for (Class<? extends Annotation> labelType : typeToLabelMap.keySet()) {
					Annotation includingAnnotation = JCoReAnnotationTools.getIncludingAnnotation(aJCas, token, labelType);
					if (null != includingAnnotation) {
						labelAnnotations.add(labelType);

						// There should be only a single label type. If not, there will be thrown an exception anyway so
						// just assume everything is alright and add the label.
						String label = typeToLabelMap.get(labelType);
						sb.append(label);
						// No further pipes, the label is the end of it.
					}
				}
				// Handling label types for which we have to retrieve the actual label string from a feature value.
				for (Class<? extends Annotation> labelType : typeToFeaturePathMap.keySet()) {
					Annotation includingAnnotation = JCoReAnnotationTools.getIncludingAnnotation(aJCas, token, labelType);
					if (null != includingAnnotation) {
						labelAnnotations.add(labelType);

						// There should be only a single label type. If not, there will be thrown an exception anyway so
						// just assume everything is alright and add the label.
						JCoReFeaturePath featurePath = typeToFeaturePathMap.get(labelType);
						String label = featurePath.getValueAsString(includingAnnotation);
						sb.append(label);
						// No further pipes, the label is the end of it.
					}
				}
				if (labelAnnotations.size() > 1)
					throw new IllegalStateException("Multiple label types for token " + token
							+ " have been found. However, it can be at most one. Found label types are:"
							+ StringUtils.join(labelAnnotations, ", "));
				else if (labelAnnotations.size() == 0)
					sb.append(outsideLabel);

				sb.append(" ");
			}
			// Delete the whitespace we added after the last token.
			sb.deleteCharAt(sb.length() - 1);
			ppdSentences.add(sb.toString());
		}

	}

	@Override
	public void batchProcessComplete() throws AnalysisEngineProcessException {
		super.batchProcessComplete();
		try {
			log.debug("Batch process complete, writing {} PPD sentences to {}.", ppdSentences.size(),
					outputFile.getAbsolutePath());
			writePPDToFile(outputFile);
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		try {
			log.debug("Collection process complete, writing {} PPD sentences to {}.", ppdSentences.size(),
					outputFile.getAbsolutePath());
			writePPDToFile(outputFile);
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	private void writePPDToFile(File outputFile) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true), StandardCharsets.UTF_8))) {
			for (String sentence : ppdSentences) {
				bw.write(sentence);
				bw.newLine();
			}
		}
		ppdSentences.clear();
	}

}
