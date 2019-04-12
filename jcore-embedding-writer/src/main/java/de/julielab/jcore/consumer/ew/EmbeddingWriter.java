package de.julielab.jcore.consumer.ew;

import de.julielab.jcore.types.Token;
import de.julielab.jcore.utility.index.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@ResourceMetaData(name = "JCoRe Flair Embedding Writer", description = "Given a Flair compatible embedding and a UIMA annotation type, this component prints the embeddings of tokens annotated with the annotation to a file.")
@TypeCapability(inputs = {"de.julielab.jcore.types.Token", "de.julielab.jcore.types.EmbeddingVector"})
public class EmbeddingWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_ANNOTATION_TYPE = "AnnotationType";
    public static final String PARAM_OUTDIR = "OutputDirectory";
    public static final String PARAM_GZIP = "UseGzip";

    private final static Logger log = LoggerFactory.getLogger(EmbeddingWriter.class);
    private static int currentConsumerNumber = 0;
    @ConfigurationParameter(name = PARAM_GZIP, mandatory = false, description = "If set to true, the output data will be compressed. Defaults to false.")
    boolean gzip;
    ByteBuffer bb;
    @ConfigurationParameter(name = PARAM_ANNOTATION_TYPE, mandatory = false, description = "Fully qualified type name to output embeddings for. If an annotation spans multiple tokens, their embeddings are averaged. If this parameter is omitted, the embeddings of all tokens will be written")
    private String annotationType;
    @ConfigurationParameter(name = PARAM_OUTDIR, description = "The directory into which the embedding files should be written. In a multi-threaded pipeline, each thread writes its own file. The file name will also include the the host name on which it ran.")
    private String outputDir;
    private String pid;
    private String hostName;
    private int consumerNumber;
    private OutputStream os;
    private File file;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        annotationType = (String) aContext.getConfigParameterValue(PARAM_ANNOTATION_TYPE);
        outputDir = (String) aContext.getConfigParameterValue(PARAM_OUTDIR);
        gzip = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_GZIP)).orElse(false);
        pid = getPID();
        hostName = getHostName();
        synchronized (PARAM_OUTDIR) {
            consumerNumber = currentConsumerNumber++;
        }
        file = new File(outputDir + File.separator + "embeddings-" + hostName + "-" + pid + "-writer" + consumerNumber + ".dat" + (gzip ? ".gz" : ""));
        final File dir = file.getParentFile();
        if (!dir.exists())
            dir.mkdirs();
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            if (gzip)
                os = new GZIPOutputStream(os);
        } catch (FileNotFoundException e) {
            log.error("Could not create output stream for the output file {}", file, e);
            throw new ResourceInitializationException(e);
        } catch (IOException e) {
            log.error("Could not create GZIPOutputStream", e);
            throw new ResourceInitializationException(e);
        }
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
                    writeEmbeddingsForAnnotation(overlappingTokens.collect(Collectors.toList()));
                }
            } else {
                for (Annotation token : aJCas.getAnnotationIndex(Token.type))
                    writeEmbeddingsForAnnotation(Arrays.asList((Token)token));
            }
        } catch (IOException e) {
            log.error("Could not write to output stream", e);
            throw new AnalysisEngineProcessException(e);
        }

    }

    private void writeEmbeddingsForAnnotation(List<Token> tokens) throws IOException {
        final double[] avgEmbedding = getAverageEmbeddingVector(tokens.stream());
        // get the text from the first to the last token
        String text = tokens.get(0).getCAS().getDocumentText().substring(tokens.get(0).getBegin(), tokens.get(tokens.size() - 1).getEnd());
        final byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int requiredBytes = Integer.BYTES * 2 + textBytes.length + avgEmbedding.length * Double.BYTES;
        if (bb == null || bb.capacity() < requiredBytes)
            bb = ByteBuffer.allocate(requiredBytes);
        bb.position(0);

        bb.putInt(textBytes.length);
        bb.put(textBytes);
        bb.putInt(avgEmbedding.length);
        for (double d : avgEmbedding)
            bb.putDouble(d);

        byte[] output = bb.array();
        if (gzip) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final GZIPOutputStream os = new GZIPOutputStream(baos);
            os.write(output);
            os.close();
            output = baos.toByteArray();
        }

        os.write(output);
    }

    private double[] getAverageEmbeddingVector(Stream<Token> tokens) {
        final MutablePair<Integer, double[]> vectorAvg = tokens.collect(() -> new MutablePair<Integer, double[]>(0, null), (p, t) -> {
            p.setLeft(p.getLeft() + 1);
            double[] vector = p.getRight();
            if (vector == null) {
                vector = new double[t.getEmbeddingVectors(0).getVector().size()];
                t.getEmbeddingVectors(0).getVector().copyToArray(0, vector, 0, vector.length);
                p.setRight(vector);
            } else {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] += t.getEmbeddingVectors(0).getVector(i);
                }
            }
        }, (p1, p2) -> {
            p1.setLeft(p1.getLeft() + p2.getLeft());
            double[] vectorSum = p1.getRight();
            for (int i = 0; i < vectorSum.length; i++)
                vectorSum[i] += p2.getRight()[i];
        });
        return DoubleStream.of(vectorAvg.getRight()).map(d -> d / vectorAvg.getLeft()).toArray();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        try {
            os.close();
        } catch (IOException e) {
            log.error("Exception when closing the output stream to file {}", file, e);
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
