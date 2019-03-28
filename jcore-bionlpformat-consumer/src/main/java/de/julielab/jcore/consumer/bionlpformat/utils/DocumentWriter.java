 /**
  * Copyright (c) 2015, JULIE Lab.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the BSD-2-Clause License
  */
package de.julielab.jcore.consumer.bionlpformat.utils;

 import org.apache.uima.jcas.JCas;

 import java.io.IOException;
 import java.io.Writer;

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
