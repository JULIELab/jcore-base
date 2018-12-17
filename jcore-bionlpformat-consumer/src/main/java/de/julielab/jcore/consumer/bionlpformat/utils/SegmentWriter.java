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
package de.julielab.jcore.consumer.bionlpformat.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class SegmentWriter {
	
	private Writer writer;
	private Integer tcount;
	private String documentText;
	
	public Writer getFileWriter() {
		return writer;
	}

	public void setFileWriter(Writer writer) {
		this.writer = writer;
	}
	
	public SegmentWriter(Writer annotationFileWriter, String docText) {
		super();
		this.writer = annotationFileWriter;
		this.tcount = 1;
		this.documentText = docText;
	}

	public void writeSentence(Sentence sentence) {
		CAS cas = sentence.getCAS();
		Type tokenType = cas.getTypeSystem().getType(Token.class.getCanonicalName());
		FSIterator<AnnotationFS> tokenIter = cas.getAnnotationIndex(tokenType).subiterator(sentence);
		writeLine(sentence, "Sentence");
		writeTokens(tokenIter);
	}
	
	public void close() throws IOException {
		if (writer != null) writer.close();
//		if (medicationWriter != null)
//			medicationWriter.close();
//		if (attributesWriter != null)
//			attributesWriter.close();
	}
	
	private void writeLine(Annotation ann, String atype) {
		String out_line = "T" + (tcount++) + "\t" + atype + " ";
		Integer begin = 0;
		Boolean first = true;
		Pattern p = Pattern.compile(System.lineSeparator(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
		Matcher m = p.matcher(ann.getCoveredText());
		String begin_end;
		String text = "";
		while(m.find()) {
			if (!first && (m.start() == begin)) {
				begin += 1;
				continue;
			}
			String t = ann.getCoveredText().substring(begin, m.start());
			String ltrimmed = t.replaceAll("^\\s+", "");
			int tcount = t.indexOf(ltrimmed);
			
			text += ltrimmed + " ";
			begin_end = Integer.toString((ann.getBegin()+begin+tcount)) + " " + Integer.toString((ann.getBegin()+m.start())) + ";";
			out_line += begin_end;
			begin = m.end();
			first = false;
		}
		if (begin < ann.getCoveredText().length()) {
			String t = ann.getCoveredText().substring(begin, ann.getCoveredText().length());
			String ltrimmed = t.replaceAll("^\\s+", "");
			int tcount = t.indexOf(ltrimmed);
			
			text += ltrimmed;
			begin_end = Integer.toString((ann.getBegin()+begin+tcount)) + " " + Integer.toString((ann.getBegin()+ann.getCoveredText().length()));
			out_line += begin_end;
		}
		else {
			text = text.substring(0, text.length()-1 >= 0 ? text.length()-1 : 0);
			out_line = out_line.substring(0, out_line.length()-1);
		}
		out_line += "\t" + text  + "\n";
			
		try {
			writer.write(out_line);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeTokens(FSIterator<AnnotationFS> tokenIter) {
		while (tokenIter.hasNext()) {
            Token token = (Token) tokenIter.next();
            writeLine(token, "Token");
        }
	}

	public void writeTokensOnly(CAS cas) {
		Type tokenType = cas.getTypeSystem().getType(Token.class.getCanonicalName());
		FSIterator<AnnotationFS> tokenIter = cas.getAnnotationIndex(tokenType).iterator();
		writeTokens(tokenIter);		
	}
}
