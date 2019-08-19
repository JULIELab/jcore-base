/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.consumer.bionlpformat.main;

import de.julielab.jcore.consumer.bionlpformat.utils.DocumentWriter;
import de.julielab.jcore.consumer.bionlpformat.utils.SegmentWriter;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.*;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.util.Iterator;
import java.util.Optional;

public class SegmentConsumer extends JCasAnnotator_ImplBase {
	

    public static final String DIRECTORY_PARAM = "outDirectory";
    public static final String SEGMENT_TYPE_PARAM = "segmentAnnotationName";

    @ConfigurationParameter(name = DIRECTORY_PARAM)
    private File directory;
    @ConfigurationParameter(name= SEGMENT_TYPE_PARAM, mandatory = false, description = "The qualified UIMA name of the segment annotation to be written. Defaults to de.julielab.jcore.types.Sentence.", defaultValue = "de.julielab.jcore.types.Sentence")
    private String segmentAnnotationName;

    int id = 1;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String directoryName = (String) aContext.getConfigParameterValue(DIRECTORY_PARAM);
        directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }
        segmentAnnotationName = (String) Optional.ofNullable(aContext.getConfigParameterValue(SEGMENT_TYPE_PARAM)).orElse("de.julielab.jcore.types.Sentence");
    }
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
        CAS cas = aJCas.getCas();
        String id = getDocumentId(cas);
        String documentText = cas.getDocumentText();
        Type sentenceType = cas.getTypeSystem().getType(segmentAnnotationName);
        try {
            DocumentWriter documentWriter = createDocumentWriter(id);
            SegmentWriter segmentWriter = createSegmentWriter(documentText, id);
            documentWriter.write(cas.getJCas());
            FSIterator<AnnotationFS> sentenceIterator = cas.getAnnotationIndex(sentenceType).iterator();
            if (!sentenceIterator.hasNext()) {
            	segmentWriter.writeTokensOnly(cas);
            } else {
            	while (sentenceIterator.hasNext()) {
            		Sentence sentence = (Sentence) sentenceIterator.next();
            		segmentWriter.writeSentence(sentence);
            	}
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
