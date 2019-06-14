
package de.julielab.jcore.ae.flairner;

import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Unit tests for jcore-flair-ner-ae.
 *
 */
public class FlairNerAnnotatorTest{
    @Test
    public void testAnnotator() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(FlairNerAnnotator.class, FlairNerAnnotator.PARAM_ANNOTATION_TYPE, Gene.class.getCanonicalName(), FlairNerAnnotator.PARAM_FLAIR_MODEL, "src/test/resources/genes-small-model.pt");
        String text = "Knockdown of SUB1 homolog by siRNA inhibits the early stages of HIV-1 replication in 293T cells infected with VSV-G pseudotyped HIV-1 .";
        jCas.setDocumentText(text);
        Sentence s = new Sentence(jCas, 0, text.length());
        addTokens(jCas);
        s.addToIndexes();
        engine.process(jCas);
        List<String> foundGenes = new ArrayList<>();
        for (Annotation a : jCas.getAnnotationIndex(Gene.type)) {
            Gene g = (Gene) a;
            foundGenes.add(g.getCoveredText());
            assertThat(g.getSpecificType().equals("Gene"));
        }
        assertThat(foundGenes).containsExactly("SUB1 homolog", "HIV-1", "VSV-G", "HIV-1");
        engine.collectionProcessComplete();
    }

    private void addTokens(JCas jCas) {
        final String documentText = jCas.getDocumentText();
        Matcher m = Pattern.compile("[^ ]+").matcher(documentText);
        while (m.find()) {
            new Token(jCas, m.start(), m.end()).addToIndexes();
        }
    }

    private void addSentences(JCas jCas) {
        final String documentText = jCas.getDocumentText();
        Matcher m = Pattern.compile("[^\n]+").matcher(documentText);
        while (m.find()) {
            new Sentence(jCas, m.start(), m.end()).addToIndexes();
        }
    }

    @Test
    public void testAnnotator2() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(FlairNerAnnotator.class, FlairNerAnnotator.PARAM_ANNOTATION_TYPE, Gene.class.getCanonicalName(), FlairNerAnnotator.PARAM_FLAIR_MODEL, "src/test/resources/genes-small-model.pt");
        // The sentence detection and tokenization was done by the jcore-j[st]bd-biomedical-english JCoRe project components, using the executable (java -jar) command line artifact created when building the components.
        String text = "Synergistic lethal effect between hydrogen peroxide and neocuproine ( 2,9-dimethyl 1,10-phenanthroline ) in Escherichia coli .\n" +
                "Despite 2,9-dimethyl 1,10-phenanthroline ( NC ) has been extensively used as a potential inhibitor of damage due to oxidative stress in biological systems , the incubation of E. coli cultures with the copper ion chelator NC prior to the challenge with hydrogen peroxide caused a lethal synergistic effect. \n" +
                "The SOS response seems to be involved in the repair of the synergistic lesions through the recombination pathway. \n" +
                "Furthermore , there is evidence for the UvrABC excinuclease participation in the repair of the synergistic lesions , and the base excision repair may also be required for bacterial survival to the synergistic effect mainly at high concentrations of H2O2 , being the action of Fpg protein an important event. \n" +
                "Incubation of lexA ( Ind - ) cultures with iron ( II ) ion chelator 2,2' - dipyridyl simultaneously with NC prevented the lethal synergistic effect. \n" +
                "This result suggests an important role of the Fenton reaction on the phenomenon. \n" +
                "NC treatment was able to increase the number of DNA strand breaks ( DNAsb ) induced by 10 mM of H2O2 in lexA ( Ind - ) strain and the simultaneous treatment with 2,2'-dipyridyl was able to block this effect .";
        jCas.setDocumentText(text);
        addSentences(jCas);
        addTokens(jCas);

        engine.process(jCas);
        List<String> foundGenes = new ArrayList<>();
        for (Annotation a : jCas.getAnnotationIndex(Gene.type)) {
            Gene g = (Gene) a;
            foundGenes.add(g.getCoveredText());
            assertThat(g.getSpecificType().equals("Gene"));
        }
        assertThat(foundGenes).containsExactly("copper ion chelator NC", "UvrABC excinuclease", "H2O2", "Fpg protein", "lexA", "H2O2", "lexA");
        engine.collectionProcessComplete();
    }

    @Test
    public void testAnnotatorOnOffsetIsseDocument() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types", "de.julielab.jcore.types.jcore-document-meta-pubmed-types", "de.julielab.jcore.types.extensions.jcore-document-meta-extension-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(FlairNerAnnotator.class, FlairNerAnnotator.PARAM_ANNOTATION_TYPE, Gene.class.getCanonicalName(), FlairNerAnnotator.PARAM_FLAIR_MODEL, "src/test/resources/genes-small-model.pt");

        XmiCasDeserializer.deserialize(new FileInputStream(Path.of("src", "test", "resources", "1681975.xmi").toString()), jCas.getCas());

        engine.process(jCas);
        List<String> foundGenes = new ArrayList<>();
        for (Annotation a : jCas.getAnnotationIndex(Gene.type)) {
            Gene g = (Gene) a;
            foundGenes.add(g.getCoveredText());
            assertThat(g.getSpecificType().equals("Gene"));
        }
        engine.collectionProcessComplete();
    }


}
