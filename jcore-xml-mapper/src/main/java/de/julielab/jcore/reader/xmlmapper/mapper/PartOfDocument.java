/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.reader.xmlmapper.mapper;

public class PartOfDocument {
	private String xPath;
	private int[] begin;
	private int[] end;
	private String[] text;
	private int id;
	// If set, this parser is used to get the text of the text part pointed to
	// by xPath. The parser might also set additional annotations internally.
	private DocumentTextPartParser parser;

	public DocumentTextPartParser getParser() {
		return parser;
	}

	public PartOfDocument(int id) {
		this.id = id;
	}

	public String getXPath() {
		return xPath;
	}

	public void setxPath(String xPath) {
		this.xPath = xPath;
	}

	/**
	 * Returns the very beginning of the document part, i.e. the first begin of
	 * the first substructure (if any).
	 * 
	 * @return
	 */
	public int getBegin() {
		if (begin.length > 0)
			return begin[0];
		return 0;
	}

	public int[] getBeginOffsets() {
		return begin;
	}

	public void setBeginOffsets(int[] begin) {
		this.begin = begin;
	}

	/**
	 * Returns the very end of the document part, i.e. the last end of the last
	 * substructure (if any).
	 * 
	 * @return
	 */
	public int getEnd() {
		if (end.length > 0)
			return end[end.length - 1];
		return 0;
	}

	public int[] getEndOffsets() {
		return end;
	}

	public void setEndOffsets(int[] end) {
		this.end = end;
	}

	public String[] getText() {
		return text;
	}

	public void setText(String[] text) {
		this.text = text;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setParser(DocumentTextPartParser parser) {
		this.parser = parser;
	}


}
