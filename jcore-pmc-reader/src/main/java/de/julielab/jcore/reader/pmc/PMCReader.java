package de.julielab.jcore.reader.pmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.reader.pmc.parser.DocTypeNotFoundException;
import de.julielab.jcore.reader.pmc.parser.DocumentParsingException;
import de.julielab.jcore.reader.pmc.parser.ElementParsingException;
import de.julielab.jcore.reader.pmc.parser.ElementParsingResult;
import de.julielab.jcore.reader.pmc.parser.NxmlDocumentParser;
import de.julielab.jcore.reader.pmc.parser.ParsingResult;
import de.julielab.jcore.reader.pmc.parser.TextParsingResult;

public class PMCReader extends PMCReaderBase {

    public static final String PARAM_INPUT = PMCReaderBase.PARAM_INPUT;
    public static final String PARAM_RECURSIVELY = PMCReaderBase.PARAM_RECURSIVELY;
    public static final String PARAM_SEARCH_ZIP = PMCReaderBase.PARAM_SEARCH_ZIP;
    public static final String PARAM_WHITELIST = PMCReaderBase.PARAM_WHITELIST;
    private static final Logger log = LoggerFactory.getLogger(PMCReader.class);
    private CasPopulator casPopulator;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        try {
            casPopulator = new CasPopulator(pmcFiles);
        } catch (IOException e) {
            log.error("Exception occurred when trying to initialize NXML parser", e);
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void getNext(JCas cas) throws CollectionException {
        URI next = null;
        try {
            next = pmcFiles.next();
            casPopulator.populateCas(next, cas);

        } catch (DocumentParsingException | ElementParsingException e) {
            log.error("Exception occurred when trying to parse {}", next, e);
            throw new CollectionException(e);
        }
        completed++;
    }
}
