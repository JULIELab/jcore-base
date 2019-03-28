
package de.julielab.jcore.consumer.ppd;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for jcore-ppd-writer.
 *
 */
public class PPDWriterTest{
    @Test
    public void testProcess() throws Exception {
        File correctOutput = new File("src/test/resources/testoutput-correct.ppd");
        String outputFileString = "src/test/resources/testoutput.ppd";
        File outputFile = new File(outputFileString);
        if (outputFile.exists())
            outputFile.delete();
        AnalysisEngine ppdConsumer = AnalysisEngineFactory.createEngine(PPDWriter.class,
                PPDWriter.PARAM_TYPE_LABEL_MAPPINGS, new String[] { "de.julielab.jcore.types.Gene=GENE" },
                PPDWriter.PARAM_META_DATA_TYPE_MAPPINGS,
                new String[] { "de.julielab.jcore.types.PennBioIEPOSTag=/value" }, PPDWriter.PARAM_OUTSIDE_LABEL,
                "O", PPDWriter.PARAM_OUTPUT_FILE, outputFileString);
        JCas jcas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types");
        XmiCasDeserializer.deserialize(new FileInputStream("src/test/resources/10438902.xmi"), jcas.getCas());
        ppdConsumer.process(jcas.getCas());
        ppdConsumer.collectionProcessComplete();
        assertTrue(outputFile.exists());
        assertEquals(FileUtils.readFileToString(correctOutput, "UTF-8"),
                FileUtils.readFileToString(outputFile, "UTF-8"));
        outputFile.delete();
    }
}
