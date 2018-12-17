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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import de.julielab.jcore.consumer.bionlpformat.utils.EntityWriter;
import de.julielab.jcore.consumer.bionlpformat.utils.EventMentionWriter;
import de.julielab.jcore.consumer.bionlpformat.utils.MedEventWriter;
import de.julielab.jcore.types.EntityMention;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.pubmed.Header;

public class MedEventConsumer extends JCasAnnotator_ImplBase {

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
        Type entityType = cas.getTypeSystem().getType(EntityMention.class.getCanonicalName());
        try {
            DocumentWriter documentWriter = createDocumentWriter(id);
            MedEventWriter eventWriter = createEventWriter(documentText, id);
            documentWriter.write(cas.getJCas());
            FSIterator<AnnotationFS> entityIterator = cas.getAnnotationIndex(entityType).iterator();
            while (entityIterator.hasNext()) {
                EntityMention entityMention = (EntityMention) entityIterator.next();
                eventWriter.writeEvent(entityMention);
            }
            documentWriter.close();
            eventWriter.close();

        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        } catch (CASException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private MedEventWriter createEventWriter(String documentText, String id) throws AnalysisEngineProcessException {
        Writer annotationFileWriter = null;
        EntityWriter medicationWriter = null;
        EventMentionWriter attributeWriter = null;

        try {
            annotationFileWriter = new BufferedWriter(new FileWriter(new File(directory, id + ".ann")));
            medicationWriter = new EntityWriter(annotationFileWriter, documentText);
            attributeWriter = new EventMentionWriter(annotationFileWriter, documentText);
            return new MedEventWriter(annotationFileWriter, medicationWriter, attributeWriter);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private DocumentWriter createDocumentWriter(String id) throws IOException {
        return new DocumentWriter(new BufferedWriter(new FileWriter(new File(directory, id + ".txt"))));
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
