/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.reader.xmlmapper.mapper;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DocumentTextData{
	private String text;
	private HashMap<Integer, PartOfDocument> parts;
	Logger LOGGER = LoggerFactory.getLogger(DocumentTextData.class);
	
	public DocumentTextData() {
		parts = new HashMap<Integer, PartOfDocument>(); 
	}
	
	public void put(int id,PartOfDocument part){
		parts.put(id,part);
	}
	
	public String getText(){
		return text;
	}

	public PartOfDocument get(int id) {
		return parts.get(id);
	}

	public void setText(String text) {
		this.text=text;
	}

	public HashMap<Integer, PartOfDocument> getParts() {
		return parts;
	}

	public int size() {
		return this.parts.size();
	}
	
}
