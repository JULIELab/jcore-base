package de.julielab.jcore.ae.flairner;

import de.julielab.jcore.types.EmbeddingVector;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.index.Comparators;
import de.julielab.jcore.utility.index.JCoReTreeMapAnnotationIndex;
import de.julielab.jcore.utility.index.TermGenerators;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.assertj.core.data.Offset;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for jcore-flair-ner-ae.
 */
public class FlairNerAnnotatorTest {
    /**
     * This field is a global state to carry some information between two tests. This should allow to show
     * that for the case that FLAIR creates subtokens (due to whitespace token splitting) to our UIMA tokens,
     * the original embeddings get averaged. For this, the first test writes the original embeddings into this field
     * and the second test can retrieve them for comparison.
     */
    private List<double[]> embeddingsCache = new ArrayList<>();

    @Test
    public void testAnnotatorWithoutWordEmbeddings() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(FlairNerAnnotator.class, FlairNerAnnotator.PARAM_ANNOTATION_TYPE, Gene.class.getCanonicalName(), FlairNerAnnotator.PARAM_FLAIR_MODEL, "src/test/resources/genes-small-model.pt");
        String text = "Knockdown of SUB1 homolog by siRNA inhibits the early stages of HIV-1 replication in 293T cells infected with VSV-G pseudotyped HIV-1 .";
        jCas.setDocumentText(text);
        Sentence s = new Sentence(jCas, 0, text.length());
        addTokens(jCas);
        s.addToIndexes();
        engine.process(jCas);
        List<String> foundGenes = new ArrayList<>();
        JCoReTreeMapAnnotationIndex<Long, Token> tokenIndex = new JCoReTreeMapAnnotationIndex<>(TermGenerators.longOffsetTermGenerator(), TermGenerators.longOffsetTermGenerator(), jCas, Token.type);
        for (Annotation a : jCas.getAnnotationIndex(Gene.type)) {
            Gene g = (Gene) a;
            foundGenes.add(g.getCoveredText());
            assertThat(g.getSpecificType().equals("Gene"));
            final Iterator<Token> tokenIt = tokenIndex.searchFuzzy(g).iterator();
            while (tokenIt.hasNext()) {
                Token token = tokenIt.next();
                assertThat(token.getEmbeddingVectors()).isNull();
            }
        }
        assertThat(foundGenes).containsExactly("SUB1 homolog", "HIV-1", "VSV-G", "HIV-1");
        engine.collectionProcessComplete();
    }

    @Test
    public void testAnnotatorWithEntityWordEmbeddings() throws Exception {
        embeddingsCache.clear();
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(FlairNerAnnotator.class, FlairNerAnnotator.PARAM_STORE_EMBEDDINGS, FlairNerAnnotator.StoreEmbeddings.ENTITIES, FlairNerAnnotator.PARAM_ANNOTATION_TYPE, Gene.class.getCanonicalName(), FlairNerAnnotator.PARAM_FLAIR_MODEL, "src/test/resources/genes-small-model.pt");
        String text = "Knockdown of SUB1 homolog by siRNA inhibits the early stages of HIV-1 replication in 293T cells infected with VSV-G pseudotyped HIV-1 .";
        jCas.setDocumentText(text);
        Sentence s = new Sentence(jCas, 0, text.length());
        addTokens(jCas);
        s.addToIndexes();
        engine.process(jCas);
        List<String> foundGenes = new ArrayList<>();
        JCoReTreeMapAnnotationIndex<Long, Token> tokenIndex = new JCoReTreeMapAnnotationIndex<>(Comparators.longOverlapComparator(), TermGenerators.longOffsetTermGenerator(), TermGenerators.longOffsetTermGenerator(), jCas, Token.type);
        for (Annotation a : jCas.getAnnotationIndex(Gene.type)) {
            Gene g = (Gene) a;
            foundGenes.add(g.getCoveredText());
            assertThat(g.getSpecificType().equals("Gene"));
            final Iterator<Token> tokenIt = tokenIndex.searchFuzzy(g).iterator();
            while (tokenIt.hasNext()) {
                Token token = tokenIt.next();
                assertThat(token.getEmbeddingVectors()).isNotNull();
                assertThat(token.getEmbeddingVectors()).hasSize(1);
                final EmbeddingVector embedding = (EmbeddingVector) token.getEmbeddingVectors().get(0);
                assertThat(embedding.getSource()).isEqualTo("src/test/resources/genes-small-model.pt");
                assertThat(embedding.getVector()).hasSize(1024);
                embeddingsCache.add(embedding.getVector().toArray());
            }
        }
        JCoReTreeMapAnnotationIndex<Long, Token> geneIndex = new JCoReTreeMapAnnotationIndex<>(Comparators.longOverlapComparator(), TermGenerators.longOffsetTermGenerator(), TermGenerators.longOffsetTermGenerator(), jCas, Gene.type);
        for (Token t : jCas.<Token>getAnnotationIndex(Token.type)) {
            final Optional<Token> any = geneIndex.searchFuzzy(t).findAny();
            if (!any.isPresent()) {
                assertThat(t.getEmbeddingVectors()).isNull();
            }
        }
        assertThat(foundGenes).containsExactly("SUB1 homolog", "HIV-1", "VSV-G", "HIV-1");
        engine.collectionProcessComplete();
    }

    @Test(dependsOnMethods = "testAnnotatorWithEntityWordEmbeddings")
    public void testAnnotatorWithEntitySubWordEmbeddings() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(FlairNerAnnotator.class, FlairNerAnnotator.PARAM_STORE_EMBEDDINGS, FlairNerAnnotator.StoreEmbeddings.ENTITIES, FlairNerAnnotator.PARAM_ANNOTATION_TYPE, Gene.class.getCanonicalName(), FlairNerAnnotator.PARAM_FLAIR_MODEL, "src/test/resources/genes-small-model.pt");
        String text = "Knockdown of SUB1 homolog by siRNA inhibits the early stages of HIV-1 replication in 293T cells infected with VSV-G pseudotyped HIV-1 .";
        jCas.setDocumentText(text);
        Sentence s = new Sentence(jCas, 0, text.length());
        addTokens(jCas);
        // We now manipulate the tokenization to have "SUB1 homolog" appear as one UIMA token. FLAIR will interpret
        // the two words still as two tokens. We want to check if this case in handles correctly.
        final List<Token> tokens = new ArrayList<>(JCasUtil.select(jCas, Token.class));
        final Token newToken = new Token(jCas, tokens.get(2).getBegin(), tokens.get(3).getEnd());
        tokens.get(2).removeFromIndexes();
        tokens.get(3).removeFromIndexes();
        newToken.addToIndexes();
        assertThat(newToken.getCoveredText()).isEqualTo("SUB1 homolog");
        s.addToIndexes();
        engine.process(jCas);
        List<String> foundGenes = new ArrayList<>();
        JCoReTreeMapAnnotationIndex<Long, Token> tokenIndex = new JCoReTreeMapAnnotationIndex<>(TermGenerators.longOffsetTermGenerator(), TermGenerators.longOffsetTermGenerator(), jCas, Token.type);
        int i = 0;
        for (Annotation a : jCas.getAnnotationIndex(Gene.type)) {
            Gene g = (Gene) a;
            foundGenes.add(g.getCoveredText());
            assertThat(g.getSpecificType().equals("Gene"));
            final Iterator<Token> tokenIt = tokenIndex.searchFuzzy(g).iterator();
            while (tokenIt.hasNext()) {
                Token token = tokenIt.next();
                assertThat(token.getEmbeddingVectors()).isNotNull();
                assertThat(token.getEmbeddingVectors()).hasSize(1);
                final EmbeddingVector embedding = (EmbeddingVector) token.getEmbeddingVectors().get(0);
                assertThat(embedding.getSource()).isEqualTo("src/test/resources/genes-small-model.pt");
                assertThat(embedding.getVector()).hasSize(1024);

                // We manipulated the first entity to have a single token in UIMA but two for flair.
                // We average the subtoken embeddings to obtain a single embedding for the UIMA token.
                if (i == 0) {
                    // Check that we know have the average of the two subtokens
                    final double[] avgEmbedding = embedding.getVector().toArray();
                    // This is what the embeddingsCache field is for: We can now access the subtoken embeddings created
                    // in another test
                    final double[] sub1Embedding = embeddingsCache.get(0);
                    final double[] homologEmbedding = embeddingsCache.get(1);

                    assertThat(l2Norm(avgEmbedding)).isNotCloseTo(l2Norm(sub1Embedding), Offset.offset(0.0001));
                    assertThat(l2Norm(avgEmbedding)).isNotCloseTo(l2Norm(homologEmbedding), Offset.offset(0.0001));

                    double[] sub1HomoloAvg = new double[sub1Embedding.length];
                    for (int j = 0; j < sub1HomoloAvg.length; j++) {
                        sub1HomoloAvg[j] = (sub1Embedding[j] + homologEmbedding[j]) / 2;
                    }
                    assertThat(l2Norm(avgEmbedding)).isCloseTo(l2Norm(sub1HomoloAvg), Offset.offset(0.0001));
                }
            }
            ++i;
        }
        assertThat(foundGenes).containsExactly("SUB1 homolog", "HIV-1", "VSV-G", "HIV-1");
        engine.collectionProcessComplete();
    }

    private double l2Norm(double[] vector) {
        double norm = 0;
        for (int i = 0; i < vector.length; i++) {
            norm += Math.pow(vector[i], 2);
        }
        return Math.sqrt(norm);
    }

    @Test
    public void testAnnotatorWithAllEmbeddings() throws Exception {
        final JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-semantics-biology-types");
        final AnalysisEngine engine = AnalysisEngineFactory.createEngine(FlairNerAnnotator.class, FlairNerAnnotator.PARAM_STORE_EMBEDDINGS, FlairNerAnnotator.StoreEmbeddings.ALL, FlairNerAnnotator.PARAM_ANNOTATION_TYPE, Gene.class.getCanonicalName(), FlairNerAnnotator.PARAM_FLAIR_MODEL, "src/test/resources/genes-small-model.pt");
        String text = "Knockdown of SUB1 homolog by siRNA inhibits the early stages of HIV-1 replication in 293T cells infected with VSV-G pseudotyped HIV-1 .";
        jCas.setDocumentText(text);
        Sentence s = new Sentence(jCas, 0, text.length());
        addTokens(jCas);
        s.addToIndexes();
        engine.process(jCas);
        for (Token token : jCas.<Token>getAnnotationIndex(Token.type)) {
            assertThat(token.getEmbeddingVectors()).isNotNull();
            assertThat(token.getEmbeddingVectors()).hasSize(1);
            final EmbeddingVector embedding = (EmbeddingVector) token.getEmbeddingVectors().get(0);
            assertThat(embedding.getSource()).isEqualTo("src/test/resources/genes-small-model.pt");
            assertThat(embedding.getVector()).hasSize(1024);
        }
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
