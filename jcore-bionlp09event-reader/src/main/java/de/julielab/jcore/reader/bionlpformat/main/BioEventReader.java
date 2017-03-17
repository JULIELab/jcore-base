/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 */

package de.julielab.jcore.reader.bionlp09event.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.reader.bionlp09event.utils.AbstractFileMapper;
import de.julielab.jcore.reader.bionlp09event.utils.AnnotationFileMapper;
import de.julielab.jcore.types.Annotation;

public class EventReader extends CollectionReader_ImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventReader.class);
    public static final String DIRECTORY_PARAM = "inDirectory";
    public static final String PROTEIN_FILE_PARAM = "proteinFile";
    public static final String ABSTRACT_FILE_PARAM = "abstractFile";
    public static final String BIOEVENT_SERVICE_MODE_PARAM = "bioEventServiceMode";

    private File[] files;
    private int i;
    private AnnotationFileMapper annotationFileMapper;
    private AbstractFileMapper abstractFileMapper;

    @ConfigurationParameter(name = DIRECTORY_PARAM, mandatory = true)
    private String directoryName;
    @ConfigurationParameter(name = PROTEIN_FILE_PARAM, mandatory = false)
    private String proteinFileString;
    @ConfigurationParameter(name = ABSTRACT_FILE_PARAM, mandatory = false)
    private String abstractFileString;
    @ConfigurationParameter(name = BIOEVENT_SERVICE_MODE_PARAM, mandatory = false)
    private boolean bioEventServiceMode;

    private File directory;
    File proteinFile;
    File abstractFile;

    @Override
    public void initialize() throws ResourceInitializationException {
        super.initialize();

        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File directory, String name) {
                return name.endsWith(".txt");
            }
        };
        if ((Boolean) getConfigParameterValue(BIOEVENT_SERVICE_MODE_PARAM) != null) {
            bioEventServiceMode = (Boolean) getConfigParameterValue(BIOEVENT_SERVICE_MODE_PARAM);
        }

        if (!bioEventServiceMode) {
            directoryName = (String) getConfigParameterValue(DIRECTORY_PARAM);
            directory = new File(directoryName);
            files = directory.listFiles(filter);
        }

        abstractFileMapper = new AbstractFileMapper();
        annotationFileMapper = new AnnotationFileMapper();
        i = -1;

        if (bioEventServiceMode) {
            proteinFileString = (String) getConfigParameterValue(PROTEIN_FILE_PARAM);
            abstractFileString = (String) getConfigParameterValue(ABSTRACT_FILE_PARAM);

            if (abstractFileString != null) {
                abstractFile = new File(abstractFileString);
            }

            if (abstractFile != null && abstractFile.exists()) {
                files = new File[1];
                files[0] = new File(abstractFileString);
            } else {
                LOGGER.error("[initialize] abstract file does not exist");

            }

            if (proteinFileString != null) {
                proteinFile = new File(proteinFileString);
            }

            if (proteinFile != null && proteinFile.exists()) {
                ;
            } else {
                LOGGER.error("[initialize] protein file does not exist");

            }

        }

    }

    @Override
    public void getNext(CAS cas) throws IOException, CollectionException {
        if (!bioEventServiceMode) {
            File abstractFile = null;
            try {
                abstractFile = files[i];
            } catch (ArrayIndexOutOfBoundsException e) {
                LOGGER.error(
                        "File access crashed for \"" + this.getClass().getName() + "\": call \"hasNext()\" first!");
                e.printStackTrace();
            }
            String abstractFilename = abstractFile.getName();
            String pid = abstractFilename.substring(0, abstractFilename.indexOf("."));
            File proteinFile = new File(directory, pid + ".a1");
            File eventFile = new File(directory, pid + ".a2");
            JCas jcas = null;
            try {
                jcas = cas.getJCas();
            } catch (CASException e) {
                throw new CollectionException(e);
            }
            LOGGER.debug("Reading " + pid);
            BufferedReader abstractReader = new BufferedReader(new FileReader(abstractFile));
            abstractFileMapper.mapAbstractFile(pid, abstractFilename, abstractReader, jcas);
            BufferedReader proteinReader = new BufferedReader(new FileReader(proteinFile));
            Map<String, Annotation> mappedProteins = annotationFileMapper.mapProteinFile(proteinReader, jcas);
            BufferedReader eventReader = null;
            if (eventFile.exists()) {
                eventReader = new BufferedReader(new FileReader(eventFile));
                annotationFileMapper.mapEventFile(mappedProteins, eventReader, jcas);
            }
        } else {

            File abstractFile = files[i];
            String abstractFilename = abstractFile.getName();
            String pid = abstractFilename.substring(0, abstractFilename.indexOf("."));
            File proteinFile = new File(proteinFileString);

            JCas jcas = null;
            try {
                jcas = cas.getJCas();
            } catch (CASException e) {
                throw new CollectionException(e);
            }
            LOGGER.debug("Reading " + pid);

            BufferedReader abstractReader = new BufferedReader(new FileReader(abstractFile));
            abstractFileMapper.mapAbstractFile(pid, abstractFilename, abstractReader, jcas);
            BufferedReader proteinReader = new BufferedReader(new FileReader(proteinFile));
            Map<String, Annotation> mappedProteins = annotationFileMapper.mapProteinFile(proteinReader, jcas);

        }

    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[] { new ProgressImpl(i, files.length, "docs") };
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return ++i < files.length;
    }
}
