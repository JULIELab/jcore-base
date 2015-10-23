/** 
 * AnnotationFileMapperTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: landefeld
 * 
 * Current version: //TODO insert current version number 	
 * Since version:   //TODO insert version number of first appearance of this class
 *
 * Creation date: 23.12.2008 
 * 
 * //TODO insert short description
 **/

package de.julielab.jcore.reader.bionlp09event.utils;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import de.julielab.jcore.reader.bionlp09event.utils.AnnotationFileMapper;
import de.julielab.jules.types.Annotation;
import de.julielab.jules.types.ArgumentMention;
import de.julielab.jules.types.Entity;
import de.julielab.jules.types.EntityMention;
import de.julielab.jules.types.EventMention;
import de.julielab.jules.types.EventTrigger;
import de.julielab.jules.types.Gene;



public class AnnotationFileMapperTest {

	private static final String DESCRIPTOR_FILE = "src/test/resources/de/julielab/jcore/reader/bionlp09event/desc/EventReaderTest.xml";
	private static final String EXAMPLE_1 = "T1	Protein 34 39	STAT6";
	private static final String EXAMPLE_2 = "T38	Entity 322 330	tyrosine";
	private static final String EXAMPLE_3 = "T33	Negative_regulation 12 19	inhibit";
	private static final String EXAMPLE_4 = "E1	Negative_regulation:T33 Theme:E2 Cause:E3";
	private static final String EXAMPLE_5 = "T34	Positive_regulation 20 30	activation";
	private static final String EXAMPLE_6 =	"T35	Positive_regulation 79 87	inducing";
	private static final String EXAMPLE_7 = "E2	Positive_regulation:T34 Theme:T1 Cause:T2";
	private static final String EXAMPLE_8 = "E3	Positive_regulation:T35 Theme:E4";
	private static final String EXAMPLE_9 = "E4	Gene_expression:T36 Theme:T3";
	private static final String EXAMPLE_10 = "T36	Gene_expression 100 110	expression";
	private static final String EXAMPLE_11 = "T9	Binding 246 253	binding";
	private static final String EXAMPLE_12 = "T17	Regulation 711 720	regulated";
	private static final String EXAMPLE_13 = "*	Equiv T38 T39";
	private static final String EXAMPLE_14 = "T39	Entity 322 330	tyrosine";
	
	private JCas cas;
	private AnnotationFileMapper annotationFileMapper;
	
	private Gene t1;
	private Gene t2;
	private Gene t3;
	private Map<String, Annotation> mappedProteins;
	
	@Before
	public void setUp() throws Exception {
		CollectionReaderDescription readerDescription = (CollectionReaderDescription) UIMAFramework.getXMLParser().parseCollectionReaderDescription(new XMLInputSource(DESCRIPTOR_FILE));
		CollectionReader collectionReader = UIMAFramework.produceCollectionReader(readerDescription);
		cas = CasCreationUtils.createCas(collectionReader.getProcessingResourceMetaData()).getJCas();
		annotationFileMapper = new AnnotationFileMapper();
		
		t1 = new Gene(cas);
		t1.setBegin(34);
		t1.setEnd(39);
		t1.setId("T1");
		
		t2 = new Gene(cas);
		t2.setBegin(43);
		t2.setEnd(56);
		t2.setId("T2");
		
		t3 = new Gene(cas);
		t3.setBegin(88);
		t3.setEnd(94);
		t3.setId("T3");
		
		mappedProteins = new HashMap<String, Annotation>();
		mappedProteins.put("T1", t1);
		mappedProteins.put("T2", t2);
		mappedProteins.put("T3", t3);
	}
	
	@Test
	public void testMapProteinFile() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_1);
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);
		
		annotationFileMapper.mapProteinFile(bufferedReader, cas);
		verify(bufferedReader);
		
		Type proteinType = cas.getTypeSystem().getType("de.julielab.jules.types.Gene");
		Gene protein = (Gene) cas.getAnnotationIndex(proteinType).iterator().next();
		assertNotNull(protein);
		assertEquals(34, protein.getBegin());
		assertEquals(39, protein.getEnd());
		assertEquals("T1", protein.getId());
		assertEquals("protein", protein.getSpecificType());				
	}
	
	@Test
	public void testMapEventFileEntityMention() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_2);
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);
		
		annotationFileMapper.mapEventFile(null, bufferedReader, cas);
		verify(bufferedReader);
		
		Type entityMentionType = cas.getTypeSystem().getType("de.julielab.jules.types.EntityMention");
		EntityMention entityMention = (EntityMention) cas.getAnnotationIndex(entityMentionType).iterator().next();
		assertNotNull(entityMention);
		assertEquals(322, entityMention.getBegin());
		assertEquals(330, entityMention.getEnd());
		assertEquals("T38", entityMention.getId());
		assertEquals("entity", entityMention.getSpecificType());					
	}

	@Test
	public void testMapEventFileEventTriggers() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_3);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_11);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_12);
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);
		
		annotationFileMapper.mapEventFile(null, bufferedReader, cas);
		verify(bufferedReader);
		
		Type eventTriggerType = cas.getTypeSystem().getType("de.julielab.jules.types.EventTrigger");
		FSIterator iterator = cas.getAnnotationIndex(eventTriggerType).iterator();

		EventTrigger eventTriggerT33 = (EventTrigger) iterator.next();
		assertNotNull(eventTriggerT33);
		assertEquals(12, eventTriggerT33.getBegin());
		assertEquals(19, eventTriggerT33.getEnd());
		assertEquals("T33", eventTriggerT33.getId());
		assertEquals("Negative_regulation", eventTriggerT33.getSpecificType());

		EventTrigger eventTriggerT9 = (EventTrigger) iterator.next();
		assertNotNull(eventTriggerT9);
		assertEquals(246, eventTriggerT9.getBegin());
		assertEquals(253, eventTriggerT9.getEnd());
		assertEquals("T9", eventTriggerT9.getId());
		assertEquals("Binding", eventTriggerT9.getSpecificType());	

		EventTrigger eventTriggerT17 = (EventTrigger) iterator.next();
		assertNotNull(eventTriggerT17);
		assertEquals(711, eventTriggerT17.getBegin());
		assertEquals(720, eventTriggerT17.getEnd());
		assertEquals("T17", eventTriggerT17.getId());
		assertEquals("Regulation", eventTriggerT17.getSpecificType());	
	}

	@Test
	public void testMapEventFileGeneExpression() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_10);
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);
		
		annotationFileMapper.mapEventFile(null, bufferedReader, cas);
		verify(bufferedReader);
		
		Type eventTriggerType = cas.getTypeSystem().getType("de.julielab.jules.types.EventTrigger");
		EventTrigger eventTrigger = (EventTrigger) cas.getAnnotationIndex(eventTriggerType).iterator().next();
		assertNotNull(eventTrigger);
		assertEquals(100, eventTrigger.getBegin());
		assertEquals(110, eventTrigger.getEnd());
		assertEquals("T36", eventTrigger.getId());
		assertEquals("Gene_expression", eventTrigger.getSpecificType());
	}

	@Test
	public void testMapEventFileGeneExpressionTriggeredEvent() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_9);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_10);
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);
		
		annotationFileMapper.mapEventFile(mappedProteins, bufferedReader, cas);
		verify(bufferedReader);

		Type eventTriggerType = cas.getTypeSystem().getType("de.julielab.jules.types.EventTrigger");
		EventTrigger eventTrigger = (EventTrigger) cas.getAnnotationIndex(eventTriggerType).iterator().next();
		Type eventMentionType = cas.getTypeSystem().getType("de.julielab.jules.types.EventMention");
		EventMention eventMention = (EventMention) cas.getAnnotationIndex(eventMentionType).iterator().next();
		
		assertNotNull(eventMention);
		assertEquals(100, eventMention.getBegin());
		assertEquals(110, eventMention.getEnd());
		assertEquals(eventTrigger, eventMention.getTrigger());
		assertEquals(1, eventMention.getArguments().size());
		assertEquals("E4", eventMention.getId());
		assertEquals("Gene_expression", eventMention.getSpecificType());
		
		ArgumentMention argumentMention = (ArgumentMention) eventMention.getArguments().get(0);
		assertEquals(t3, argumentMention.getRef());
		assertEquals(88, argumentMention.getBegin());
		assertEquals(94, argumentMention.getEnd());
		assertEquals("Theme", argumentMention.getRole());
	}

	@Test
	public void testMapEventFilePositiveRegulationTriggeredEvent() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_6);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_8);		
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_9);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_10);
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);
		
		annotationFileMapper.mapEventFile(mappedProteins, bufferedReader, cas);
		verify(bufferedReader);

		Type eventTriggerType = cas.getTypeSystem().getType("de.julielab.jules.types.EventTrigger");
		EventTrigger eventTrigger = (EventTrigger) cas.getAnnotationIndex(eventTriggerType).iterator().next();
		Type eventMentionType = cas.getTypeSystem().getType("de.julielab.jules.types.EventMention");
		FSIterator iterator= cas.getAnnotationIndex(eventMentionType).iterator();
		EventMention eventMentionE3 = (EventMention) iterator.next();
		EventMention eventMentionE4 = (EventMention) iterator.next();
		
		assertNotNull(eventMentionE3);
		assertEquals(79, eventMentionE3.getBegin());
		assertEquals(87, eventMentionE3.getEnd());
		assertEquals(eventTrigger, eventMentionE3.getTrigger());
		assertEquals(1, eventMentionE3.getArguments().size());
		assertEquals("E3", eventMentionE3.getId());
		assertEquals("Positive_regulation", eventMentionE3.getSpecificType());
		
		ArgumentMention argumentMention = (ArgumentMention) eventMentionE3.getArguments().get(0);
		assertEquals(eventMentionE4, argumentMention.getRef());
		assertEquals(100, argumentMention.getBegin());
		assertEquals(110, argumentMention.getEnd());
		assertEquals("Theme", argumentMention.getRole());
	}
	
	@Test
	public void testMapEventFilePositiveRegulationTriggeredEventWithCause() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_1);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_5);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_7);		
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);

		annotationFileMapper.mapEventFile(mappedProteins, bufferedReader, cas);
		verify(bufferedReader);

		Type eventTriggerType = cas.getTypeSystem().getType("de.julielab.jules.types.EventTrigger");
		EventTrigger eventTriggerT34 = (EventTrigger) cas.getAnnotationIndex(eventTriggerType).iterator().next();
		Type eventMentionType = cas.getTypeSystem().getType("de.julielab.jules.types.EventMention");
		EventMention eventMentionE2 = (EventMention) cas.getAnnotationIndex(eventMentionType).iterator().next();
		Type entityMentionType = cas.getTypeSystem().getType("de.julielab.jules.types.EntityMention");
		FSIterator iterator = cas.getAnnotationIndex(entityMentionType).iterator();
		EntityMention entityMentionT1 = (EntityMention) iterator.next();


		assertNotNull(eventMentionE2);
		assertEquals(20, eventMentionE2.getBegin());
		assertEquals(30, eventMentionE2.getEnd());
		assertEquals(eventTriggerT34, eventMentionE2.getTrigger());
		assertEquals(2, eventMentionE2.getArguments().size());
		assertEquals("E2", eventMentionE2.getId());
		assertEquals("Positive_regulation", eventMentionE2.getSpecificType());
		
		ArgumentMention argumentMention1 = (ArgumentMention) eventMentionE2.getArguments().get(0);
		assertEquals(entityMentionT1, argumentMention1.getRef());
		assertEquals(34, argumentMention1.getBegin());
		assertEquals(39, argumentMention1.getEnd());
		assertEquals("Theme", argumentMention1.getRole());

		ArgumentMention argumentMention2 = (ArgumentMention) eventMentionE2.getArguments().get(1);
		assertEquals(t2, argumentMention2.getRef());
		assertEquals(43, argumentMention2.getBegin());
		assertEquals(56, argumentMention2.getEnd());
		assertEquals("Cause", argumentMention2.getRole());
		
	}	

	@Test
	public void testMapEventFileNegativeRegulationWithEventArguments() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_1);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_3);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_4);		
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_5);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_6);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_7);		
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_8);		
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_9);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_10);
		
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);
		
		annotationFileMapper.mapEventFile(mappedProteins, bufferedReader, cas);
		
		verify(bufferedReader);
		
		Type eventMentionType = cas.getTypeSystem().getType("de.julielab.jules.types.EventMention");
		FSIterator iterator = cas.getAnnotationIndex(eventMentionType).iterator();
		EventMention eventMentionE1 = (EventMention) iterator.next();
		EventMention eventMentionE2 = (EventMention) iterator.next();
		EventMention eventMentionE3 = (EventMention) iterator.next();
		
		Type eventTriggerType = cas.getTypeSystem().getType("de.julielab.jules.types.EventTrigger");
		EventTrigger eventTriggerT33 = (EventTrigger) cas.getAnnotationIndex(eventTriggerType).iterator().next();
			
		assertNotNull(eventMentionE1);
		assertEquals(12, eventMentionE1.getBegin());
		assertEquals(19, eventMentionE1.getEnd());
		assertEquals(eventTriggerT33, eventMentionE1.getTrigger());
		assertEquals(2, eventMentionE1.getArguments().size());
		assertEquals("E1", eventMentionE1.getId());
		assertEquals("Negative_regulation", eventMentionE1.getSpecificType());
		
		ArgumentMention argumentMention1E1 = (ArgumentMention) eventMentionE1.getArguments().get(0);
		assertEquals(eventMentionE2, argumentMention1E1.getRef());
		assertEquals(20, argumentMention1E1.getBegin());
		assertEquals(30, argumentMention1E1.getEnd());
		assertEquals("Theme", argumentMention1E1.getRole());
		
		ArgumentMention argumentMention2E1 = (ArgumentMention) eventMentionE1.getArguments().get(1);
		assertEquals(eventMentionE3, argumentMention2E1.getRef());
		assertEquals(79, argumentMention2E1.getBegin());
		assertEquals(87, argumentMention2E1.getEnd());
		assertEquals("Cause", argumentMention2E1.getRole());
	}

	@Test
	public void testMapEventFileEquality() throws Exception{
		BufferedReader bufferedReader = createMock(BufferedReader.class);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_2);		
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_13);
		expect(bufferedReader.readLine()).andReturn(EXAMPLE_14);
		
		expect(bufferedReader.readLine()).andReturn(null);
		replay(bufferedReader);
		
		annotationFileMapper.mapEventFile(mappedProteins, bufferedReader, cas);
		
		verify(bufferedReader);
		
		Type entityType = cas.getTypeSystem().getType(Entity.class.getCanonicalName());
		FSIterator iterator = cas.getAnnotationIndex(entityType).iterator();
		Entity entity = (Entity) iterator.next();
		FSArray mentions = entity.getMentions();
		assertNotNull(mentions);
		assertEquals(2, mentions.size());
		
		EntityMention mention1 = (EntityMention) mentions.get(0);
		assertEquals("T38", mention1.getId());
		assertEquals(entity, mention1.getRef());
		
		EntityMention mention2 = (EntityMention) mentions.get(1);
		assertEquals("T39", mention2.getId());
		assertEquals(entity, mention2.getRef());
				
	}
	
}
