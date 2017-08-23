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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public abstract class AbstractMapper {
	final String mainClassification;
	final String subClassification;
	final Map<String, Class<? extends DocumentClassification>> classification2class;
	final Class<? extends DocumentClassification> defaultClass;

	AbstractMapper(
			final String mainClassification,
			final String subClassification,
			final Map<String, Class<? extends DocumentClassification>> classification2class) {
		this(mainClassification, subClassification, classification2class, null);
	}

	AbstractMapper(
			final String mainClassification,
			final String subClassification,
			final Map<String, Class<? extends DocumentClassification>> classification2class,
			final Class<? extends DocumentClassification> defaultClass) {
		this.mainClassification = MappingService.CLASIFICATION
				+ mainClassification;
		this.subClassification = MappingService.CLASIFICATION
				+ subClassification;
		this.classification2class = classification2class;
		this.defaultClass = defaultClass;
	}

	DocumentClassification getClassification(final JCas jcas,
			final String xmlFileName, final Map<String, String[]> classInfo)
			throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		if (classInfo.containsKey(mainClassification)) {
			if (classInfo.get(mainClassification).length != 1)
				throw new IllegalArgumentException("More than 1 "
						+ mainClassification + " classification in "
						+ xmlFileName);
			final String mainClass = classInfo.get(mainClassification)[0];
			Class<? extends DocumentClassification> aClass = classification2class
					.get(mainClass);
			if (aClass == null)
				if (defaultClass == null)
					throw new IllegalArgumentException(mainClass
							+ " not supported in " + xmlFileName);
				else
					aClass = defaultClass;
			final Constructor<? extends DocumentClassification> constructor = aClass
					.getConstructor(new Class[] { JCas.class });
			final DocumentClassification classification = constructor
					.newInstance(jcas);
			classification.setClassification(mainClass);

			if (classInfo.get(subClassification) != null) {
				if (classInfo.get(subClassification).length != 1)
					throw new IllegalArgumentException("More than 1 "
							+ subClassification + " classification in "
							+ xmlFileName);
				final String subClass = classInfo.get(subClassification)[0];
				classification.setSubClassification(subClass);
			}

			classification.addToIndexes();
			return classification;
		}
		return null;
	}
}
