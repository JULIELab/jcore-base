package de.julielab.jcore.reader.pmc;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Progress;

import java.io.IOException;

public class PMCMultiplierReader extends JCasCollectionReader_ImplBase {
    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {

    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return false;
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[0];
    }
}
