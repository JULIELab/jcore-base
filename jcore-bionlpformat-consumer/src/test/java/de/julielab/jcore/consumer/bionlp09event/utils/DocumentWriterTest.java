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
package de.julielab.jcore.consumer.bionlp09event.utils;

import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import java.io.IOException;
import java.io.Writer;

import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;
import org.apache.uima.fit.factory.JCasFactory;

import de.julielab.jcore.consumer.bionlpformat.utils.DocumentWriter;
import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Title;

public class DocumentWriterTest {

	
	private static final String DOCUMENT_TITLE = "Interferons inhibit activation of STAT6 by interleukin 4 in human monocytes by inducing SOCS-1 gene expression. "; 
	private static final String DOCUMENT_ABSTRACT = "Interferons (IFNs) inhibit induction by IL-4 of multiple genes in human monocytes. However, the mechanism by which IFNs mediate this inhibition has not been defined. IL-4 activates gene expression by inducing tyrosine phosphorylation, homodimerization, and nuclear translocation of the latent transcription factor, STAT6 (signal transducer and activator of transcription-6). STAT6-responsive elements are characteristically present in the promoters of IL-4-inducible genes. Because STAT6 activation is essential for IL-4-induced gene expression, we examined the ability of type I and type II IFNs to regulate activation of STAT6 by IL-4 in primary human monocytes. Pretreatment of monocytes with IFN-beta or IFN-gamma, but not IL-1, IL-2, macrophage colony-stimulating factor, granulocyte/macrophage colony-stimulating factor, IL-6, or transforming growth factor beta suppressed activation of STAT6 by IL-4. This inhibition was associated with decreased tyrosine phosphorylation and nuclear translocation of STAT6 and was not evident unless the cells were preincubated with IFN for at least 1 hr before IL-4 stimulation. Furthermore, inhibition by IFN could be blocked by cotreatment with actinomycin D and correlated temporally with induction of the JAK/STAT inhibitory gene, SOCS-1. Forced expression of SOCS-1 in a macrophage cell line, RAW264, markedly suppressed trans-activation of an IL-4-inducible reporter as well as IL-6- and IFN-gamma-induced reporter gene activity. These findings demonstrate that IFNs inhibit IL-4-induced activation of STAT6 and STAT6-dependent gene expression, at least in part, by inducing expression of SOCS-1.";
	
	private JCas cas;
	private DocumentWriter documentWriter;
	private Writer writer;

	@Before
	public void setUp() throws Exception{
		cas = JCasFactory.createJCas("src/test/resources/types/jcore-all-types");
		cas.setDocumentText(DOCUMENT_TITLE + "\n" + DOCUMENT_ABSTRACT);
		
		Title title = new Title(cas);
		title.setBegin(0);
		title.setEnd(DOCUMENT_TITLE.length());
		title.addToIndexes();
		
		AbstractText abstractText = new AbstractText(cas);
		abstractText.setBegin(DOCUMENT_TITLE.length()+1);
		abstractText.setEnd(DOCUMENT_ABSTRACT.length() + 
				            DOCUMENT_TITLE.length() + 1);
		abstractText.addToIndexes();
		
		writer = createMock(Writer.class);
		documentWriter = new DocumentWriter(writer);				
	}
	
	@Test
	public void testWriteDocument() throws IOException{
//		writer.write(DOCUMENT_TITLE+"\n");
//		writer.write(DOCUMENT_ABSTRACT+"\n");
		writer.write(DOCUMENT_TITLE + "\n" + DOCUMENT_ABSTRACT + "\n");
		replay(writer);
		
		documentWriter.write(cas);
		verify(writer);
	}
	
	@Test
	public void testClose() throws IOException{
		writer.close();
		replay(writer);
		
		documentWriter.close();
		verify(writer);
	}
	
}
