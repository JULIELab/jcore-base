package de.julielab.jcore.consumer.bionlpformat.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.cas.FSArray;

import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.medical.Medication;

public class MedEventWriter {
	private static final String MEDICATION_TYPE = "DRUG";
	
	private Writer writer;
	private EntityWriter medicationWriter;
	private EventMentionWriter attributesWriter;
	private Set<String> writtenIds;
	
	public Writer getFileWriter() {
		return writer;
	}

	public void setFileWriter(Writer writer) {
		this.writer = writer;
	}
	
	public MedEventWriter(Writer writer, EntityWriter medicationWriter,
			EventMentionWriter attributeWriter) {
		super();
		this.writer = writer;
		this.medicationWriter = medicationWriter;
		this.attributesWriter = attributeWriter;
		this.writtenIds = new HashSet<String>();
	}

	public void writeEvent(EntityMention entity) {
		if (null == entity)
			throw new IllegalArgumentException("null reference has been passed instead on an EventMention instance.");
		
		String id = entity.getId();
		if (!writtenIds.contains(id))
			writtenIds.add(id);
		else {
			System.out.println("ERROR! Event already written " + entity.getId() + " " + entity.getCoveredText());
			return;
		}
		
		if ( entity.getSpecificType().equals(MEDICATION_TYPE) ) {
			Medication med = (Medication) entity;
			try {
				medicationWriter.writeEntity(med);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			attributesWriter.writeEvent(med);
		}
		
	}

	public void close() throws IOException {
		if (writer != null)
			writer.close();
		if (medicationWriter != null)
			medicationWriter.close();
		if (attributesWriter != null)
			attributesWriter.close();
	}

}
