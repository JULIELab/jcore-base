package de.julielab.jcore.reader.cord19;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayDeque;
import java.util.Queue;

@ResourceMetaData(name = "JCoRe CORD-19 CAS Multiplier", vendor = "JULIE Lab Jena, Germany", version = "2.5.0-SNAPSHOT", description = "This component reads the CORD-19 (https://pages.semanticscholar.org/coronavirus-research) JSON format into UIMA CAS instances.")
@TypeCapability(inputs = {"de.julielab.jcore.types.casmultiplier.JCoReURI"}, outputs = {"de.julielab.jcore.types.pubmed.Header",
        "de.julielab.jcore.types.Title",
        "de.julielab.jcore.types.pubmed.AbstractText",
        "de.julielab.jcore.types.pubmed.AbstractSection",
        "de.julielab.jcore.types.pubmed.AbstractSectionHeading",
        "de.julielab.jcore.types.Section",
        "de.julielab.jcore.types.Caption",
        "de.julielab.jcore.types.pubmed.InternalReference"})
public class Cord19Multiplier extends JCasMultiplier_ImplBase {

    private Queue<JCoReURI> uris = new ArrayDeque<>();
    private Cord19Reader cord19Reader;
    private String metadataFile;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        cord19Reader = new Cord19Reader();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        AnnotationIndex<JCoReURI> index = aJCas.getAnnotationIndex(JCoReURI.type);
        int i = 0;
        for (JCoReURI uri : index) {
            if (i == 0)
                metadataFile = uri.getUri();
            else
                uris.add(uri);
            ++i;
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
        cord19Reader.readCord19JsonFile(uri, metadataFile, newcas);
        return newcas;
    }
}
