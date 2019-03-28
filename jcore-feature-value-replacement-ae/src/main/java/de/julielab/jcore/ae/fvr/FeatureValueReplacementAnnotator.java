package de.julielab.jcore.ae.fvr;

import de.julielab.jcore.utility.JCoReFeaturePath;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FeatureValueReplacementAnnotator extends JCasAnnotator_ImplBase {

	private static final Logger log = LoggerFactory.getLogger(FeatureValueReplacementAnnotator.class);

	public static final String RESOURCE_REPLACEMENTS = "Replacements";
	public static final String PARAM_FEATURE_PATHS = "FeaturePaths";
	@ConfigurationParameter(name = PARAM_FEATURE_PATHS, mandatory = true, description = "An array of type-featurePath pairs of the form <qualified type>=<feature path>[?defaultValue]. Each value pointed to by the feature path of the annotations of the respective type will be replaced according to the replacement map given as an external resource with key \""
			+ RESOURCE_REPLACEMENTS
			+ "\". If a defaultValue is specified, feature values not contained in the map will be mapped to this defaultValue. The string \"null\" means the null-reference.")
	private JCoReFeaturePath[] featurePaths;
	private Type[] types;
	private boolean initialized;
	private String[] typeNames;

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			IFeatureValueReplacementsProvider replacementsProvider = (IFeatureValueReplacementsProvider) aContext
					.getResourceObject(RESOURCE_REPLACEMENTS);
			Map<String, String> replacements = replacementsProvider.getFeatureValueReplacements();
			String[] featurePathStrings = (String[]) aContext.getConfigParameterValue(PARAM_FEATURE_PATHS);
			featurePaths = new JCoReFeaturePath[featurePathStrings.length];
			typeNames = new String[featurePathStrings.length];
			for (int i = 0; i < featurePathStrings.length; i++) {
				String typeAndFpString = featurePathStrings[i];
				String[] split = typeAndFpString.split("=");
				if (split.length != 2)
					throw new ResourceAccessException(new IllegalArgumentException("Format error: The parameter \""
							+ PARAM_FEATURE_PATHS
							+ "\" expects entries of the form <qualified type>=<feature path>[?defaultValue]. Thus, the given value \""
							+ typeAndFpString + "\" is not valid."));
				String typeName = split[0];
				String fpString = split[1];
				String defaultValue = null;
				if (fpString.contains("?")) {
					String[] fpSplit = fpString.split("\\?");
					fpString = fpSplit[0];
					defaultValue = fpSplit[1];
				}
				typeNames[i] = typeName.trim();
				JCoReFeaturePath fp = new JCoReFeaturePath();
				fp.initialize(fpString.trim(), replacements);
				fp.setReplaceUnmappedValues(null != defaultValue);
				fp.setDefaultReplacementValue(
						null != defaultValue && defaultValue.equals("null") ? null : defaultValue);
				featurePaths[i] = fp;
				log.debug(
						"Initializing feature path {} on type {} with default replacement value {}. The default replacement value is the null reference: {}. Default value replacement is switched on: {}",
						fpString, typeName, fp.getDefaultReplacementValue(), fp.getDefaultReplacementValue() == null,
						fp.getReplaceUnmappedValues());
			}
		} catch (ResourceAccessException | CASException e) {
			throw new ResourceInitializationException(e);
		}
		initialized = false;
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		// We need the TypeSystem to do the initialization of types and for this
		// we need a CAS...
		if (!initialized) {
			TypeSystem ts = aJCas.getTypeSystem();
			types = new Type[typeNames.length];
			for (int i = 0; i < typeNames.length; i++) {
				String typeName = typeNames[i];
				Type type = ts.getType(typeName);
				if (type == null)
					throw new IllegalArgumentException(
							"The annotation type \"" + typeName + "\" was not found in the type system.");
				types[i] = type;
			}
			initialized = true;
		}

		for (int i = 0; i < types.length; i++) {
			Type type = types[i];
			JCoReFeaturePath fp = featurePaths[i];
			FSIterator<Annotation> iterator = aJCas.getAnnotationIndex(type).iterator();
			while (iterator.hasNext()) {
				Annotation annotation = iterator.next();
				fp.replaceValue(annotation);
			}
		}

		// Clear the already-replaced-cache after each CAS or it can (and will!)
		// come to collisions, resulting in
		// not-replaced feature values.
		for (int i = 0; i < featurePaths.length; i++) {
			JCoReFeaturePath fp = featurePaths[i];
			fp.clearReplacementCache();
		}
	}

}
