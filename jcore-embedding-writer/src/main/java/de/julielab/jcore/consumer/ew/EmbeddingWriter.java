package de.julielab.jcore.consumer.ew;

import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.index.Comparators;
import de.julielab.jcore.utility.index.JCoReTreeMapAnnotationIndex;
import de.julielab.jcore.utility.index.TermGenerators;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@ResourceMetaData(name = "JCoRe Flair Embedding Writer", description = "Given a Flair compatible embedding and a UIMA annotation type, this component prints the embeddings of tokens annotated with the annotation to a file.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Token", "de.julielab.jcore.types.EmbeddingVector"})
public class EmbeddingWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_ANNOTATION_TYPE = "AnnotationType";
    public static final String PARAM_OUTDIR = "OutputDirectory";
    public static final String PARAM_GZIP = "UseGzip";
    public static final String PARAM_MAX_FILE_ENTRY_SIZE = "MaximumEntriesPerOutputFile";

    private final static Logger log = LoggerFactory.getLogger(EmbeddingWriter.class);
    private static int currentConsumerNumber = 0;
    @ConfigurationParameter(name = PARAM_GZIP, mandatory = false, description = "If set to true, the output data will be compressed. Defaults to false.")
    boolean gzip;
    @ConfigurationParameter(name = PARAM_ANNOTATION_TYPE, mandatory = false, description = "Fully qualified type name to output embeddings for. If an annotation spans multiple tokens, their embeddings are averaged. If this parameter is omitted, the embeddings of all tokens will be written")
    private String annotationType;
    @ConfigurationParameter(name = PARAM_OUTDIR, description = "The directory into which the embedding files should be written. In a multi-threaded pipeline, each thread writes its own files. The file names will also include the the host name on which it ran. All output files are ordered by tokens or covered annotation text spans. To control the maximum file size, refer to the " + PARAM_MAX_FILE_ENTRY_SIZE + " parameter.")
    private String outputDir;
    @ConfigurationParameter(name = PARAM_MAX_FILE_ENTRY_SIZE, mandatory = false, description = "The text-embedding pairs are accumulated from multiple CASes before writing them to file. The accumulator keeps the entries sorted by the text part, thus output files are also ordered. This parameter defines the maximum size the accumulate will take before writing its contents to file and clearing itself.", defaultValue = "200000")
    private int maxEntriesPerFile;
    private String pid;
    private String hostName;
    private int consumerNumber;
    private OutputStream os;
    private File nextOutputFile;
    private int currentBatch;
    // This ByteBuffer is used to write the current embedding data into
    private ByteBuffer bb;
    // We store the embeddings indexed with their text in an ordered fashion. When the maximum size of the
    // cache is reached, it is output to file. The advantage is that the output files are always ordered
    // which makes the subsequent merging easier.
    private TreeMap<String, byte[]> outputCache;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        annotationType = (String) aContext.getConfigParameterValue(PARAM_ANNOTATION_TYPE);
        outputDir = (String) aContext.getConfigParameterValue(PARAM_OUTDIR);
        gzip = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_GZIP)).orElse(false);
        maxEntriesPerFile = Integer.valueOf(Optional.ofNullable((String) aContext.getConfigParameterValue(PARAM_MAX_FILE_ENTRY_SIZE)).orElse("200000"));
        pid = getPID();
        hostName = getHostName();
        synchronized (PARAM_OUTDIR) {
            consumerNumber = currentConsumerNumber++;
        }
        currentBatch = 0;
        nextOutputFile = getNextOutputFile();
        final File dir = nextOutputFile.getParentFile();
        if (!dir.exists())
            dir.mkdirs();
        try {
            os = new BufferedOutputStream(new FileOutputStream(nextOutputFile));
            if (gzip)
                os = new GZIPOutputStream(os);
        } catch (FileNotFoundException e) {
            log.error("Could not create output stream for the output file {}", nextOutputFile, e);
            throw new ResourceInitializationException(e);
        } catch (IOException e) {
            log.error("Could not create GZIPOutputStream", e);
            throw new ResourceInitializationException(e);
        }
        outputCache = new TreeMap<>();
    }

    private File getNextOutputFile() {
        return new File(outputDir + File.separator + "embeddings-" + hostName + "-" + pid + "-writer" + consumerNumber + "-batch" + ++currentBatch + ".dat" + (gzip ? ".gz" : ""));
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        try {
            if (!StringUtils.isBlank(annotationType)) {
                Type type = aJCas.getTypeSystem().getType(annotationType);
                if (type == null) {
                    throw new AnalysisEngineProcessException(new IllegalArgumentException("The type " + annotationType + " was not found in the type system."));
                }
                if (!aJCas.getAnnotationIndex(type).iterator().hasNext())
                    return;
                JCoReTreeMapAnnotationIndex<Long, Token> tokenIndex = new JCoReTreeMapAnnotationIndex(Comparators.longOverlapComparator(), TermGenerators.longOffsetTermGenerator(), TermGenerators.longOffsetTermGenerator(), aJCas, Token.type);
                for (Annotation a : aJCas.getAnnotationIndex(type)) {
                    final Stream<Token> overlappingTokens = tokenIndex.search(a);
                    cacheEmbeddingsForAnnotation(overlappingTokens.collect(Collectors.toList()));
                }
            } else {
                for (Annotation token : aJCas.getAnnotationIndex(Token.type))
                    cacheEmbeddingsForAnnotation(Arrays.asList((Token) token));
            }
            if (outputCache.size() >= maxEntriesPerFile)
                writeEmbeddingsToFile();
        } catch (IOException e) {
            log.error("Could not write to output stream", e);
            throw new AnalysisEngineProcessException(e);
        }

    }

    private void writeEmbeddingsToFile() throws IOException {
        for (byte[] textVector : outputCache.values())
            os.write(textVector);
        nextOutputFile = getNextOutputFile();
        outputCache.clear();
    }

    private void cacheEmbeddingsForAnnotation(List<Token> tokens) throws IOException {
        // get the text from the first to the last token
        String text = tokens.get(0).getCAS().getDocumentText().substring(tokens.get(0).getBegin(), tokens.get(tokens.size() - 1).getEnd());
        final double[] avgEmbedding = VectorOperations.getAverageEmbeddingVector(tokens.stream().map(t -> t.getEmbeddingVectors(0).getVector().toArray()));
        byte[] cacheArray = Encoder.encodeTextVectorPair(text, avgEmbedding, bb);
        outputCache.put(text, cacheArray);
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        try {
            writeEmbeddingsToFile();
        } catch (IOException e) {
            log.error("Exception while writing the last batch of embedding vectors to file {}", nextOutputFile, e);
            throw new AnalysisEngineProcessException(e);
        }
        try {
            os.close();
        } catch (IOException e) {
            log.error("Exception when closing the output stream to file {}", nextOutputFile, e);
            throw new AnalysisEngineProcessException(e);
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
