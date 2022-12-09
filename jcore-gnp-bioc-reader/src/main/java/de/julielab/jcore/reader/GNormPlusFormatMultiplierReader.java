package de.julielab.jcore.reader;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

@ResourceMetaData(name = "JCoRe GNormPlus Format Multiplier Reader", description = "A reader for the BioC XML format used by GNormPlus. Requires the matching multiplier.")
public class GNormPlusFormatMultiplierReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_INPUT_PATH = "InputPath";
    public static final String PARAM_RECURSIVE = "Recursive";
    public static final String PARAM_BATCH_SIZE = "BatchSize";
    private final static Logger log = LoggerFactory.getLogger(GNormPlusFormatMultiplierReader.class);
    @ConfigurationParameter(name = PARAM_INPUT_PATH, description = "Path to a directory or file to be read. In case of a directory, all files ending in .xml will be read.")
    private String inputPathString;
    @ConfigurationParameter(name = PARAM_RECURSIVE, mandatory = false, defaultValue = "true", description = "Whether to read also the subdirectories of the input directory, if the input path points to a directory.")
    private boolean recursive;
    @ConfigurationParameter(name = PARAM_BATCH_SIZE, mandatory = false, defaultValue = "20", description = "The number of XML file URI references to send to the CAS multipliers in each work assignment. Defaults to 20.")
    private int batchSize;
    private Iterator<Path> fileIterator;
    private int completed;


    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        inputPathString = (String) context.getConfigParameterValue(PARAM_INPUT_PATH);
        recursive = Optional.of((boolean) context.getConfigParameterValue(PARAM_RECURSIVE)).orElse(true);
        batchSize = Optional.of((Integer) context.getConfigParameterValue((PARAM_BATCH_SIZE))).orElse(20);
        try {
            Path inputPath = Path.of(inputPathString);
            Stream<Path> pathStream;
            if (recursive)
                pathStream = Files.walk(inputPath, FileVisitOption.FOLLOW_LINKS);
            else
                pathStream = Files.list(inputPath);
            pathStream = pathStream.filter(p -> p.toString().toLowerCase().endsWith(".xml"));
            fileIterator = pathStream.iterator();
        } catch (IOException e) {
            log.error("Could not read the files of inputPath {}", inputPathString, e);
            throw new ResourceInitializationException(e);
        }
        completed = 0;
    }

    @Override
    public void getNext(JCas jCas) throws CollectionException {
        for (int i = 0; i < batchSize && fileIterator.hasNext(); i++) {
            URI uri = fileIterator.next().toUri();
            try {
                JCoReURI fileType = new JCoReURI(jCas);
                fileType.setUri(uri.toString());
                fileType.addToIndexes();
            } catch (Exception e) {
                log.error("Exception with URI: " + uri, e);
                throw new CollectionException(e);
            }
            completed++;
            if (completed % 10 == 0) {
                log.debug("{} input files read", completed);
            }
        }
    }


    @Override
    public Progress[] getProgress() {
            return new Progress[]{new ProgressImpl(completed, -1, "documents")};
    }

    @Override
    public boolean hasNext() {
        return fileIterator.hasNext();
    }

}
