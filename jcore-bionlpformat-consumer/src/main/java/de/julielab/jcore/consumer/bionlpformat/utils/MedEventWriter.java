/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
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
