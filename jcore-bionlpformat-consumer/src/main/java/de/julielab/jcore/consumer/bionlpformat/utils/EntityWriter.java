 /**
  * Copyright (c) 2015, JULIE Lab.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the BSD-2-Clause License
  */

package de.julielab.jcore.consumer.bionlpformat.utils;

 import de.julielab.jcore.types.EntityMention;

 import java.io.IOException;
 import java.io.Writer;
 import java.util.HashSet;
 import java.util.Set;

public class EntityWriter {
	private Writer writer;
	private String documentText;
	private Set<String> writtenIds;
	
	public Writer getFileWriter() {
		return writer;
	}

	public void setFileWriter(Writer writer) {
		this.writer = writer;
	}

	public EntityWriter(Writer writer, String documentText) {
		super();
		this.writer = writer;
		this.documentText = documentText;
		this.writtenIds = new HashSet<String>();
	}

	public void writeEntity(EntityMention entity) throws IOException {
		String id = entity.getId();
		String etype = "Entity";
		writtenIds.add(id);
		
		if (entity.getSpecificType() != null) {
			etype = (entity.getSpecificType().equals("DRUG")) ? "Medication" : "Entity";
		}
		
		String line = "";
		line = entity.getId() + "\t" + etype + " " +
					  entity.getBegin() + " " + entity.getEnd() + "\t" + 
					  documentText.substring(entity.getBegin(), entity.getEnd()) + "\n";
		writer.write(line);
	}

	public void close() throws IOException {
		writer.close();
	}

	public boolean isWritten(EntityMention entity) {
		return writtenIds.contains(entity.getId());
	}
}
