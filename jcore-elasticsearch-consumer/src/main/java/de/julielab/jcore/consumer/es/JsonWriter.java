package de.julielab.jcore.consumer.es;

import de.julielab.jcore.consumer.es.preanalyzed.Document;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

@ResourceMetaData(name = "JCoRe JSON Writer")
public class JsonWriter extends AbstractCasToJsonConsumer {

    public static final String PARAM_OUTPUT_DEST = "OutputDestination";
    public static final String PARAM_GZIP = "GZIP";
    public static final String PARAM_FILE_OUTPUT = "FileOutput";
    private static final Logger log = LoggerFactory.getLogger(AbstractCasToJsonConsumer.class);
    private static int writerNumber;
    private List<Document> documentBatch = new ArrayList<>();
    @ConfigurationParameter(name = PARAM_OUTPUT_DEST, description = "The path to which the JSON data will be stored. This parameter can denote a file name (without extension) or a directory. See the " + PARAM_FILE_OUTPUT + " parameter which specifies whether the output should be currentBatchSize into large files - one document per line, one file per thread - or into a directory - one document per file. The files or directory will be created if they does not exist, including all parent directories. All files will be overwritten.")
    private File outputDest;
    @ConfigurationParameter(name = PARAM_GZIP, mandatory = false)
    private Boolean gzip;
    @ConfigurationParameter(name = PARAM_FILE_OUTPUT, description = "This boolean parameter determines whether a single file (parameter set to 'true') or a directory of files (parameter set to 'false') will be output to the location given with " + PARAM_OUTPUT_DEST + ". File output supports multithreading on the same machine through  ")
    private Boolean fileMode;
    private BufferedWriter bw;
    private String pathname;
    private int currentBatchSize;
    private int batchNum;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        outputDest = new File((String) aContext.getConfigParameterValue(PARAM_OUTPUT_DEST));
        gzip = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_GZIP)).orElse(false);
        fileMode = (Boolean) aContext.getConfigParameterValue(PARAM_FILE_OUTPUT);

        if (!outputDest.exists()) {
            if (!fileMode)
                outputDest.mkdirs();
            else {
                final File parentFile = outputDest.getParentFile();
                if (parentFile != null && !parentFile.exists())
                    parentFile.mkdirs();
            }
        }
        if (fileMode) {
            try {
                createFileOutputStream();
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        }
        currentBatchSize = 0;
        batchNum = 1;
        log.info("{}: {}", PARAM_OUTPUT_DEST, outputDest.getAbsolutePath());
        log.info("{}: {}", PARAM_GZIP, gzip);
        log.info("{}: {}", PARAM_FILE_OUTPUT, fileMode);
    }

    private void createFileOutputStream() throws IOException {
        synchronized (JsonWriter.class) {
            pathname = outputDest.getAbsolutePath() + "-" + getHostName() + "-" + ++writerNumber + "-" + batchNum + ".json";
            if (gzip)
                pathname += ".gz";
        }
        try {
            OutputStream os = new FileOutputStream(pathname);
            if (gzip)
                os = new GZIPOutputStream(os);
            bw = new BufferedWriter(new OutputStreamWriter(os));
        } catch (IOException e) {
            log.error("Could not access output destination {} for writing", pathname, e);
            throw e;
        }
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

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        try {
            Document singleDocument = convertCasToDocument(aJCas);
            if (singleDocument != null && !singleDocument.isEmpty())
                documentBatch.add(singleDocument);

            List<Document> documents = convertCasToDocuments(aJCas);
            if (documents != null) {
                for (Document document : documents)
                    documentBatch.add(document);
            }
        } catch (Throwable t) {
            log.error("Error occurred", t);
            throw t;
        }
    }

    private void writeDocumentBatch() throws AnalysisEngineProcessException {
        if (!fileMode) {
            // Write one file for each document into the given directory
            log.info("Writing current batch of {} JSONized documents to output directory {}", documentBatch.size(),
                    outputDest.getAbsolutePath());
            try {
                for (Document document : documentBatch) {
                    String id = document.getId();
                    String filepath = outputDest.getAbsolutePath() + File.separator + id + ".json";
                    if (gzip)
                        filepath += ".gz";
                    try (OutputStream os = gzip ? new GZIPOutputStream(new FileOutputStream(filepath))
                            : new FileOutputStream(filepath)) {
                        String json = gson.toJson(document);
                        IOUtils.write(json, os, "UTF-8");
                    }
                }
            } catch (IOException e) {
                log.error("Error while writing to {}", outputDest, e);
                throw new AnalysisEngineProcessException(e);
            }
        } else {
            // Add the documents to the single file, one per line
            try {
                // We write multiple batches of files so that they don't grow too large
                if (currentBatchSize > 1000000) {
                    bw.close();
                    ++batchNum;
                    createFileOutputStream();
                }
                for (Document document : documentBatch) {
                    String json = gson.toJson(document);
                    bw.write(json);
                    bw.newLine();
                }
                bw.flush();
            } catch (IOException e) {
                log.error("Error while writing to {}", pathname, e);
                throw new AnalysisEngineProcessException(e);
            }
            currentBatchSize += documentBatch.size();
        }
        documentBatch.clear();
    }

    @Override
    public void batchProcessComplete() throws AnalysisEngineProcessException {
        try {
            writeDocumentBatch();
            super.batchProcessComplete();
        } catch (Throwable throwable) {
            log.error("Error occurred", throwable);
            throw throwable;
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        try {
            writeDocumentBatch();
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    log.error("Could not close writer to the current output file", e);
                    throw new AnalysisEngineProcessException(e);
                }
            }
            super.collectionProcessComplete();
        } catch (Throwable t) {
            log.error("Error occurred", t);
            throw t;
        }
    }

}
