/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 */
package de.julielab.jcore.consumer.bionlpformat.main;

import de.julielab.jcore.consumer.bionlpformat.utils.*;
import de.julielab.jcore.types.EventMention;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;

public class BioEventConsumer extends JCasAnnotator_ImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BioEventConsumer.class);

    public static final String DIRECTORY_PARAM = "outDirectory";
    public static final String A2_FILE_PARAM = "a2FileString";
    public static final String BIOEVENT_SERVICE_MODE_PARAM = "bioEventServiceMode";

    @ConfigurationParameter(name = DIRECTORY_PARAM, mandatory = true)
    private File directory;
    @ConfigurationParameter(name = BIOEVENT_SERVICE_MODE_PARAM, mandatory = false)
    private boolean bioEventServiceMode;
    @ConfigurationParameter(name = A2_FILE_PARAM, mandatory = false, defaultValue = "ONLY_USED_IN_CONJUNCTION_WITH_BIOEVENTSERVICEMODE")
    private String a2FileString;
    File a2File;
    int id = 1;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        if (aContext.getConfigParameterValue(BIOEVENT_SERVICE_MODE_PARAM) != null) {
            bioEventServiceMode = (Boolean) aContext.getConfigParameterValue(BIOEVENT_SERVICE_MODE_PARAM);
        }

        if (!bioEventServiceMode) {
            String directoryName = (String) aContext.getConfigParameterValue(DIRECTORY_PARAM);
            directory = new File(directoryName);
            if (!directory.exists()) {
                directory.mkdirs();
            }
        } else {

            a2FileString = (String) aContext.getConfigParameterValue(A2_FILE_PARAM);
            if (a2FileString != null) {
                a2File = new File(a2FileString);

            } else {
                LOGGER.error("[initialize] a2 file path is null. Exit program");
                System.exit(-1);

            }

        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        CAS cas = aJCas.getCas();
        String id = getDocumentId(cas);
        String documentText = cas.getDocumentText();
        Type eventType = cas.getTypeSystem().getType(EventMention.class.getCanonicalName());
        try {
            if (!bioEventServiceMode) {
                DocumentWriter documentWriter = createDocumentWriter(id);
                BioEventWriter eventWriter = createEventWriter(documentText, id);
                documentWriter.write(cas.getJCas());
                Iterator eventIterator = cas.getAnnotationIndex(eventType).iterator();
                int i = 1;
                while (eventIterator.hasNext()) {
                    EventMention eventMention = (EventMention) eventIterator.next();
                    eventWriter.writeEvent(eventMention);
                }
                documentWriter.close();
                eventWriter.close();
            } else {

                BioEventWriter eventWriter = createEventWriter(documentText, a2File);
                Iterator eventIterator = cas.getAnnotationIndex(eventType).iterator();
                int i = 1;
                while (eventIterator.hasNext()) {
                    EventMention eventMention = (EventMention) eventIterator.next();
                    eventWriter.writeEvent(eventMention);
                }

                eventWriter.close();
            }

        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        } catch (CASException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private BioEventWriter createEventWriter(String documentText, File file) {
        Writer eventFileWriter = null;
        EventTriggerWriter triggerWriter = null;
        try {
            eventFileWriter = null;
            triggerWriter = null;
            eventFileWriter = new BufferedWriter(new FileWriter(a2File));
            triggerWriter = new EventTriggerWriter(eventFileWriter, documentText);
        } catch (IOException e) {
            LOGGER.error("[createEventWriter] " + e.getMessage());
            e.printStackTrace();
        }

        return new BioEventWriter(eventFileWriter, null, triggerWriter, null);

    }

    private BioEventWriter createEventWriter(String documentText, String id) throws AnalysisEngineProcessException {
        Writer proteinFileWriter = null;
        Writer eventFileWriter = null;
        ProteinWriter proteinWriter = null;
        EventTriggerWriter triggerWriter = null;
        EntityWriter entityWriter = null;

        try {
            proteinFileWriter = new BufferedWriter(new FileWriter(new File(directory, id + ".a1")));
            eventFileWriter = new BufferedWriter(new FileWriter(new File(directory, id + ".a2")));
            proteinWriter = new ProteinWriter(proteinFileWriter, documentText);
            triggerWriter = new EventTriggerWriter(eventFileWriter, documentText);
            entityWriter = new EntityWriter(eventFileWriter, documentText);
            return new BioEventWriter(eventFileWriter, proteinWriter, triggerWriter, entityWriter);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private DocumentWriter createDocumentWriter(String id) throws IOException {
        return new DocumentWriter(new BufferedWriter(new FileWriter(new File(directory, id + ".txt"))));
    }

    private String getDocumentId(CAS cas) {
        Header header = null;
        Type headerType = cas.getTypeSystem().getType(Header.class.getCanonicalName());
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
