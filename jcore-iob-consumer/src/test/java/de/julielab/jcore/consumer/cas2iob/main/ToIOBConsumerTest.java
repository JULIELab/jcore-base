/**
 * ToIOBConsumerTest.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: faessler
 * <p>
 * Current version: 1.0
 * Since version:   1.0
 * <p>
 * Creation date: 06.09.2007
 * <p>
 * Test of the ToIOBConsumer by employing the XMIToIOBApplication
 */

/**
 *
 */
package de.julielab.jcore.consumer.cas2iob.main;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.PennBioIEPOSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author faessler
 */
public class ToIOBConsumerTest {

    @Test
    public void testWriteIOB() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-semantics-biology-types"
                , "de.julielab.jcore.types.jcore-document-meta-types");
        jCas.setDocumentText("BRCA influences cancer.");

        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        new Gene(jCas, 0, 4).addToIndexes();
        new Token(jCas, 0, 4).addToIndexes();
        new Token(jCas, 5, 15).addToIndexes();
        new Token(jCas, 16, 22).addToIndexes();
        new Token(jCas, 22, 23).addToIndexes();

        final String outputDir = "src/test/resources/iob-output";
        final AnalysisEngine iobwriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.cas2iob.desc.jcore-iob-consumer",
                ToIOBConsumer.PARAM_OUTFOLDER, outputDir,
                ToIOBConsumer.PARAM_LABEL_METHODS, new String[]{"Gene"},
                ToIOBConsumer.PARAM_IOB_LABEL_NAMES, new String[]{"de.julielab.jcore.types.Gene=Gene"},
                ToIOBConsumer.PARAM_TYPE_PATH, "de.julielab.jcore.types.");
        iobwriter.process(jCas);
        final File file = Path.of(outputDir, "1.iob").toFile();
        assertThat(file.exists());
        assertThat(IOUtils.readLines(new FileInputStream(file), "UTF-8")).containsExactly("BRCA	B_Gene",
                "influences	O",
                "cancer	O",
                ".	O");
    }

    @Test
    public void testWriteIOBWithPos() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-semantics-biology-types"
                , "de.julielab.jcore.types.jcore-document-meta-types");
        jCas.setDocumentText("BRCA influences cancer.");

        new Sentence(jCas, 0, jCas.getDocumentText().length()).addToIndexes();
        new Gene(jCas, 0, 4).addToIndexes();
        final Token t1 = new Token(jCas, 0, 4);
        t1.addToIndexes();
        final Token t2 = new Token(jCas, 5, 15);
        t2.addToIndexes();
        final Token t3 = new Token(jCas, 16, 22);
        t3.addToIndexes();
        final Token t4 = new Token(jCas, 22, 23);
        t4.addToIndexes();
        PennBioIEPOSTag tag = new PennBioIEPOSTag(jCas, 0, 4);
        tag.setValue("NN");
        t1.setPosTag(JCoReTools.addToFSArray(null, tag));
        tag = new PennBioIEPOSTag(jCas, 5, 15);
        tag.setValue("VBZ");
        t2.setPosTag(JCoReTools.addToFSArray(null, tag));
        tag = new PennBioIEPOSTag(jCas, 16, 22);
        tag.setValue("NN");
        t3.setPosTag(JCoReTools.addToFSArray(null, tag));
        tag = new PennBioIEPOSTag(jCas, 22, 23);
        tag.setValue(".");
        t4.setPosTag(JCoReTools.addToFSArray(null, tag));

        final String outputDir = "src/test/resources/iob-output";
        final AnalysisEngine iobwriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.cas2iob.desc.jcore-iob-consumer",
                ToIOBConsumer.PARAM_OUTFOLDER, outputDir,
                ToIOBConsumer.PARAM_LABEL_METHODS, new String[]{"Gene"},
                ToIOBConsumer.PARAM_IOB_LABEL_NAMES, new String[]{"de.julielab.jcore.types.Gene=Gene"},
                ToIOBConsumer.PARAM_TYPE_PATH, "de.julielab.jcore.types.",
                ToIOBConsumer.PARAM_ADD_POS, true,
                ToIOBConsumer.PARAM_IOB_MARK_SEPARATOR, "-");
        iobwriter.process(jCas);
        final File file = Path.of(outputDir, "1.iob").toFile();
        assertThat(file.exists());
        assertThat(IOUtils.readLines(new FileInputStream(file), "UTF-8")).containsExactly("BRCA	NN	B-Gene",
                "influences	VBZ	O",
                "cancer	NN	O",
                ".	.	O");

    }
}
