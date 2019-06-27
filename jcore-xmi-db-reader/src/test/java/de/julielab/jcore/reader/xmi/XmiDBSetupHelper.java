package de.julielab.jcore.reader.xmi;

import de.julielab.costosys.Constants;
import de.julielab.jcore.consumer.xmi.XMIDBWriter;
import de.julielab.jcore.types.*;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class XmiDBSetupHelper {
    public static void processAndSplitData(String costosysConfig, boolean gzip, String nsSchema) throws UIMAException, IOException {
        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, gzip,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), Header.class.getCanonicalName()},
                XMIDBWriter.PARAM_XMI_META_SCHEMA, nsSchema
        );
        JCas jCas = getJCasWithRequiredTypes();
        jCas.setDocumentText("This is a sentence. This is another one.");
        Header header = new Header(jCas);
        header.setDocId("12345");
        header.addToIndexes();
        new Sentence(jCas, 0, 19).addToIndexes();
        new Sentence(jCas, 20, 40).addToIndexes();
        // Of course, these token offsets are wrong, but it doesn't matter to the test
        new Token(jCas, 0, 19).addToIndexes();
        new Token(jCas, 20, 40).addToIndexes();

        xmiWriter.process(jCas);
        jCas.reset();
        xmiWriter.collectionProcessComplete();
    }

    public static void createDbcConfig(PostgreSQLContainer postgres) {
        String hiddenConfigPath = "src/test/resources/hiddenConfig.txt";
        try (BufferedWriter w = new BufferedWriter(new FileWriter(hiddenConfigPath))) {
            w.write(postgres.getDatabaseName());
            w.newLine();
            w.write(postgres.getUsername());
            w.newLine();
            w.write(postgres.getPassword());
            w.newLine();
            w.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.setProperty(Constants.HIDDEN_CONFIG_PATH, hiddenConfigPath);
    }

    public static void processAndStoreCompleteXMIData(String costosysConfig, boolean gzip) throws UIMAException, IOException {
        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[0],
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, true,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, false,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, gzip,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, false,
                XMIDBWriter.PARAM_UPDATE_MODE, true
        );
        JCas jCas = getJCasWithRequiredTypes();
        jCas.setDocumentText("This is a sentence. This is another one.");
        Header header = new Header(jCas);
        header.setDocId("12345");
        header.addToIndexes();
        new Sentence(jCas, 0, 19).addToIndexes();
        new Sentence(jCas, 20, 40).addToIndexes();
        // Of course, these token offsets are wrong, but it doesn't matter to the test
        new Token(jCas, 0, 19).addToIndexes();
        new Token(jCas, 20, 40).addToIndexes();

        xmiWriter.process(jCas);
        jCas.reset();
        xmiWriter.collectionProcessComplete();
    }

    public static JCas getJCasWithRequiredTypes() throws UIMAException {
        return JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types",
                "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types",
                "de.julielab.jcore.types.jcore-xmi-splitter-types",
                "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types");
    }
}
