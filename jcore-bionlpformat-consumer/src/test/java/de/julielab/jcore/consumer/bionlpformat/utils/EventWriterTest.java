/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.consumer.bionlpformat.utils;

import de.julielab.jcore.types.*;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;


public class EventWriterTest {
	private static final String EVENT_E8 = "E8	Phosphorylation:T14 Theme:T17 Site:T13\n";

	private JCas cas;
	private BioEventWriter eventWriter;
	private EventTriggerWriter triggerWriter;
	private EntityWriter entityWriter;
	private ProteinWriter proteinWriter;
	private Writer writer;
	
	private EventMention eventE8;
	private EventTrigger triggerT14;
	private Gene proteinT17;
	private EntityMention entityT13;
	
	@BeforeEach
	public void setUp() throws Exception{
		cas = JCasFactory.createJCas("src/test/resources/types/jcore-semantics-biology-types");
		
		writer = createMock(FileWriter.class);
		triggerWriter = createMock(EventTriggerWriter.class);
		entityWriter = createMock(EntityWriter.class);
		proteinWriter = createMock(ProteinWriter.class);
		
		eventWriter = new BioEventWriter(writer, proteinWriter, triggerWriter, entityWriter);
				
		triggerT14 = new EventTrigger(cas);
		triggerT14.setId("T14");
		triggerT14.setSpecificType("Phosphorylation");

		proteinT17 = new Gene(cas);
		proteinT17.setId("T17");
		proteinT17.setSpecificType("protein");
		
		entityT13 = new EntityMention(cas);
		entityT13.setId("T13");
		
		eventE8 = new EventMention(cas);
		eventE8.setId("E8");
		eventE8.setTrigger(triggerT14);
		FSArray arguments = new FSArray(cas, 2);

		ArgumentMention argument1 = new ArgumentMention(cas);
		argument1.setRole("Theme");
		argument1.setRef(proteinT17);
		arguments.set(0, argument1);

		ArgumentMention argument2 = new ArgumentMention(cas);
		argument2.setRole("Site");
		argument2.setRef(entityT13);
		arguments.set(1, argument2);
		
		eventE8.setArguments(arguments);	
	}

	@Test
	public void testWriteEventE8() throws Exception{
		writer.write(EVENT_E8);
		expect(triggerWriter.isWritten(triggerT14)).andReturn(false);
		triggerWriter.writeTrigger(triggerT14);
		expect(proteinWriter.isWritten(proteinT17)).andReturn(false);
		proteinWriter.writeProtein(proteinT17);
		expect(entityWriter.isWritten(entityT13)).andReturn(false);
		entityWriter.writeEntity(entityT13);
		
		replay(writer);
		replay(triggerWriter);
		replay(proteinWriter);
		replay(entityWriter);
		
		eventWriter.writeEvent(eventE8);
		
		verify(writer);
		verify(triggerWriter);
		verify(proteinWriter);
		verify(entityWriter);
	}

	@Test
	public void testClose() throws IOException{
		writer.close();
		triggerWriter.close();
		proteinWriter.close();
		entityWriter.close();
		replay(writer);
		replay(triggerWriter);
		replay(proteinWriter);
		replay(entityWriter);
		
		eventWriter.close();

		verify(writer);
		verify(triggerWriter);
		verify(proteinWriter);
		verify(entityWriter);
	}
}
