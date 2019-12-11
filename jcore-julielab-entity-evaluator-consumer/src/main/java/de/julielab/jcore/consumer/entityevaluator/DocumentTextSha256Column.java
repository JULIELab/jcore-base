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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;

import java.util.ArrayDeque;
import java.util.Deque;


public class DocumentTextSha256Column extends Column {

    public DocumentTextSha256Column() {
        super();
    }

    @Override
    public Deque<String> getValue(TOP a, JCas aJCas) {
        final String documentText = aJCas.getDocumentText();
        final byte[] sha = DigestUtils.sha256(documentText.getBytes());
        final String finalHash = Base64.encodeBase64String(sha);
        Deque<String> ret = new ArrayDeque<>();
        ret.add(finalHash);
        return ret;
    }

}
