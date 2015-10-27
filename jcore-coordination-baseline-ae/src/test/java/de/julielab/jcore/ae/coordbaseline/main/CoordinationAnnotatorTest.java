/** 
 * CoordinationAnnotatorTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: Lichtenwald
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 26.09.2007 
 * 
 * Test for the wrapper for the baseline (prediction of EEEs, coordination elements (conjunctions, conjuncts, antecedents) and resolved ellipses
 * The tested sentence is: Almost all of these mutations occur in X, Y, and Z cells; simple upstream and downstream sequence elements are indicated by negative elements.
 * This sentence contains two EEEs:
 * 1) X, Y, and Z cells (entity: variation-location)
 * 2) simple upstream and downstream sequence elements (entity: DNA)
 **/

package de.julielab.jcore.ae.coordbaseline.main;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.FSIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Coordination;
import de.julielab.jcore.types.CoordinationElement;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.Entity;
import de.julielab.jcore.types.EEE;
import junit.framework.TestCase;

import java.util.ArrayList;


public class CoordinationAnnotatorTest extends TestCase 
{

	private static final Logger LOGGER = LoggerFactory.getLogger(CoordinationAnnotatorTest.class);
	private static final String LOGGER_PROPERTIES = "src/test/java/log4j.properties";

	private static final String text = "Almost all of these mutations occur in X, Y, and Z cells; simple upstream and downstream sequence elements are indicated by negative elements.";
	private static final int tokenNumber = 27;
	private static final String EEE1 = "X, Y, and Z cells";
	private static final String EEE2 = "simple upstream and downstream sequence elements";
	private static final String ellipsis1 = "X cells, Y cells, and Z cells";
	private static final String ellipsis2 = "simple upstream sequence elements and simple downstream sequence elements";
	private static final String coordinationLabels1 = "conjunct,conjunction,conjunct,conjunction,conjunction,conjunct,antecedent";
	private static final String coordinationLabels2 = "antecedent,conjunct,conjunction,conjunct,antecedent,antecedent";
	private static final String TEST_DESC = "src/test/resources/desc/CoordinationAnnotatorTest.xml";
	
	
	protected void setUp() throws Exception 
	{
		super.setUp();
	} // of setUp
      
	public void initCas(JCas jcas) 
	{
		jcas.reset();
		
		/*--------------------------*/
		/* Initialize the sentence.	*/
		/*--------------------------*/
		jcas.setDocumentText(text);
		Sentence sentence = new Sentence(jcas);
		sentence.setBegin(0);
		sentence.setEnd(text.length());
		sentence.addToIndexes();  
		
		/*--------------------------*/
		/* Initialize the 27 tokens.*/
		/*--------------------------*/
		
		POSTag posTag = null;
		FSArray posTagFSArray = null;
		
		/*--------------*/
		/*  Almost|RB	*/
		/*--------------*/
		Token t1 = new Token(jcas);
		t1.setBegin(0); t1.setEnd(6);
			posTag = new POSTag(jcas);
			posTag.setBegin(0);
			posTag.setEnd(6);
			posTag.setValue("RB");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t1.setPosTag(posTagFSArray);
		t1.addToIndexes();
		
		/*--------------*/
		/*     all|DT	*/
		/*--------------*/
		Token t2 = new Token(jcas);
		t2.setBegin(7); t2.setEnd(10); 
			posTag = new POSTag(jcas);
			posTag.setBegin(7);
			posTag.setEnd(10);
			posTag.setValue("DT");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t2.setPosTag(posTagFSArray);
		t2.addToIndexes();
		
		
		/*----------*/
		/*  of|IN	*/
		/*----------*/
		Token t3 = new Token(jcas); 
		t3.setBegin(11); t3.setEnd(13);
			posTag = new POSTag(jcas);
			posTag.setBegin(11);
			posTag.setEnd(13);
			posTag.setValue("IN");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t3.setPosTag(posTagFSArray);	
		t3.addToIndexes();
		
		
		
		/*--------------*/
		/* 	 these|DT	*/
		/*--------------*/
		Token t4 = new Token(jcas); 
		t4.setBegin(14); t4.setEnd(19);
			posTag = new POSTag(jcas);
			posTag.setBegin(14);
			posTag.setEnd(19);
			posTag.setValue("DT");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t4.setPosTag(posTagFSArray);
		t4.addToIndexes();
		

		
		/*------------------*/
		/*   mutations|NNS	*/
		/*------------------*/
		Token t5 = new Token(jcas); 
		t5.setBegin(20); t5.setEnd(29);
			posTag = new POSTag(jcas);
			posTag.setBegin(20);
			posTag.setEnd(29);
			posTag.setValue("NNS");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t5.setPosTag(posTagFSArray);
		t5.addToIndexes();
		
		/*--------------*/
		/*  occur|VBP	*/
		/*--------------*/
		Token t6 = new Token(jcas); 
		t6.setBegin(30); t6.setEnd(35);
			posTag = new POSTag(jcas);
			posTag.setBegin(20);
			posTag.setEnd(35);
			posTag.setValue("VBP");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t6.setPosTag(posTagFSArray);
		t6.addToIndexes();
		
		/*--------------*/
		/*	   in|IN	*/
		/*--------------*/
		Token t7 = new Token(jcas); 
		t7.setBegin(36); t7.setEnd(38);
			posTag = new POSTag(jcas);
			posTag.setBegin(36);
			posTag.setEnd(38);
			posTag.setValue("IN");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t7.setPosTag(posTagFSArray);
		t7.addToIndexes();
		
		/*----------*/
		/*	  X|NN 	*/
		/*----------*/
		Token t8 = new Token(jcas); 
		t8.setBegin(39); t8.setEnd(40);
			posTag = new POSTag(jcas);
			posTag.setBegin(39);
			posTag.setEnd(40);
			posTag.setValue("NN");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t8.setPosTag(posTagFSArray);
		t8.addToIndexes();
		
		
		/*--------------*/
		/*	   ,|,		*/
		/*--------------*/
		Token t9 = new Token(jcas); 
		t9.setBegin(40); t9.setEnd(41);
			posTag = new POSTag(jcas);
			posTag.setBegin(40);
			posTag.setEnd(41);
			posTag.setValue(",");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t9.setPosTag(posTagFSArray);
		t9.addToIndexes();
		
		
		/*--------------*/
		/*	   Y|NN		*/
		/*--------------*/
		Token t10 = new Token(jcas); 
		t10.setBegin(42); t10.setEnd(43);
			posTag = new POSTag(jcas);
			posTag.setBegin(42);
			posTag.setEnd(43);
			posTag.setValue("NN");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t10.setPosTag(posTagFSArray);
		t10.addToIndexes();
		
		
		/*--------------*/
		/*	   ,|,		*/
		/*--------------*/
		Token t11 = new Token(jcas); 
		t11.setBegin(43); t11.setEnd(44);
			posTag = new POSTag(jcas);
			posTag.setBegin(43);
			posTag.setEnd(44);
			posTag.setValue(",");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t11.setPosTag(posTagFSArray);
		t11.addToIndexes();
		
		
		/*--------------*/
		/*	   and|CC	*/
		/*--------------*/
		Token t12 = new Token(jcas); 
		t12.setBegin(45); t12.setEnd(48);
			posTag = new POSTag(jcas);
			posTag.setBegin(45);
			posTag.setEnd(48);
			posTag.setValue("CC");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t12.setPosTag(posTagFSArray);
		t12.addToIndexes();
		
		
		/*--------------*/
		/*	   Z|NN		*/
		/*--------------*/
		Token t13 = new Token(jcas); 
		t13.setBegin(49); t13.setEnd(50);
			posTag = new POSTag(jcas);
			posTag.setBegin(49);
			posTag.setEnd(50);
			posTag.setValue("NN");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t13.setPosTag(posTagFSArray);
		t13.addToIndexes();
		
		
		/*------------------*/
		/*	   cells|NNS	*/
		/*------------------*/
		Token t14 = new Token(jcas); 
		t14.setBegin(51); t14.setEnd(56);
			posTag = new POSTag(jcas);
			posTag.setBegin(51);
			posTag.setEnd(56);
			posTag.setValue("NNS");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t14.setPosTag(posTagFSArray);
		t14.addToIndexes();
		
		
		/*--------------*/
		/*	   ;|;		*/
		/*--------------*/
		Token t15 = new Token(jcas); 
		t15.setBegin(56); t15.setEnd(57);
			posTag = new POSTag(jcas);
			posTag.setBegin(56);
			posTag.setEnd(57);
			posTag.setValue(";");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t15.setPosTag(posTagFSArray);
		t15.addToIndexes();
		
		
		/*------------------*/
		/*	   simple|JJ	*/
		/*--------------*/
		Token t16 = new Token(jcas); 
		t16.setBegin(58); t16.setEnd(64);
			posTag = new POSTag(jcas);
			posTag.setBegin(58);
			posTag.setEnd(64);
			posTag.setValue("JJ");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t16.setPosTag(posTagFSArray);
		t16.addToIndexes();
		
		
		/*------------------*/
		/*	  upstream|JJ	*/
		/*------------------*/
		Token t17 = new Token(jcas); 
		t17.setBegin(65); t17.setEnd(73);
			posTag = new POSTag(jcas);
			posTag.setBegin(65);
			posTag.setEnd(73);
			posTag.setValue("JJ");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t17.setPosTag(posTagFSArray);
		t17.addToIndexes();
		
		
		/*--------------*/
		/*	   and|CC	*/
		/*--------------*/
		Token t18 = new Token(jcas); 
		t18.setBegin(74); t18.setEnd(77);
			posTag = new POSTag(jcas);
			posTag.setBegin(74);
			posTag.setEnd(77);
			posTag.setValue("CC");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t18.setPosTag(posTagFSArray);
		t18.addToIndexes();
		
		
		/*----------------------*/
		/*	   downstream|JJ	*/
		/*----------------------*/
		Token t19 = new Token(jcas); 
		t19.setBegin(78); t19.setEnd(88);
			posTag = new POSTag(jcas);
			posTag.setBegin(78);
			posTag.setEnd(88);
			posTag.setValue("JJ");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t19.setPosTag(posTagFSArray);
		t19.addToIndexes();
		
		
		/*------------------*/
		/*	  sequence|NN	*/
		/*------------------*/
		Token t20 = new Token(jcas); 
		t20.setBegin(89); t20.setEnd(97);
			posTag = new POSTag(jcas);
			posTag.setBegin(89);
			posTag.setEnd(97);
			posTag.setValue("NN");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t20.setPosTag(posTagFSArray);
		t20.addToIndexes();
		
		
		/*------------------*/
		/*	 elements|NNS	*/
		/*------------------*/
		Token t21 = new Token(jcas); 
		t21.setBegin(98); t21.setEnd(106);
			posTag = new POSTag(jcas);
			posTag.setBegin(98);
			posTag.setEnd(106);
			posTag.setValue("NNS");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t21.setPosTag(posTagFSArray);
		t21.addToIndexes();
		
		
		/*--------------*/
		/*	   are|VBP	*/
		/*--------------*/
		Token t22 = new Token(jcas); 
		t22.setBegin(107); t22.setEnd(110);
			posTag = new POSTag(jcas);
			posTag.setBegin(107);
			posTag.setEnd(110);
			posTag.setValue("VBP");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t22.setPosTag(posTagFSArray);
		t22.addToIndexes();
		
		
		/*----------------------*/
		/*	   indicated|VBD	*/
		/*----------------------*/
		Token t23 = new Token(jcas); 
		t23.setBegin(111); t23.setEnd(120);
			posTag = new POSTag(jcas);
			posTag.setBegin(111);
			posTag.setEnd(120);
			posTag.setValue("VBD");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t23.setPosTag(posTagFSArray);
		t23.addToIndexes();
		
		
		/*--------------*/
		/*	   by|IN	*/
		/*--------------*/
		Token t24 = new Token(jcas); 
		t24.setBegin(121); t24.setEnd(123);
			posTag = new POSTag(jcas);
			posTag.setBegin(121);
			posTag.setEnd(123);
			posTag.setValue("IN");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t24.setPosTag(posTagFSArray);
		t24.addToIndexes();
		
		
		/*------------------*/
		/*	  negative|JJ	*/
		/*------------------*/
		Token t25 = new Token(jcas); 
		t25.setBegin(124); t25.setEnd(132);
			posTag = new POSTag(jcas);
			posTag.setBegin(124);
			posTag.setEnd(132);
			posTag.setValue("JJ");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t25.setPosTag(posTagFSArray);
		t25.addToIndexes();
		
		
		/*------------------*/
		/*	 elements|NNS	*/
		/*------------------*/
		Token t26 = new Token(jcas); 
		t26.setBegin(133); t26.setEnd(141);
			posTag = new POSTag(jcas);
			posTag.setBegin(133);
			posTag.setEnd(141);
			posTag.setValue("NNS");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t26.setPosTag(posTagFSArray);
		t26.addToIndexes();
		
		
		/*--------------*/
		/*	   .|.		*/
		/*--------------*/
		Token t27 = new Token(jcas); 
		t27.setBegin(141); t27.setEnd(142);
			posTag = new POSTag(jcas);
			posTag.setBegin(141);
			posTag.setEnd(142);
			posTag.setValue(".");
			posTag.addToIndexes();
			posTagFSArray = new FSArray(jcas, 10);
			posTagFSArray.set(0, posTag);
			posTagFSArray.addToIndexes();
		t27.setPosTag(posTagFSArray);
		t27.addToIndexes();
		
		
		
		/*------------------------------*/
		/* Initialize the 3 entities.	*/
		/*------------------------------*/
		Entity entity1 = new Entity(jcas);
		entity1.setBegin(20);
		entity1.setEnd(29);
		entity1.setSpecificType("variation-event");
		entity1.addToIndexes();
		
		Entity entity2 = new Entity(jcas);
		entity2.setBegin(39);
		entity2.setEnd(56);
		entity2.setSpecificType("variation-location");
		entity2.addToIndexes();
		
		Entity entity3 = new Entity(jcas);
		entity3.setBegin(58);
		entity3.setEnd(106);
		entity3.setSpecificType("DNA");
		entity3.addToIndexes();
		
	} // of initCas
	
	
	
	public void testProcess() 
	{
		XMLInputSource descriptor = null; 
		ResourceSpecifier specifier = null;
		AnalysisEngine ae = null;
		
		try
		{
			descriptor = new XMLInputSource(TEST_DESC);
			specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(descriptor);
			ae = UIMAFramework.produceAnalysisEngine(specifier);
		} // of try
		catch (Exception e)
		{
			LOGGER.error("TEST PROCESS: " + e.getMessage());
		} // of catch

		
		JCas jcas = null;
		
		try
		{
			jcas = ae.newJCas();
		} // of try
		catch (ResourceInitializationException e)
		{
			LOGGER.error("TEST PROCESS: " + e.getMessage());
		} // of catch
		
		initCas(jcas);
		
		
		try
		{
			ae.process(jcas, null);			
			assertTrue("Invalid JCas!", checkJCas(jcas));
			
		} // of try 
		catch (Exception e)
		{
			LOGGER.error("TEST PROCESS: " + e.getMessage());
		} // of catch
		
	} // of testProcess

/*---------------------------------------------------------------------------*/
	private boolean checkJCas(JCas jcas) 
	{
		boolean valid = true;
		if (!checkDocumentText(jcas)) return false;
		if (!checkTokens(jcas)) return false;
		if (!checkEEEs(jcas)) return false;
		if (!checkEllipsis(jcas)) return false;		
		if (!(checkCoordinationElements(jcas))) return false;
		return valid;
	} // of checkJCas
/*---------------------------------------------------------------------------*/
	private boolean checkCoordinationElements(JCas jcas) 
	{
		boolean coordinationElementsValid = true;
		ArrayList<String> coordinationLabelsList = new ArrayList<String>();
		AnnotationIndex sentenceIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
		AnnotationIndex coordinationIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Coordination.type);
		AnnotationIndex coordinationElIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(CoordinationElement.type);
		FSIterator sentenceIterator = sentenceIndex.iterator();
		
		
		while(sentenceIterator.hasNext())
		{
			Sentence sentence = (Sentence) sentenceIterator.next();
			FSIterator coordinationIterator = coordinationIndex.subiterator(sentence);
			while (coordinationIterator.hasNext())
			{
				Coordination coordination = (Coordination) coordinationIterator.next();
				if (coordination.getElliptical())
				{
					String coordinationLabels = "";	
					FSIterator coordinationElIterator = coordinationElIndex.subiterator(coordination);
					while (coordinationElIterator.hasNext())
					{
						CoordinationElement coordinationElement = (CoordinationElement) coordinationElIterator.next();
						coordinationLabels = coordinationLabels + coordinationElement.getCat() + "," ;
					} // of while
					coordinationLabels = coordinationLabels.substring(0, coordinationLabels.length()-1);
					coordinationLabelsList.add(coordinationLabels);
				} // of if
			} // of while
			
			if (!checkCoordinationLabels(coordinationLabelsList.get(0), coordinationLabels1)) return false;
			if (!checkCoordinationLabels(coordinationLabelsList.get(1), coordinationLabels2)) return false;							 
		} // of while
		return coordinationElementsValid;
	} // checkCoordinationElements
/*---------------------------------------------------------------------------*/
	private boolean checkCoordinationLabels(String coordinationLabels, String coordinationLabels1) 
	{
		boolean coordinationLabelsValid = true;
		if (!coordinationLabels.equals(coordinationLabels1)) return false;
		return coordinationLabelsValid;
	} // of checkCoordinationLabels
/*---------------------------------------------------------------------------*/
	private boolean checkEllipsis(JCas jcas) 
	{
		boolean ellipsisValid = true;
		ArrayList<Coordination> coordinationArrayList = new ArrayList<Coordination>();
		AnnotationIndex sentenceIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
		AnnotationIndex coordinationIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Coordination.type);
		FSIterator sentenceIterator = sentenceIndex.iterator();
		
		while(sentenceIterator.hasNext())
		{
			Sentence sentence = (Sentence) sentenceIterator.next();
			FSIterator coordinationIterator = coordinationIndex.subiterator(sentence);
			while (coordinationIterator.hasNext())
			{
				 Coordination coordination = (Coordination) coordinationIterator.next();
				 if (coordination.getElliptical()) coordinationArrayList.add(coordination);
			} // of while
		} // of while
		
		if (!checkEllipsis(coordinationArrayList.get(0), ellipsis1)) return false;
		if (!checkEllipsis(coordinationArrayList.get(1), ellipsis2)) return false;
				
		return ellipsisValid;
	} // of checkEllipsis
/*---------------------------------------------------------------------------*/	
	private boolean checkEllipsis(Coordination coordination, String ellipsis) 
	{	
		boolean ellipsisValid = true;
		if (!coordination.getResolved().equals(ellipsis)) return false;
		return ellipsisValid;
	} // of checkEllipsis
	
/*---------------------------------------------------------------------------*/
	private boolean checkEEEs(JCas jcas) 
	{
		boolean eeeValid = true;
		AnnotationIndex EEEIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(EEE.type);
		AnnotationIndex sentenceIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);
		FSIterator sentenceIterator = sentenceIndex.iterator();
		ArrayList<EEE> eeeArrayList = new ArrayList<EEE>();
		
		while (sentenceIterator.hasNext())
		{
			Sentence sentence = (Sentence) sentenceIterator.next();
			FSIterator EEEIterator = EEEIndex.subiterator(sentence);
			while (EEEIterator.hasNext())
			{
				EEE eee = (EEE) EEEIterator.next(); 
				eeeArrayList.add(eee);
			} // of while
		} // of while
	
		if (!checkEEE(eeeArrayList.get(0), EEE1)) return false;
		if (!checkEEE(eeeArrayList.get(1), EEE2)) return false;
		
		return eeeValid;
	} // of testEEEs
/*---------------------------------------------------------------------------*/
	private boolean checkEEE(EEE eee, String coveredText)
	{
		boolean eeeValid = true; 
		if (!eee.getCoveredText().equals(coveredText)) return false;
		return eeeValid;
	} // of checkEEE
/*---------------------------------------------------------------------------*/
	private boolean checkTokens(JCas jcas) 
	{
		AnnotationIndex tokenIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Token.type);
		AnnotationIndex sentenceIndex = (AnnotationIndex) jcas.getJFSIndexRepository().getAnnotationIndex(Sentence.type);		
		FSIterator sentenceIterator = sentenceIndex.iterator();
		int tokenCounter = 0;
		
		while (sentenceIterator.hasNext())
		{
			Sentence sentence = (Sentence) sentenceIterator.next();
			FSIterator tokenIterator = tokenIndex.subiterator(sentence);
			while (tokenIterator.hasNext())
			{
				tokenIterator.next();
				tokenCounter++;
			} // of while
			
		} // of while
		return tokenCounter == tokenNumber;
	} // of checkTokens
/*---------------------------------------------------------------------------*/
	private boolean checkDocumentText(JCas jcas) 
	{
		String documentText = jcas.getDocumentText();
		return documentText.equals(text);	
	} // of checkDocumentText
/*---------------------------------------------------------------------------*/
} // of class CoordinationAnnotatorTest
 