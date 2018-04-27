package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.consumer.xmi.XMIDBWriter;
import de.julielab.jcore.reader.db.DBMultiplierReader;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.sql.SQLException;

public class XmiDBSetupHelper {
    public static void processAndSplitData(String costosysConfig, String table, PostgreSQLContainer postgres) throws SQLException, UIMAException, IOException {
        CollectionReader pubmedXmlReader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.medline-db.desc.jcore-medline-db-reader",
                DBMultiplierReader.PARAM_TABLE, table,
                DBMultiplierReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                DBMultiplierReader.PARAM_RESET_TABLE, true);
        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jsbd.desc.jcore-jsbd-ae-biomedical-english");
        AnalysisEngine jtbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jtbd.desc.jcore-jtbd-ae-biomedical-english");
        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ADDITIONAL_TABLES, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), Header.class.getCanonicalName()}
                );
        JCas jCas = getJCasWithRequiredTypes();
        while (pubmedXmlReader.hasNext()) {
            pubmedXmlReader.getNext(jCas.getCas());
            jsbd.process(jCas);
            jtbd.process(jCas);
            xmiWriter.process(jCas);
            jCas.reset();
        }
        xmiWriter.collectionProcessComplete();
    }

    public static JCas getJCasWithRequiredTypes() throws UIMAException {
        return JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types",
                "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types",
                "de.julielab.jcore.types.jcore-xmi-splitter-types");
    }
}
