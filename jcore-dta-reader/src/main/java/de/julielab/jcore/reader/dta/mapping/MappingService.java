/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
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
