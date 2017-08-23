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
package de.julielab.jcore.consumer.bionlp09event.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.julielab.jcore.consumer.bionlpformat.main.BioEventConsumer;
import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.ArgumentMention;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.EventTrigger;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.Header;

public class EventConsumerTest {
	private static final String EVENT_E8 = "E8	Phosphorylation:T14 Theme:T17 Site:T13";
	private static final String PROTEIN_T17 = "T17	Protein 428 433	STAT6";
	private static final String TRIGGER_T14 = "T14	Phosphorylation 331 346	phosphorylation";
	private static final String ENTITY_T13 = "T13	Entity 322 330	tyrosine";

	private static final String DOCUMENT_TITLE = "Interferons inhibit activation of STAT6 by interleukin 4 in human monocytes by inducing SOCS-1 gene expression. ";
	private static final String DOCUMENT_ABSTRACT = "Interferons (IFNs) inhibit induction by IL-4 of multiple genes in human monocytes. However, the mechanism by which IFNs mediate this inhibition has not been defined. IL-4 activates gene expression by inducing tyrosine phosphorylation, homodimerization, and nuclear translocation of the latent transcription factor, STAT6 (signal transducer and activator of transcription-6). STAT6-responsive elements are characteristically present in the promoters of IL-4-inducible genes. Because STAT6 activation is essential for IL-4-induced gene expression, we examined the ability of type I and type II IFNs to regulate activation of STAT6 by IL-4 in primary human monocytes. Pretreatment of monocytes with IFN-beta or IFN-gamma, but not IL-1, IL-2, macrophage colony-stimulating factor, granulocyte/macrophage colony-stimulating factor, IL-6, or transforming growth factor beta suppressed activation of STAT6 by IL-4. This inhibition was associated with decreased tyrosine phosphorylation and nuclear translocation of STAT6 and was not evident unless the cells were preincubated with IFN for at least 1 hr before IL-4 stimulation. Furthermore, inhibition by IFN could be blocked by cotreatment with actinomycin D and correlated temporally with induction of the JAK/STAT inhibitory gene, SOCS-1. Forced expression of SOCS-1 in a macrophage cell line, RAW264, markedly suppressed trans-activation of an IL-4-inducible reporter as well as IL-6- and IFN-gamma-induced reporter gene activity. These findings demonstrate that IFNs inhibit IL-4-induced activation of STAT6 and STAT6-dependent gene expression, at least in part, by inducing expression of SOCS-1.";
	private static final String TARGET_DIRECTORY = "src/test/resources/data";

	private JCas cas;

	private EventMention eventE8;
	private EventTrigger triggerT14;
	private Gene proteinT17;

	private EntityMention entityT13;
	private AnalysisEngine consumer;
	private FilenameFilter filter;

	@Before
	public void setUp() throws Exception {
		cas = JCasFactory.createJCas("src/test/resources/types/jcore-all-types");
		consumer = AnalysisEngineFactory.createEngine(BioEventConsumer.class,
				BioEventConsumer.DIRECTORY_PARAM, "src/test/resources/data",
				BioEventConsumer.BIOEVENT_SERVICE_MODE_PARAM, false);
		cas.setDocumentText(DOCUMENT_TITLE + "\n" + DOCUMENT_ABSTRACT);

		triggerT14 = new EventTrigger(cas);
		triggerT14.setId("T14");
		triggerT14.setSpecificType("Phosphorylation");
		triggerT14.setBegin(331);
		triggerT14.setEnd(346);

		proteinT17 = new Gene(cas);
		proteinT17.setId("T17");
		proteinT17.setBegin(428);
		proteinT17.setEnd(433);
		proteinT17.setSpecificType("protein");

		entityT13 = new EntityMention(cas);
		entityT13.setId("T13");
		entityT13.setBegin(322);
		entityT13.setEnd(330);

		eventE8 = new EventMention(cas);
		eventE8.setId("E8");
		eventE8.setTrigger(triggerT14);
		eventE8.setSpecificType("Phosphorylation");
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
		eventE8.addToIndexes();

		Title title = new Title(cas);
		title.setBegin(0);
		title.setEnd(DOCUMENT_TITLE.length());
		title.addToIndexes();

		AbstractText abstractText = new AbstractText(cas);
		abstractText.setBegin(DOCUMENT_TITLE.length() + 1);
		abstractText.setEnd(DOCUMENT_ABSTRACT.length()
				+ DOCUMENT_TITLE.length() + 1);
		abstractText.addToIndexes();

		Header header = new Header(cas);
		header.setDocId("123");
		header.addToIndexes();
		
		filter = new FilenameFilter() {

			public boolean accept(File file, String name) {
				return name.endsWith("a1") || name.endsWith("a2")
						|| name.endsWith("txt");
			}

		};
	}

	@After
	public void tearDown() {

		File dataDirectory = new File(TARGET_DIRECTORY);
		for (File file : dataDirectory.listFiles(filter))
			file.delete();

		dataDirectory.delete();
	}

	@Test
	public void testProcessCas() throws Exception {
		consumer.processCas(cas.getCas());

		boolean bioEventServiceMode = (Boolean) consumer
				.getConfigParameterValue(BioEventConsumer.BIOEVENT_SERVICE_MODE_PARAM);

		if (!bioEventServiceMode) {
			List<String> fileA1 = readFile(TARGET_DIRECTORY + "/" + "123.a1");
			assertEquals(1, fileA1.size());
			assertTrue(fileA1.contains(PROTEIN_T17));

			List<String> fileA2 = readFile(TARGET_DIRECTORY + "/" + "123.a2");
			assertEquals(3, fileA2.size());
			assertTrue(fileA2.contains(TRIGGER_T14));
			assertTrue(fileA2.contains(ENTITY_T13));
			assertTrue(fileA2.contains(EVENT_E8));

			List<String> fileTXT = readFile(TARGET_DIRECTORY + "/" + "123.txt");
			assertEquals(2, fileTXT.size());
			assertTrue(fileTXT.contains(DOCUMENT_TITLE));
			assertTrue(fileTXT.contains(DOCUMENT_ABSTRACT));
		} else {
			String TARGET_FILE = (String) consumer
					.getConfigParameterValue(BioEventConsumer.A2_FILE_PARAM);

			List<String> fileA2 = readFile(TARGET_FILE);

			assertTrue(fileA2.contains(TRIGGER_T14));
			assertTrue(fileA2.contains(EVENT_E8));

		}

	}

	public List<String> readFile(String fileName) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader(
				fileName));
		String line = null;
		while ((line = bufferedReader.readLine()) != null)
			lines.add(line);

		bufferedReader.close();
		return lines;
	}
}
