/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.reader.bionlpformat.utils;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.Header;

public class TextFileMapper {

    public void mapAbstractFile(String id, String source, BufferedReader bufferedReader, JCas cas) throws IOException {
        String title = bufferedReader.readLine();
        String text = "";

        StringBuilder sb = new StringBuilder();
        Boolean addedText = false;
        while (text != null) {
            text = bufferedReader.readLine();
            if (text != null) {
                sb.append(text + " ");
                if (!addedText) {
                    addedText = true;
                }
            }
        }

        text = !addedText ? sb.toString() : sb.toString().substring(0, sb.length() - 1);

        title = title == null ? "" : title;
        text = text == null ? "" : text;
        cas.setDocumentText(title + "\n" + text);

        Title titleAnnotation = new Title(cas);
        titleAnnotation.setBegin(0);
        titleAnnotation.setEnd(title.length());
        titleAnnotation.addToIndexes();

        AbstractText abstractTextAnnotation = new AbstractText(cas);
        abstractTextAnnotation.setBegin(title.length() + 1);
        abstractTextAnnotation.setEnd(title.length() + text.length() + 1);
        abstractTextAnnotation.addToIndexes();

        Header header = new Header(cas);
        header.setBegin(0);
        header.setEnd(text.length() + title.length());
        header.setDocId(id);
        header.setSource(source);
        header.addToIndexes();
    }

}
