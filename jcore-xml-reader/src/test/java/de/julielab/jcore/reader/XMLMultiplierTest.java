package de.julielab.jcore.reader;

import de.julielab.jcore.multiplier.xml.XMLMultiplier;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XMLMultiplierTest {

    @Test
    public void testNext() throws UIMAException {
        JCas jCas = JCasFactory.createJCas(
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types",
                "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.casmultiplier.jcore-uri-multiplier-types");

        AnalysisEngine multiplier = AnalysisEngineFactory.createEngine(XMLMultiplier.class,
                XMLMultiplier.PARAM_HEADER_TYPE, "de.julielab.jcore.types.pubmed.Header",
                XMLMultiplier.PARAM_FOR_EACH, "/PubmedArticleSet/PubmedArticle",
                XMLMultiplier.PARAM_MAPPING_FILE, "src/test/resources/medlineMappingFile.xml");
        JCoReURI uri = new JCoReURI(jCas);
        uri.setUri(new File("src/test/resources/pubmedXML/pubmedsample18n0001.xml.gz").toURI().toString());
        uri.addToIndexes();

        JCasIterator jCasIterator = multiplier.processAndOutputNewCASes(jCas);
        assertTrue(jCasIterator.hasNext());
        JCas newcas = jCasIterator.next();
        Title title = JCasUtil.selectSingle(newcas, Title.class);
        assertEquals("Hospital debt management and cost reimbursement.", title.getCoveredText());
        int i = 1;
        newcas.release();
        while (jCasIterator.hasNext()) {
            jCasIterator.next().release();
            i++;
        }
        assertEquals(177, i);
    }
}
