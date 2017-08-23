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
package de.julielab.jcore.ae.ec;

import static org.junit.Assert.*;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.julielab.jcore.ae.ecn.ECNumberAnnotator;
import de.julielab.jcore.types.Enzyme;

public class ECNumberAnnotatorTest {
	@Test
	public void simpleTest() throws UIMAException {
		// This test delivers a brief text with one EC number. We expect a
		// single Enzyme annotation for this number with the appropriate values
		// set.
		JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
		jcas.setDocumentText("Acetylesterase has number EC 3.1.1.6");
		AnalysisEngine ae = AnalysisEngineFactory.createEngine(ECNumberAnnotator.class);
		ae.process(jcas);
		Enzyme enzyme = JCasUtil.selectSingle(jcas, Enzyme.class);
		assertEquals("3.1.1.6", enzyme.getSpecificType());
		assertEquals(26, enzyme.getBegin());
		assertEquals(36, enzyme.getEnd());
	}
}
