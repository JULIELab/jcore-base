package de.julielab.jcore.reader.bionlpformat.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

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

import de.julielab.jcore.reader.bionlpformat.utils.TextFileMapper;
import de.julielab.jcore.reader.bionlpformat.utils.AnnotationFileMapper_Med;
import de.julielab.jcore.reader.bionlpformat.utils.AnnotationFileMapper_Seg;

public class SegmentReader  extends CollectionReader_ImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BioEventReader.class);
    public static final String DIRECTORY_PARAM = "inDirectory";

    private File[] files;
    private int i;
    private AnnotationFileMapper_Seg annotationFileMapper;
    private TextFileMapper textFileMapper;

    @ConfigurationParameter(name = DIRECTORY_PARAM, mandatory = true)
    private String directoryName;
    private File directory;

    @Override
    public void initialize() throws ResourceInitializationException {
        super.initialize();

        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File directory, String name) {
                return name.endsWith(".txt");
            }
        };
        directoryName = (String) getConfigParameterValue(DIRECTORY_PARAM);
        directory = new File(directoryName);
        files = directory.listFiles(filter);
        
        textFileMapper = new TextFileMapper();
        annotationFileMapper = new AnnotationFileMapper_Seg();
        i = -1;
    }

    @Override
    public void getNext(CAS cas) throws IOException, CollectionException {
        File textFile = null;
        try {
            textFile = files[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.error(
                    "File access crashed for \"" + this.getClass().getName() + "\": call \"hasNext()\" first!");
            e.printStackTrace();
        }
        String textFilename = textFile.getName();
        String fid = textFilename.substring(0, textFilename.lastIndexOf("."));
        File annotationFile = new File(directory, fid + ".ann");
        JCas jcas = null;
        try {
            jcas = cas.getJCas();
        } catch (CASException e) {
            throw new CollectionException(e);
        }
        LOGGER.debug("Reading " + fid);
        BufferedReader textReader = new BufferedReader(new FileReader(textFile));
        textFileMapper.mapAbstractFile(fid, textFilename, textReader, jcas);
        BufferedReader annotationReader = new BufferedReader(new FileReader(annotationFile));
        annotationFileMapper.mapEventFile(annotationReader, jcas);
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
