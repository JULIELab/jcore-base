package de.julielab.jcore.consumer.es;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.consumer.es.preanalyzed.Document;

public class JsonWriter extends AbstractCasToJsonConsumer {

    public static final String PARAM_OUTPUT_DEST = "OutputDestination";
    public static final String PARAM_GZIP = "GZIP";
    public static final String PARAM_FILE_OUTPUT = "FileOutput";
    public static final String PARAM_MAX_FILE_SIZE = "MaxFileSize";
    private static final Logger log = LoggerFactory.getLogger(AbstractCasToJsonConsumer.class);
    private List<Document> documentBatch = new ArrayList<>();
    @ConfigurationParameter(name = PARAM_OUTPUT_DEST, description = "The path to which the JSON data will be stored. This parameter can denote a file or a directory. See the " + PARAM_FILE_OUTPUT + " parameter which specifies whether the output should be written into one large file - one document per line - or into a directory - one document per file. The file or directory will be created if it does not exist, including all parent directories. In the directory case, the files in the directory will be overwritten. In the single file case, new documents will be appended, requiring the file to be deleted manually if desired.")
    private File outputDest;
    @ConfigurationParameter(name = PARAM_GZIP, mandatory = false)
    private Boolean gzip;
    @ConfigurationParameter(name = PARAM_FILE_OUTPUT, description = "This boolean parameter determines whether a single file (parameter set to 'true') or a directory of files (parameter set to 'false') will be output to the location given with " + PARAM_OUTPUT_DEST + ". File output supports multithreading on the same machine through  ")
    private Boolean fileMode;
    //@ConfigurationParameter(name = PARAM_MAX_FILE_SIZE, mandatory = false, description = "Only in effect when " + PARAM_FILE_OUTPUT + "is set to 'true'. Then, when the resulting file surpasses the given size, the current file is closed and a new one is opened. The files receive a number suffix. This can be helpful for large document collections to be written into GZIP format because in file mode only the complete file can be GZIPPed. Defaults to 0 which deactivates this function.", defaultValue = "0")
    //private Long maxFileSize;

    private RandomAccessFile randomAccessFile;
    private FileChannel channel;
    private static Object lock = new Object();


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        outputDest = new File((String) aContext.getConfigParameterValue(PARAM_OUTPUT_DEST));
        gzip = Optional.ofNullable((Boolean) aContext.getConfigParameterValue(PARAM_GZIP)).orElse(false);
        fileMode = (Boolean) aContext.getConfigParameterValue(PARAM_FILE_OUTPUT);
     //   maxFileSize = Optional.ofNullable((Long) aContext.getConfigParameterValue(PARAM_MAX_FILE_SIZE)).orElse(0L);
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
                randomAccessFile = new RandomAccessFile(outputDest, "rwd");
                channel = randomAccessFile.getChannel();
            } catch (FileNotFoundException e) {
                log.error("Could not access output destination {} for writing", outputDest, e);
                throw new ResourceInitializationException(e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.info("{}: {}", PARAM_OUTPUT_DEST, outputDest.getAbsolutePath());
        log.info("{}: {}", PARAM_GZIP, gzip);
        log.info("{}: {}", PARAM_FILE_OUTPUT, fileMode);
        //log.info("{}: {}", PARAM_MAX_FILE_SIZE, maxFileSize);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Document singleDocument = convertCasToDocument(aJCas);
        if (singleDocument != null && !singleDocument.isEmpty())
            documentBatch.add(singleDocument);

        List<Document> documents = convertCasToDocuments(aJCas);
        if (documents != null) {
            for (Document document : documents)
                documentBatch.add(document);
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
                throw new AnalysisEngineProcessException(e);
            }
        } else {
            synchronized (lock) {
                // Add the documents to the single file, one per line
                ByteBuffer newlineBuf = ByteBuffer.wrap(System.getProperty("line.separator").getBytes());
                try {
                    for (Document document : documentBatch) {
                        String json = gson.toJson(document);
                        ByteBuffer buffer = ByteBuffer.wrap(json.getBytes());
                        try (FileLock ignored = channel.lock()) {
                            randomAccessFile.seek(outputDest.length());
                            while (buffer.hasRemaining())
                                channel.write(buffer);
                            newlineBuf.rewind();
                            while(newlineBuf.hasRemaining())
                                channel.write(newlineBuf);
                        }
                    }
                } catch (IOException e) {
                    throw new AnalysisEngineProcessException(e);
                }
            }
        }
        documentBatch.clear();
    }

    @Override
    public void batchProcessComplete() throws AnalysisEngineProcessException {
        writeDocumentBatch();
        super.batchProcessComplete();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        writeDocumentBatch();
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                log.error("Could not close channel to the current output file", e);
                throw new AnalysisEngineProcessException(e);
            }
        }
        super.collectionProcessComplete();
    }

}
