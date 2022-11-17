package de.julielab.jcore.reader.cord19;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ResourceMetaData(name = "JCoRe CORD-19 Multiplier Reader", vendor = "JULIE Lab Jena, Germany", version = "2.5.0-SNAPSHOT", description = "This component reads file paths to JSON files and the CORD-19 (https://pages.semanticscholar.org/coronavirus-research) meta data file to send them to CAS multipliers.")
@TypeCapability(outputs = {"de.julielab.jcore.types.casmultiplier.JCoReURI"})
public class Cord19MultiplierReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_INPUT_DIR = "InputDirectory";
    public static final String PARAM_SEARCH_RECURSIVELY = "SearchRecursively";
    public static final String PARAM_METADATA_FILE = "MetadataFile";
    private final static Logger log = LoggerFactory.getLogger(Cord19MultiplierReader.class);
    @ConfigurationParameter(name = PARAM_SEARCH_RECURSIVELY, mandatory = false, defaultValue = "false", description = "Whether or not to search for CORD-19 JSON files recursively in subdirectories of the input directory.")
    boolean searchRecursively;
    @ConfigurationParameter(name = PARAM_INPUT_DIR, description = "A directory that contains CORD-19 JSON files.")
    private File inputDir;
    @ConfigurationParameter(name = PARAM_METADATA_FILE, mandatory = false, description = "The path of the CORD-19 metadata file. This parameter can be omitted if the InputDirectory contains the file 'metadata.csv'.")
    private File metadataFile;
    private ConcurrentFileWalker fileWalker;
    private int completed;
    private List<Path> currentFileBatch;
    private int currentBatchIndex;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        inputDir = new File((String) context.getConfigParameterValue(PARAM_INPUT_DIR));
        searchRecursively = Optional.ofNullable((Boolean)context.getConfigParameterValue(PARAM_SEARCH_RECURSIVELY)).orElse(false);
        metadataFile = new File((String) Optional.ofNullable(context.getConfigParameterValue(PARAM_METADATA_FILE)).orElse(new File(inputDir, "metadata.csv").getAbsolutePath()));
        fileWalker = new ConcurrentFileWalker(inputDir.toPath());
        fileWalker.start();
        completed = 0;
        currentFileBatch = Collections.emptyList();
        if (!metadataFile.exists())
            log.warn("Could not find the metadata file {}. The metadata information - like the actual CORD-19 document ID - will not be added to the CASes.", metadataFile.getAbsolutePath());
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void getNext(JCas jCas) throws CollectionException {
        try {
            if (hasNext()) {
                JCoReURI metadataUri = new JCoReURI(jCas);
                metadataUri.setUri(metadataFile.toString());
                metadataUri.addToIndexes();
                for (; currentBatchIndex < currentFileBatch.size(); currentBatchIndex++) {
                    Path p = currentFileBatch.get(currentBatchIndex);
                    if (p != Cord19FileVisitor.END) {
                        JCoReURI uri = new JCoReURI(jCas);
                        try {
                            uri.setUri(p.toUri().toString());
                        } catch (NullPointerException e) {
                            log.error("Could not retrieve URI string for path {}, resolved URI {}", p, p!= null ? p.toUri() : "<path is null>");
                        }
                        uri.addToIndexes();
                        ++completed;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error when getting the next files", e);
            throw new CollectionException(e);
        }
    }


    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(completed, completed, "files")};
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        if (currentBatchIndex == currentFileBatch.size()) {
            currentFileBatch = fileWalker.getFiles(50);
            currentBatchIndex = 0;
        }
        boolean hasNext = currentFileBatch.get(currentBatchIndex) != Cord19FileVisitor.END;
        if (!hasNext)
            log.info("Read {} files.", completed);
        return hasNext;
    }

}
