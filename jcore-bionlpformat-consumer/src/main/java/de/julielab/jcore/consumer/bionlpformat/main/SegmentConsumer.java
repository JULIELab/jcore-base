package de.julielab.jcore.consumer.bionlpformat.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.consumer.bionlpformat.utils.DocumentWriter;
import de.julielab.jcore.consumer.bionlpformat.utils.SegmentWriter;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.pubmed.Header;

public class SegmentConsumer extends JCasAnnotator_ImplBase {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(BioEventConsumer.class);

    public static final String DIRECTORY_PARAM = "outDirectory";

    @ConfigurationParameter(name = DIRECTORY_PARAM, mandatory = true)
    private File directory;
    int id = 1;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String directoryName = (String) aContext.getConfigParameterValue(DIRECTORY_PARAM);
        directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
        CAS cas = aJCas.getCas();
        String id = getDocumentId(cas);
        String documentText = cas.getDocumentText();
        Type sentenceType = cas.getTypeSystem().getType(Sentence.class.getCanonicalName());
        try {
            DocumentWriter documentWriter = createDocumentWriter(id);
            SegmentWriter segmentWriter = createSegmentWriter(documentText, id);
            documentWriter.write(cas.getJCas());
            FSIterator<AnnotationFS> sentenceIterator = cas.getAnnotationIndex(sentenceType).iterator();
            while (sentenceIterator.hasNext()) {
                Sentence sentence = (Sentence) sentenceIterator.next();
                segmentWriter.writeSentence(sentence);
            }
            documentWriter.close();
            segmentWriter.close();
        }
        catch (IOException e) {
        	throw new AnalysisEngineProcessException(e);
        } catch (CASException e) {
        	throw new AnalysisEngineProcessException(e);
        }
	}
	
	private DocumentWriter createDocumentWriter(String id) throws IOException {
        return new DocumentWriter(new BufferedWriter(
        		new OutputStreamWriter(new FileOutputStream(new File(directory, id + ".txt")), "UTF-8")));
    }
	
	private SegmentWriter createSegmentWriter(String documentText, String id) throws AnalysisEngineProcessException {
		Writer annotationFileWriter = null;
		try {
            annotationFileWriter = new BufferedWriter(
            		new OutputStreamWriter(new FileOutputStream(new File(directory, id + ".ann")), "UTF-8"));
            return new SegmentWriter(annotationFileWriter, documentText);
		} catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
	}
    
    private String getDocumentId(CAS cas) {
        Header header = null;
        Iterator<org.apache.uima.jcas.tcas.Annotation> iterator;
        try {
            iterator = cas.getJCas().getAnnotationIndex(Header.type).iterator();

            header = (Header) iterator.next();
        } catch (CASRuntimeException e) {
            e.printStackTrace();
        } catch (CASException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (header != null) {
            return header.getDocId();
        }
        return new Integer(id++).toString();
    }
}
