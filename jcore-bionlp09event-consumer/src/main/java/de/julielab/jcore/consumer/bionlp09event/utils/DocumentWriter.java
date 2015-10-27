 /**
  * Copyright (c) 2015, JULIE Lab.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
  */
package de.julielab.jcore.consumer.bionlp09event.utils;

import java.io.IOException;
import java.io.Writer;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Title;

public class DocumentWriter {

	private Writer writer;

	public DocumentWriter(Writer writer) {
		this.writer = writer;
	}

	public void write(JCas cas) throws IOException {
//		Type titleType = cas.getTypeSystem().getType(Title.class.getCanonicalName());
//		Type abstractTextType = cas.getTypeSystem().getType(AbstractText.class.getCanonicalName());
//		
//		Title title = (Title) cas.getAnnotationIndex(titleType).iterator().next();
//		AbstractText abstractText = (AbstractText) cas.getAnnotationIndex(abstractTextType).iterator().next();
		String documentText = cas.getDocumentText();
//		
//		writer.write(documentText.substring(title.getBegin(), title.getEnd())+"\n");
//		writer.write(documentText.substring(abstractText.getBegin(), abstractText.getEnd())+"\n");
		writer.write(documentText + "\n");
	}

	public void close() throws IOException {
		writer.close();
	}

}
