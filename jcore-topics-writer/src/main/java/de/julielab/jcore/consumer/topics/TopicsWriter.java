package de.julielab.jcore.consumer.topics;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.jcore.types.DocumentTopics;
import de.julielab.jcore.types.Header;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ResourceMetaData(name = "JCoRe Topics Writer", description = "Writes the topic weights, given the jcore-topic-indexing-ae running before, into a simple text file. Thus, the output consists of a sequency of double numbers encodes as strings, separated by tab characters. The topic ID is just the 0-based index of each number, from left to right in the written file. The first entry of each file is the document ID.")
@TypeCapability(inputs = {"de.julielab.jcore.types.DocumentTopics"})
public class TopicsWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_OUTPUT_DIR = "OutputDirectory";
    private final static Logger log = LoggerFactory.getLogger(TopicsWriter.class);
    private static int currentConsumerNumber = 0;
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR, description = "The directory to place the files into that contain " +
            "topic weight assignments for the documents. For corpora larger than a few hundred documents, or even then, " +
            "multiple files will be written, each containing a batch of document weights. At the end of processing, " +
            "all these files can just be concatenated to get one large file with the topic weights of all documents.")
    private File outputDirectory;
    private String pid;
    private String hostName;
    private int consumerNumber;
    private Map<String, List<String>> outputCache = new HashMap<>();
    private int docNum;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        outputDirectory = new File((String) aContext.getConfigParameterValue(PARAM_OUTPUT_DIR));
        pid = getPID();
        hostName = getHostName();
        synchronized (PARAM_OUTPUT_DIR) {
            consumerNumber = currentConsumerNumber++;
        }
        docNum = 0;
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas)  {
        Collection<DocumentTopics> documentTopicAnnotations = JCasUtil.select(aJCas, DocumentTopics.class);
        String docId;
        try {
            docId = JCasUtil.selectSingle(aJCas, Header.class).getDocId();
        } catch (IllegalArgumentException e) {
            // No header
            docId = "doc" + docNum;
        }
        for (DocumentTopics topics : documentTopicAnnotations) {
            String modelID = topics.getModelID();
            String modelVersion = topics.getModelVersion();
            String mapKey = modelID + "-" + modelVersion;
            List<String> cacheForModel = outputCache.compute(mapKey, (key, list) -> list != null ? list : new ArrayList<>());
            String cacheLine = Stream.concat(Stream.of(docId), Arrays.stream(topics.getWeights().toArray()).mapToObj(String::valueOf)).collect(Collectors.joining("\t"));
            cacheForModel.add(cacheLine);
        }
        docNum++;
    }


    @Override
    public void batchProcessComplete() throws AnalysisEngineProcessException {
        super.batchProcessComplete();
        try {
            writeCache();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            writeCache();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void writeCache() throws IOException {
        if (!outputDirectory.exists())
            outputDirectory.mkdirs();
        for (String mapKey : outputCache.keySet()) {
            File file = new File(outputDirectory.getAbsolutePath() + File.separator + mapKey + "-" + hostName + "-" + pid + "-writer" + currentConsumerNumber + ".gz");
            try (BufferedWriter bw = FileUtilities.getWriterToFile(file)) {
                List<String> lines = outputCache.get(mapKey);
                for (String line : lines) {
                    bw.write(line);
                    bw.newLine();
                }
            }

        }
    }

    private String getPID() {
        String id = ManagementFactory.getRuntimeMXBean().getName();
        return id.substring(0, id.indexOf('@'));
    }

    private String getHostName() {
        InetAddress address;
        String hostName;
        try {
            address = InetAddress.getLocalHost();
            hostName = address.getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        return hostName;
    }
}
