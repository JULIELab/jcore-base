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
package de.julielab.jcore.ae.jnet.uima;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.io.Files;

import de.julielab.jcore.types.Sentence;

public class MiniTestapp {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityAnnotatorTest.class);
    private static final String PREFIX = "src/test/resources/de/julielab/jcore/ae/jnet/uima/";

    private static final String TEST_XMI_IN = PREFIX + "17088488.xmi";

    private static final String TEST_XMI_OUT = PREFIX + "miniapp_out.xmi";
    private static final String TEST_XMI_OUT_TEMPLATE = PREFIX + "miniapp_out_template.xmi";

    private static final String ANNOTATOR_DESC = PREFIX + "EntityAnnotatorTest.xml";

    @After
    public void clean() {
        if (new File(TEST_XMI_OUT).isFile()) {
            new File(TEST_XMI_OUT).delete();
        }
    }

    /**
     * Was a main method, made a dummy test out of it
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception {

        // reading XMI file
        final File filename = new File(TEST_XMI_IN);

        final CAS cas = CasCreationUtils.createCas(
                UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(ANNOTATOR_DESC)));
        initCas(cas, filename);

        final JCas aJCas = cas.getJCas();

        final JFSIndexRepository indexes = aJCas.getJFSIndexRepository();
        indexes.getAnnotationIndex(Sentence.type).iterator();

        // producing entity-ae

        AnalysisEngine entityAE;
        ResourceSpecifier spec;

        spec = UIMAFramework.getXMLParser().parseResourceSpecifier(new XMLInputSource(ANNOTATOR_DESC));
        entityAE = UIMAFramework.produceAnalysisEngine(spec);

        // process document
        entityAE.process(cas);

        // write results to new XMI
        if (new File(TEST_XMI_OUT).isFile()) {
            new File(TEST_XMI_OUT).delete();
        }
        writeCasToXMI(cas, TEST_XMI_OUT);

        String output = Files.toString(new File(TEST_XMI_OUT), Charset.forName("UTF-8"));
        String template = Files.toString(new File(TEST_XMI_OUT_TEMPLATE), Charset.forName("UTF-8"));

        assertEquals(template, output);
    }

    /**
     * @param cas
     */
    private static void initCas(final CAS cas, final File filename) throws Exception {
        LOGGER.info("Reading test document");
        final FileInputStream fis = new FileInputStream(filename.getAbsolutePath());
        XmiCasDeserializer.deserialize(fis, cas);
    }

    /**
     * writes produced annotations to CAS
     */
    private static void writeCasToXMI(final CAS cas, final String filename)
            throws CASException, IOException, SAXException {
        // now write CAS
        final FileOutputStream fos = new FileOutputStream(filename);
        final XmiCasSerializer ser = new XmiCasSerializer(cas.getTypeSystem());
        final XMLSerializer xmlSer = new XMLSerializer(fos, false);
        ser.serialize(cas, xmlSer.getContentHandler());
    }

}
