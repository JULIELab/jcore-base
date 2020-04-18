package de.julielab.reader.cord19;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;

import java.util.ArrayDeque;
import java.util.Queue;

public class Cord19Multiplier extends JCasMultiplier_ImplBase {

    private Queue<JCoReURI> uris = new ArrayDeque<>();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        AnnotationIndex<JCoReURI> index = aJCas.getAnnotationIndex(JCoReURI.type);
        for (JCoReURI uri : index) {
            uris.add(uri);
        }
    }

    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
        return !uris.isEmpty();
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        JCas newcas = getEmptyJCas();
        JCoReURI uri = uris.poll();
        Cord19Reader.readCord19JsonFile(newcas);
        return newcas;
    }
}
