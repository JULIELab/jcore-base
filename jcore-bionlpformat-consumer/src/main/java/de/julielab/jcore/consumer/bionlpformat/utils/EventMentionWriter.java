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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.jcas.cas.FSArray;

import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.medical.Dose;
import de.julielab.jcore.types.medical.Duration;
import de.julielab.jcore.types.medical.Frequency;
import de.julielab.jcore.types.medical.Medication;
import de.julielab.jcore.types.medical.Modus;
import de.julielab.jcore.types.medical.Reason;

public class EventMentionWriter {
	private Writer writer;
	private String documentText;
	private Set<String> writtenIds;
	private int eventId = 1;

	public Writer getFileWriter() {
		return writer;
	}

	public void setFileWriter(Writer writer) {
		this.writer = writer;
	}
	
	public EventMentionWriter(Writer writer, String documentText) {
		super();
		this.writer = writer;
		this.documentText = documentText;
		this.writtenIds = new HashSet<String>();
	}

	public void close() throws IOException {
		writer.close();
	}

	public void writeEvent(Medication med) {
		
		FSArray dosArray = med.getDose();
		FSArray durArray = med.getDuration();
		FSArray freqArray = med.getFrequency();
		FSArray modArray = med.getModus();
		FSArray reasArray = med.getReason();
		
		if (dosArray != null) {
			for (int i=0; i<dosArray.size(); i++) {
//				Dose dos = (Dose) dosArray.get(i);
//				writeLine(dos);
				EventMention em = (EventMention) dosArray.get(i);
				writeTrigger(em);
				writeEvent(med, em);
			}
		}
		
		if (durArray != null) {
			for (int i=0; i<durArray.size(); i++) {
//				Duration dur = (Duration) durArray.get(i);
//				writeLine(dur);
				EventMention em = (EventMention) durArray.get(i);
				writeTrigger(em);
				writeEvent(med, em);
			}
		}
		
		if (freqArray != null) {
			for (int i=0; i<freqArray.size(); i++) {
//				Frequency freq = (Frequency) freqArray.get(i);
//				writeLine(freq);
				EventMention em = (EventMention) freqArray.get(i);
				writeTrigger(em);
				writeEvent(med, em);
			}
		}
		
		if (modArray != null) {
			for (int i=0; i<modArray.size(); i++) {
//				Modus mod = (Modus) modArray.get(i);
//				writeLine(mod);
				EventMention em = (EventMention) modArray.get(i);
				writeTrigger(em);
				writeEvent(med, em);
			}
		}
		
		if (reasArray != null) {
			for (int i=0; i<reasArray.size(); i++) {
				EventMention em = (EventMention) reasArray.get(i);
				writeTrigger(em);
				writeEvent(med, em);
			}
		}
	}
	
	private void writeEvent(Medication med, EventMention em) {
		// E1	Dose:T3 Dose-Arg:T2
		String line = "E" + Integer.toString(eventId++) + "\t" +
				em.getSpecificType() + ":" + em.getId() + " " +
				em.getSpecificType() + "-Arg:" + med.getId() + "\n";
	
		try {
			writer.write(line);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void writeTrigger(EventMention em) {
		String line = em.getId() + "\t" + em.getSpecificType() + " " +
				  em.getBegin() + " " + em.getEnd() + "\t" + 
				  documentText.substring(em.getBegin(), em.getEnd()) + "\n";
	
		try {
			writer.write(line);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isWritten(EventMention event) {
		return writtenIds.contains(event.getId());
	}
}
