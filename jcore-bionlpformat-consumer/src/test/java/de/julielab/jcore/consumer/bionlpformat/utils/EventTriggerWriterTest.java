/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.consumer.bionlpformat.utils;

import de.julielab.jcore.types.EventTrigger;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Writer;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventTriggerWriterTest {
	private static final String TRIGGER_T1 = "T1	Negative_regulation 12 19	inhibit\n";
	private static final String DOCUMENT_TEXT = "Interferons inhibit activation of STAT6 by interleukin 4 in human monocytes by inducing SOCS-1 gene expression. \n" + 
												"Interferons (IFNs) inhibit induction by IL-4 of multiple genes in human monocytes. However, the mechanism by which IFNs mediate this inhibition has not been defined. IL-4 activates gene expression by inducing tyrosine phosphorylation, homodimerization, and nuclear translocation of the latent transcription factor, STAT6 (signal transducer and activator of transcription-6). STAT6-responsive elements are characteristically present in the promoters of IL-4-inducible genes. Because STAT6 activation is essential for IL-4-induced gene expression, we examined the ability of type I and type II IFNs to regulate activation of STAT6 by IL-4 in primary human monocytes. Pretreatment of monocytes with IFN-beta or IFN-gamma, but not IL-1, IL-2, macrophage colony-stimulating factor, granulocyte/macrophage colony-stimulating factor, IL-6, or transforming growth factor beta suppressed activation of STAT6 by IL-4. This inhibition was associated with decreased tyrosine phosphorylation and nuclear translocation of STAT6 and was not evident unless the cells were preincubated with IFN for at least 1 hr before IL-4 stimulation. Furthermore, inhibition by IFN could be blocked by cotreatment with actinomycin D and correlated temporally with induction of the JAK/STAT inhibitory gene, SOCS-1. Forced expression of SOCS-1 in a macrophage cell line, RAW264, markedly suppressed trans-activation of an IL-4-inducible reporter as well as IL-6- and IFN-gamma-induced reporter gene activity. These findings demonstrate that IFNs inhibit IL-4-induced activation of STAT6 and STAT6-dependent gene expression, at least in part, by inducing expression of SOCS-1.";
	private JCas cas;
	private EventTriggerWriter eventTriggerWriter;
	private Writer writer;
	private EventTrigger triggerT1;

	@BeforeEach
	public void setUp() throws Exception{
		cas = JCasFactory.createJCas("src/test/resources/types/jcore-semantics-biology-types");
		
		writer = createMock(Writer.class);
		eventTriggerWriter = new EventTriggerWriter(writer, DOCUMENT_TEXT);
		
		triggerT1 = new EventTrigger(cas);
		triggerT1.setBegin(12);
		triggerT1.setEnd(19);
		triggerT1.setId("T1");
		triggerT1.setSpecificType("Negative_regulation");
	}
	@Test
	public void testWriteTrigger() throws Exception{
		writer.write(TRIGGER_T1);
		replay(writer);
		
		eventTriggerWriter.writeTrigger(triggerT1);
		
		verify(writer);
	}
	
	@Test
	public void testIsWritten() throws Exception{
		writer.write(TRIGGER_T1);
		replay(writer);
		
		assertFalse(eventTriggerWriter.isWritten(triggerT1));
		eventTriggerWriter.writeTrigger(triggerT1);
		assertTrue(eventTriggerWriter.isWritten(triggerT1));
	}
	
	@Test
	public void testClose() throws IOException{
		writer.close();
		replay(writer);
		
		eventTriggerWriter.close();
		verify(writer);
	}
}
