package de.julielab.reader.cord19;

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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ResourceMetaData(name = "JCoRe CORD-19 Reader", vendor = "JULIE Lab Jena, Germany", version = "2.5.0-SNAPSHOT", description = "This component reads the CORD-19 (https://pages.semanticscholar.org/coronavirus-research) JSON format into UIMA CAS instances.")
@TypeCapability(outputs = {})
public class Cord19MultiplierReader extends JCasCollectionReader_ImplBase {

    public static final String PARAM_INPUT_DIR = "InputDirectory";
    public static final String PARAM_SEARCH_RECURSIVELY = "SearchRecursively";
    private final static Logger log = LoggerFactory.getLogger(Cord19MultiplierReader.class);
    @ConfigurationParameter(name = PARAM_SEARCH_RECURSIVELY, mandatory = false, defaultValue = "false", description = "Whether or not to search for CORD-19 JSON files recursively in subdirectories of the input directory.")
    boolean searchRecursively;
    @ConfigurationParameter(name = PARAM_INPUT_DIR, description = "A directory that contains CORD-19 JSON files.")
    private Path inputDir;
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
        inputDir = Path.of((String) context.getConfigParameterValue(PARAM_INPUT_DIR));
        searchRecursively = Boolean.parseBoolean((String) Optional.ofNullable(context.getConfigParameterValue(PARAM_SEARCH_RECURSIVELY)).orElse("false"));
        fileWalker = new ConcurrentFileWalker(inputDir);
        fileWalker.start();
        completed = 0;
        currentFileBatch = Collections.emptyList();
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void getNext(JCas jCas) throws CollectionException {
        for (; currentBatchIndex < currentFileBatch.size(); currentBatchIndex++) {
            Path p = currentFileBatch.get(currentBatchIndex);
            if (p != Cord19FileVisitor.END) {
                JCoReURI uri = new JCoReURI(jCas);
                uri.setUri(p.toUri().toString());
                uri.addToIndexes();
            }
        }
    }


    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(completed, 0, "documents")};
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        if (currentBatchIndex == currentFileBatch.size()) {
            currentFileBatch = fileWalker.getFiles(50);
            currentBatchIndex = 0;
        }
        return currentFileBatch.get(currentBatchIndex) == Cord19FileVisitor.END;
    }

}
