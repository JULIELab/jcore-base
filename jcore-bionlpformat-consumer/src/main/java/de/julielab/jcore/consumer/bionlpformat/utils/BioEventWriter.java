 /**
  * Copyright (c) 2015, JULIE Lab.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the BSD-2-Clause License
  */

package de.julielab.jcore.consumer.bionlpformat.utils;

 import de.julielab.jcore.types.*;
 import org.apache.uima.jcas.cas.FSArray;

 import java.io.IOException;
 import java.io.Writer;
 import java.util.HashSet;
 import java.util.Set;

public class BioEventWriter {

	private Writer writer;
	private ProteinWriter proteinWriter;
	private EventTriggerWriter triggerWriter;
	private EntityWriter entityWriter;
	private Set<String> writtenIds;

	public Writer getFileWriter() {
		return writer;
	}

	public void setFileWriter(Writer writer) {
		this.writer = writer;
	}

	public BioEventWriter(Writer writer, ProteinWriter proteinWriter, EventTriggerWriter triggerWriter,
			EntityWriter entityWriter) {
		super();
		this.writer = writer;
		this.proteinWriter = proteinWriter;
		this.triggerWriter = triggerWriter;
		this.entityWriter = entityWriter;
		this.writtenIds = new HashSet<String>();
	}

	public void writeEvent(EventMention event) throws IOException {
		if (null == event)
			throw new IllegalArgumentException("null reference has been passed instead on an EventMention instance.");
		String id = event.getId();
		if (!writtenIds.contains(id))
			writtenIds.add(id);
		else {
			System.out.println("ERROR! Event already written " + event.getId() + " " + event.getCoveredText());
			return;
		}
		EventTrigger trigger = event.getTrigger();
		if (null == trigger)
			throw new IllegalArgumentException("An EventMention without a trigger occurred: " + event);
		if (!triggerWriter.isWritten(trigger))
			triggerWriter.writeTrigger(trigger);
		String line = event.getId() + "\t" + trigger.getSpecificType() + ":" + trigger.getId() + " ";
		FSArray arguments = event.getArguments();
		if (null == arguments || 0 == arguments.size())
			throw new IllegalArgumentException("An EventMention without arguments occurred: " + event);
		for (int i = 0; i < arguments.size(); i++) {
			ArgumentMention argument = (ArgumentMention) arguments.get(i);
			if (argument != null) {
				Annotation reference = argument.getRef();
				if (null == reference)
					throw new IllegalArgumentException(
							"An argument with a null-entitiy-reference occurred: " + argument
									+ "; EventMention: "
									+ event);
				line += argument.getRole() + ":" + reference.getId();
				if (i < arguments.size() - 1)
					line += " ";
				if (proteinWriter != null && entityWriter != null) {
					if (reference instanceof Gene) {
						Gene protein = (Gene) reference;
						if (!proteinWriter.isWritten(protein) && protein.getSpecificType().equals("protein"))
							proteinWriter.writeProtein(protein);
					} else if (reference instanceof EntityMention) {
						EntityMention entityMention = (EntityMention) reference;
						if (!entityWriter.isWritten(entityMention))
							entityWriter.writeEntity((EntityMention) reference);
					}
				}
			}
		}
		line += "\n";
		writer.write(line);
	}

	public void close() throws IOException {
		if (writer != null)
			writer.close();
		if (proteinWriter != null)
			proteinWriter.close();
		if (triggerWriter != null)
			triggerWriter.close();
		if (entityWriter != null)
			entityWriter.close();
	}
}
