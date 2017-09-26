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
package de.julielab.jcore.reader.pubtator;

import org.apache.commons.lang3.Range;

public class PubtatorEntity {
	private String docId;
	private Range<Integer> offsets;
	private String text;
	private String entityType;
	private String entityId;

	public PubtatorEntity(String docId, String begin, String end, String type, String id) {
		this.docId = docId;
		try {
			this.offsets = Range.between(Integer.parseInt(begin), Integer.parseInt(end));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("The given start and end offsets are no integers: " + begin + ", " + end,
					e);
		}
		this.entityType = type;
		this.entityId = id;
	}

	public PubtatorEntity(String docId, String begin, String end, String type) {
		this(docId, begin, end, type, null);
	}

	public String getDocId() {
		return docId;
	}

	public void setDocId(String docId) {
		this.docId = docId;
	}

	public Range<Integer> getOffsets() {
		return offsets;
	}

	public void setOffsets(Range<Integer> offsets) {
		this.offsets = offsets;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public int getBegin() {
		return offsets.getMinimum();
	}

	public int getEnd() {
		return offsets.getMaximum();
	}
}
