package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.JCasIterator;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.IOException;
import java.util.Collection;

public class DeleteMe {
    public static void main(String args[]) throws IOException, UIMAException {
        CollectionReader xmiMpReader = CollectionReaderFactory.createReaderFromPath("src/main/resources/de/julielab/jcore/reader/xmi/desc/de.julielab.jcore.reader.xmi.XmiDBReader.xml");
        JCas jCas = JCasFactory.createJCas("de.julielab.jcore.types.jcore-all-types", "de.julielab.jcore.types.casmultiplier.jcore-dbtable-multiplier-types");
        while (xmiMpReader.hasNext()) {
            xmiMpReader.getNext(jCas.getCas());
            Collection<Token> token = JCasUtil.select(jCas, Token.class);
            for (Token sentence : token) {
                System.out.println("Lemma: " + sentence.getLemma().getValue());
            }
        }
        jCas.reset();
    }
}
