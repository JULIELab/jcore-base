package de.julielab.jcore.multiplier.pmc;

import de.julielab.jcore.reader.db.DBMultiplierReader;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * This is not as much a test as it is a facility to check error cases in isolation. The existing code
 * reads from an XML database table and parses the PMC document from there
 */
@Disabled
public class ErrorTest {

    @Test
    public void errorTest() throws Exception {
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.jcore-document-structure-pubmed-types", "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types");
        CollectionReader reader = CollectionReaderFactory.createReader(DBMultiplierReader.class, DBMultiplierReader.PARAM_COSTOSYS_CONFIG_NAME, Path.of("src", "test", "resources", "costosys-errortest.xml").toString(), DBMultiplierReader.PARAM_TABLE, "_data.errordoc", DBMultiplierReader.PARAM_RESET_TABLE, true);
        AnalysisEngine engine = AnalysisEngineFactory.createEngine(PMCDBMultiplier.class, PMCDBMultiplier.PARAM_OMIT_BIB_REFERENCES, true);
        while (reader.hasNext()) {
            reader.getNext(jCas.getCas());
            JCasIterator jCasIterator = engine.processAndOutputNewCASes(jCas);
            while (jCasIterator.hasNext()) {
                JCas next = jCasIterator.next();
                System.out.println(JCoReTools.getDocId(next));
                next.release();
            }
        }
    }
}
