package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.ext.DBProcessingMetaData;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class DebugTest {
    @Test
    public void test() throws Exception {
        CollectionReader xmiReader = CollectionReaderFactory.createReader(XmiDBMultiplierReader.class,
                XmiDBReader.PARAM_COSTOSYS_CONFIG_NAME, "/Users/faessler/bin/cstsys-pubmed2019/costosys.xml",
                XmiDBReader.PARAM_READS_BASE_DOCUMENT, true,
                XmiDBReader.PARAM_TABLE, "pubmed.errordoc",
                XmiDBReader.PARAM_RESET_TABLE, true
        );
        final AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(XmiDBMultiplier.class);
        JCas jCas = XmiDBSetupHelper.getJCasWithRequiredTypes();
        assertTrue(xmiReader.hasNext());
        if (xmiReader.hasNext()) {
            xmiReader.getNext(jCas.getCas());
            final JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
            assertTrue(jCasIterator.hasNext());
            if (jCasIterator.hasNext()) {
                final JCas next = jCasIterator.next();
                System.out.println(next.getDocumentText());
                jCasIterator.release();
            }
            jCas.reset();
        }
    }
}
