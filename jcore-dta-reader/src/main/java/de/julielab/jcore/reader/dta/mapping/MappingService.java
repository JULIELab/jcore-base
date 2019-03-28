/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.dta.mapping;

import de.julielab.jcore.types.extensions.dta.DocumentClassification;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public abstract class MappingService {
	static final String CLASIFICATION = "http://www.deutschestextarchiv.de/doku/klassifikation#";
	private static final String CLASIFICATION_DTA_CORPUS = CLASIFICATION
			+ "DTACorpus";
	private static final AbstractMapper[] mappers = new AbstractMapper[] {
			new DTAMapper(), new DWDS1Mapper(), new DWDS2Mapper() };

	public static FSArray getClassifications(final JCas jcas,
			final String xmlFileName, final Map<String, String[]> classInfo)
			throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		final ArrayList<DocumentClassification> classificationsList = new ArrayList<>();
		for (final AbstractMapper mapper : mappers) {
			final DocumentClassification classification = mapper
					.getClassification(jcas, xmlFileName, classInfo);
			if (classification != null)
				classificationsList.add(classification);
		}
		if (classificationsList.isEmpty())
			return null;
		final FeatureStructure[] classificationsArray = classificationsList
				.toArray(new FeatureStructure[classificationsList.size()]);
		final FSArray classificationsFSArray = new FSArray(jcas,
				classificationsArray.length);
		classificationsFSArray.copyFromArray(classificationsArray, 0, 0,
				classificationsArray.length);
		classificationsFSArray.addToIndexes();
		return classificationsFSArray;
	}

	public static boolean isCoreCorpus(final Map<String, String[]> classInfo) {
		return classInfo.containsKey(CLASIFICATION_DTA_CORPUS)
				&& Arrays.asList(classInfo.get(CLASIFICATION_DTA_CORPUS))
						.contains("core");
	}

}
