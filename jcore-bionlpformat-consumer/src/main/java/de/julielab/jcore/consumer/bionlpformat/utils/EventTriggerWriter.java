 /**
  * Copyright (c) 2015, JULIE Lab.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
  */

package de.julielab.jcore.consumer.bionlpformat.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import de.julielab.jcore.types.EventTrigger;

public class EventTriggerWriter {
	private Writer writer;
	private String documentText;
	private Set<String> writtenIds;
	
	public Writer getFileWriter() {
		return writer;
	}

	public void setFileWriter(Writer writer) {
		this.writer = writer;
	}

	public EventTriggerWriter(Writer writer, String documentText) {
		super();
		this.writer = writer;
		this.documentText = documentText;
		this.writtenIds = new HashSet<String>();
	}
	
	public void writeTrigger(EventTrigger trigger) throws IOException {
		String id = trigger.getId();
		if( !writtenIds.contains(id) )
			writtenIds.add(id);
		
		String line = trigger.getId() + "\t" + trigger.getSpecificType() + " " +
					  trigger.getBegin() + " " + trigger.getEnd() + "\t" +
					  documentText.substring(trigger.getBegin(), trigger.getEnd()) + "\n";
		
		writer.write(line);
	}

	public void close() throws IOException {
		writer.close();		
	}

	public boolean isWritten(EventTrigger trigger) {
		return writtenIds.contains(trigger.getId());
	}
}
