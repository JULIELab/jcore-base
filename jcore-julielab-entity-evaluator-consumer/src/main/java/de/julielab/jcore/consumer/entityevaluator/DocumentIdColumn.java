/**
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author:
 * <p>
 * Description:
 **/
package de.julielab.jcore.consumer.entityevaluator;

import de.julielab.jcore.utility.JCoReFeaturePath;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayDeque;
import java.util.Deque;

public class DocumentIdColumn extends Column {

    public DocumentIdColumn(Column c) {
        super(c);
    }

    @Override
    public Deque<String> getValue(TOP a, JCas aJCas) {
        String value;
        Type documentMetaInformationType = getSingleType();
        FSIterator<Annotation> it = aJCas.getAnnotationIndex(documentMetaInformationType).iterator();
        if (!it.hasNext())
            throw new IllegalArgumentException("The given document meta information type "
                    + documentMetaInformationType.getName() + " was not found in the current CAS.");
        Annotation docInfoAnnotation = it.next();
        JCoReFeaturePath fp = featurePathMap.get(documentMetaInformationType);
        value = fp.getValueAsString(docInfoAnnotation);
        Deque<String> ret = new ArrayDeque<>();
        ret.add(value);
        return ret;
    }

}
