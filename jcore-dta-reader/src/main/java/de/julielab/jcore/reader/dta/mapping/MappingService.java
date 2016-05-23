package de.julielab.jcore.reader.dta.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public abstract class MappingService {
	static final String CLASIFICATION = "http://www.deutschestextarchiv.de/doku/klassifikation#";
	private static final String CLASIFICATION_DTA_CORPUS = CLASIFICATION + "DTACorpus";
	private static final AbstractMapper[] mappers = new AbstractMapper[] { new DTAMapper(), new DWDS1Mapper(),
			new DWDS2Mapper() };

	public static boolean isCoreCorpus(final Map<String, String[]> classInfo) {
		return classInfo.containsKey(CLASIFICATION_DTA_CORPUS)
				&& Arrays.asList(classInfo.get(CLASIFICATION_DTA_CORPUS)).contains("core");
	}

	public static FSArray getClassifications(final JCas jcas, final String xmlFileName,
			final Map<String, String[]> classInfo) throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ArrayList<DocumentClassification> classificationsList = new ArrayList<>();
		for (AbstractMapper mapper : mappers) {
			DocumentClassification classification = mapper.getClassification(jcas, xmlFileName, classInfo);
			if (classification != null)
				classificationsList.add(classification);
		}
		if (classificationsList.isEmpty())
			return null;
		FeatureStructure[] classificationsArray = classificationsList
				.toArray(new FeatureStructure[classificationsList.size()]);
		final FSArray classificationsFSArray = new FSArray(jcas, classificationsArray.length);
		classificationsFSArray.copyFromArray(classificationsArray, 0, 0, classificationsArray.length);
		classificationsFSArray.addToIndexes();
		return classificationsFSArray;
	}

}
