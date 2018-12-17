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
package de.julielab.jcore.consumer.entityevaluator;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.utility.JCoReFeaturePath;

public class DocumentIdColumn extends Column {

	public DocumentIdColumn(Column c) {
		super(c);
	}

	@Override
	public String getValue(TOP a) {
		String value;
		try {
			JCas jCas = a.getCAS().getJCas();
			Type documentMetaInformationType = getSingleType();
			FSIterator<Annotation> it = jCas.getAnnotationIndex(documentMetaInformationType).iterator();
			if (!it.hasNext())
				throw new IllegalArgumentException("The given document meta information type "
						+ documentMetaInformationType.getName() + " was not found in the current CAS.");
			Annotation docInfoAnnotation = it.next();
			JCoReFeaturePath fp = featurePathMap.get(documentMetaInformationType);
			value = fp.getValueAsString(docInfoAnnotation);
			return value;
		} catch (CASException e) {
			throw new RuntimeException(e);
		}
	}

}
