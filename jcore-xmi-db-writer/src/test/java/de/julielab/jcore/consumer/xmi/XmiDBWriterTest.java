package de.julielab.jcore.consumer.xmi;

import de.julielab.jcore.db.test.DBTestUtils;
import de.julielab.jcore.reader.db.DBMultiplierReader;
import de.julielab.jcore.types.*;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.*;
public class XmiDBWriterTest {
    @ClassRule
    public static PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer();
    private static String costosysConfig;
    private static String xmlSubsetTable;
    private static DataBaseConnector dbc;

    @BeforeClass
    public static void setup() throws SQLException, UIMAException, IOException, ConfigurationException {
        dbc = DBTestUtils.getDataBaseConnector(postgres);
        costosysConfig = DBTestUtils.createTestCostosysConfig("medline_2017", 1, postgres);
        xmlSubsetTable = DBTestUtils.setupDatabase(dbc, "src/test/resources/pubmedsample18n0001.xml.gz", "medline_2017", 177, postgres);
    }

    @AfterClass
    public static void shutDown(){
        dbc.close();
    }

    @Test
    public void testXmiDBWriterSplitAnnotations() throws UIMAException, IOException {
        CollectionReader pubmedXmlReader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.medline-db.desc.jcore-medline-db-reader",
                DBMultiplierReader.PARAM_TABLE, xmlSubsetTable,
                DBMultiplierReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                DBMultiplierReader.PARAM_RESET_TABLE, true);
        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jsbd.desc.jcore-jsbd-ae-biomedical-english");
        AnalysisEngine jtbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jtbd.desc.jcore-jtbd-ae-biomedical-english");
        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_ANNOS_TO_STORE, new String[]{Token.class.getCanonicalName(), Sentence.class.getCanonicalName()},
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, false,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.documents",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()}
        );
        JCas jCas = getJCasWithRequiredTypes();
        while (pubmedXmlReader.hasNext()) {
            pubmedXmlReader.getNext(jCas.getCas());
            jsbd.process(jCas);
            jtbd.process(jCas);
            assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
            jCas.reset();
        }
        xmiWriter.collectionProcessComplete();

        assertThat(dbc.tableExists("_data.documents")).isTrue();
        assertThat(dbc.tableExists("_data.de_julielab_jcore_types_token")).isTrue();
        assertThat(dbc.tableExists("_data.de_julielab_jcore_types_sentence")).isTrue();
    }

    @Test
    public void testXmiDBWriterWholeXMI() throws UIMAException, IOException {
        CollectionReader pubmedXmlReader = CollectionReaderFactory.createReader("de.julielab.jcore.reader.medline-db.desc.jcore-medline-db-reader",
                DBMultiplierReader.PARAM_TABLE, xmlSubsetTable,
                DBMultiplierReader.PARAM_COSTOSYS_CONFIG_NAME, costosysConfig,
                DBMultiplierReader.PARAM_RESET_TABLE, true);
        AnalysisEngine jsbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jsbd.desc.jcore-jsbd-ae-biomedical-english");
        AnalysisEngine jtbd = AnalysisEngineFactory.createEngine("de.julielab.jcore.ae.jtbd.desc.jcore-jtbd-ae-biomedical-english");
        AnalysisEngine xmiWriter = AnalysisEngineFactory.createEngine("de.julielab.jcore.consumer.xmi.desc.jcore-xmi-db-writer",
                XMIDBWriter.PARAM_COSTOSYS_CONFIG, costosysConfig,
                XMIDBWriter.PARAM_STORE_ALL, true,
                XMIDBWriter.PARAM_STORE_BASE_DOCUMENT, true,
                XMIDBWriter.PARAM_TABLE_DOCUMENT, "_data.xmidocs",
                XMIDBWriter.PARAM_DO_GZIP, false,
                XMIDBWriter.PARAM_STORE_RECURSIVELY, true,
                XMIDBWriter.PARAM_UPDATE_MODE, true,
                // this parameter makes no sense, it should just be ignored
                XMIDBWriter.PARAM_BASE_DOCUMENT_ANNOTATION_TYPES, new String[]{MeshHeading.class.getCanonicalName(), AbstractText.class.getCanonicalName(), Title.class.getCanonicalName(), de.julielab.jcore.types.pubmed.Header.class.getCanonicalName()}
        );
        JCas jCas = getJCasWithRequiredTypes();
        while (pubmedXmlReader.hasNext()) {
            pubmedXmlReader.getNext(jCas.getCas());
            jsbd.process(jCas);
            jtbd.process(jCas);
            assertThatCode(() -> xmiWriter.process(jCas)).doesNotThrowAnyException();
            jCas.reset();
        }
        xmiWriter.collectionProcessComplete();

        assertThat(dbc.tableExists("_data.xmidocs")).isTrue();
        assertThat(dbc.getNumRows("_data.xmidocs")).isEqualTo(177);
    }

    public static JCas getJCasWithRequiredTypes() throws UIMAException {
        return JCasFactory.createJCas("de.julielab.jcore.types.jcore-morpho-syntax-types",
                "de.julielab.jcore.types.jcore-document-meta-pubmed-types",
                "de.julielab.jcore.types.jcore-document-structure-pubmed-types",
                "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types",
                "de.julielab.jcore.types.jcore-xmi-splitter-types");
    }
}
