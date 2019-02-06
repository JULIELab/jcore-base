/**
 * TXTConsumer.java
 * <p>
 * Copyright (c) 2009, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * <p>
 * Author: buyko
 * <p>
 * Current version: //TODO insert current version number
 * Since version:   //TODO insert version number of first appearance of this class
 * <p>
 * Creation date: 13.05.2009
 * <p>
 * //TODO insert short description
 **/

package de.julielab.jcore.consumer.txt;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import de.julielab.java.utilities.FileUtilities;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.POSTag;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;

public class SentenceTokenConsumer extends JCasAnnotator_ImplBase {

    public static final String PARAM_OUTPUT_DIR = "outDirectory";
    public static final String PARAM_DELIMITER = "delimiter";
    public static final String PARAM_LOWERCASE = "lowercase";
    public static final String PARAM_MODE = "mode";
    public static final String PARAM_GZIP = "gzip";
    public static final String PARAM_ZIP_ARCHIVE = "zipArchive";
    public static final String PARAM_ZIP_MAX_SIZE = "maxZipSize";
    public static final String PARAM_ZIP_PREFIX = "zipFilePrefix";
    private static final Logger LOGGER = LoggerFactory.getLogger(SentenceTokenConsumer.class);
    private final static String DEFAULT_DELIMITER = "";
    private final static boolean DEFAULT_PARAM_POS_TAG = false;
    private final byte[] linesepBytes = System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8);
    int docs = 0;
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR, description = "The directory where to write the text files to.")
    private File directory;
    @ConfigurationParameter(name = PARAM_DELIMITER, mandatory = false, description = "If this parameter is given, each token will have its part of speech tag appended where the PoS tag is delimited from the token by the string given with this parameter.")
    private String delimiter;
    @ConfigurationParameter(name = PARAM_LOWERCASE, mandatory = false, defaultValue = "false", description = "If set to true, this parameter causes all written text output to be lowercased. Defaults to false.")
    private Boolean lowercase;
    @ConfigurationParameter(name = PARAM_MODE, mandatory = false, description = "Possible values: TOKEN and DOCUMENT. The first prints out tokens with one sentence per line, the second just prints out the CAS document text without changing it in any way.")
    private Mode mode;
    @ConfigurationParameter(name = PARAM_GZIP, mandatory = false, defaultValue = "false", description = "If set to true, the output files are stored in the GZIP format. The .gz extension is automatically appended. Defaults to false.")
    private Boolean gzip;
    @ConfigurationParameter(name = PARAM_ZIP_ARCHIVE, mandatory = false, defaultValue = "false", description = "If set to true, this parameter causes the output files to be stored in ZIP archives. The maximum size in terms of entries of each archive is given by the " + PARAM_ZIP_MAX_SIZE + " parameter and defaults to 10,000. The archive names are built using the prefix specified with the " + PARAM_ZIP_PREFIX + " parameter followed by a serially added number and the host name.")
    private Boolean zip;
    @ConfigurationParameter(name = PARAM_ZIP_MAX_SIZE, mandatory = false, defaultValue = "10000", description = "If the parameter " + PARAM_ZIP_ARCHIVE + " is set to true, ZIP archives will be written with a maximum number of entries to be specified with this paramter. Defaults to 10,000.")
    private Integer zipSize;
    @ConfigurationParameter(name = PARAM_ZIP_PREFIX, mandatory = false, defaultValue = "TXTConsumerArchive", description = "Specifies the base name for ZIP archives that are created in case the " + PARAM_ZIP_ARCHIVE + " parameter is enabled.")
    private String zipFilePrefix;
    private boolean addPOSTAG;
    private OutputStream currentArchive;
    private int archiveNumber = 1;
    private int currentArchiveSize = 0;

    @Override
    public void initialize(UimaContext aContext) {
        LOGGER.info("INITIALIZING TXT Consumer ...");
        String dirName = (String) aContext.getConfigParameterValue(PARAM_OUTPUT_DIR);
        directory = new File(dirName);
        if (!directory.exists()) {
            directory.mkdir();
        }
        LOGGER.info("Writing txt files to output directory '" + directory + "'");

        delimiter = (String) aContext.getConfigParameterValue(PARAM_DELIMITER);
        if (delimiter == null) {
            delimiter = DEFAULT_DELIMITER;
        }

        lowercase = (Boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_LOWERCASE)).orElse(false);

        gzip = (Boolean) aContext.getConfigParameterValue(PARAM_GZIP);
        if (gzip == null) {
            gzip = false;
        }

        if (aContext.getConfigParameterValue(PARAM_DELIMITER) != null) {
            addPOSTAG = true;
            LOGGER.info("Adding POSTags ...");
        } else {
            addPOSTAG = DEFAULT_PARAM_POS_TAG;
        }

        zip = (Boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ZIP_ARCHIVE)).orElse(false);
        zipSize = (Integer) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ZIP_MAX_SIZE)).orElse(10000);
        zipFilePrefix = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ZIP_PREFIX)).orElse("TXTConsumerArchive");

        String mode = (String) aContext.getConfigParameterValue(PARAM_MODE);
        if (mode == null) {
            mode = Mode.TOKEN.name();
        }
        this.mode = Mode.valueOf(mode);
    }

    private OutputStream createNextArchiveStream() throws IOException {
        final File outputfile = new File(directory.getCanonicalPath() + File.separator + zipFilePrefix + archiveNumber + "-" + getHostName() + "-" + Thread.currentThread().getName() + ".zip");
        if (outputfile.exists())
            throw new IllegalStateException("The next file to write for the current thread '"+Thread.currentThread().getName()+"' should be "+outputfile.getAbsolutePath()+", but this file does already exist.");
        currentArchive = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputfile)));
        ++archiveNumber;
        currentArchiveSize = 0;
        return currentArchive;
    }

    @Override
    public void process(JCas jcas) throws AnalysisEngineProcessException {
        LOGGER.trace("Processing next document ... ");
        try {
            String fileId = getDocID(jcas);
            if (fileId == null)
                fileId = new Integer(docs++).toString();

            if (mode == Mode.TOKEN) {
                FSIterator sentenceIterator = jcas.getAnnotationIndex(Sentence.type).iterator();

                AnnotationIndex tokenIndex = jcas.getAnnotationIndex(Token.type);

                ArrayList<String> sentences = new ArrayList<>();
                while (sentenceIterator.hasNext()) {
                    Sentence sentence = (Sentence) sentenceIterator.next();
                    FSIterator tokIterator = tokenIndex.subiterator(sentence);

                    String sentenceText = "";
                    while (tokIterator.hasNext()) {

                        if (addPOSTAG) {
                            sentenceText = returnWithPOSTAG(tokIterator, sentenceText);
                        } else {
                            sentenceText = returnWithoutPOSTAG(tokIterator, sentenceText);
                        }
                    }

                    sentences.add(sentenceText);

                }
                writeSentences2File(fileId, sentences);
            } else if (mode == Mode.DOCUMENT) {
                File outputFile = new File(directory.getCanonicalPath() + File.separator + fileId + ".txt" + (gzip ? ".gz" : ""));
                LOGGER.trace("Writing the verbatim CAS document text to {}", outputFile);
                writeSentences2File(fileId, Arrays.asList(jcas.getDocumentText()));
            }

        } catch (CASRuntimeException | CASException | IOException e) {
            LOGGER.error("Error while writing: ", e);
            throw new AnalysisEngineProcessException(e);
        }

    }

    private String returnWithoutPOSTAG(FSIterator tokIterator, String sentenceText) {

        Token token = (Token) tokIterator.next();

        String tokenText = token.getCoveredText();

        if (sentenceText.equals(""))
            sentenceText = tokenText;
        else {
            sentenceText = sentenceText + " " + tokenText;
        }
        return sentenceText;
    }

    private String returnWithPOSTAG(FSIterator tokIterator, String sentenceText) {
        Token token = (Token) tokIterator.next();

        String tokenText = token.getCoveredText();

        POSTag posTag = null;

        FSArray postags = token.getPosTag();
        if (postags != null && postags.size() > 0)
            posTag = (POSTag) postags.get(0);

        String postagText = posTag.getValue();

        if (sentenceText.equals(""))
            sentenceText = tokenText + delimiter + postagText;
        else {
            sentenceText = sentenceText + " " + tokenText + delimiter + postagText;
        }
        return sentenceText;
    }

    public String getDocID(JCas jcas) throws CASException {
        String docID = null;
        JFSIndexRepository indexes = jcas.getJFSIndexRepository();
        Iterator<?> headerIter = indexes.getAnnotationIndex(Header.type).iterator();
        while (headerIter.hasNext()) {
            Header h = (Header) headerIter.next();
            docID = h.getDocId();
        }
        return docID;
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        if (currentArchive != null) {
            try {
                currentArchive.close();
            } catch (IOException e) {
                throw new AnalysisEngineProcessException();
            }
        }
    }

    private void writeSentences2File(String fileId, List<String> sentences) throws IOException {
        OutputStream os = null;
        boolean zipContentWritten = false;
        try {
            File outputFile = new File(directory.getCanonicalPath() + File.separator + fileId + ".txt" + (gzip ? ".gz" : ""));
            os = zip ? currentArchive : FileUtilities.getOutputStreamToFile(outputFile);
            if (zip) {
                // Initialize the ZIP output stream if necessary
                if (os == null)
                    os = createNextArchiveStream();
                try {
                    ((ZipOutputStream) os).putNextEntry(new ZipEntry(outputFile.getName()));
                    zipContentWritten = true;
                } catch (ZipException e) {
                    if (e.getMessage().contains("duplicate")) {
                        LOGGER.warn("The file {} is already present in the current ZIP archive. Thus, the current file is omitted.", outputFile.getName());
                    } else {
                        throw e;
                    }
                }
            }
            if (!zip || zipContentWritten) {
                // Write the actual data to the stream
                for (String text : sentences) {
                    final byte[] bytes = lowercase ? text.toLowerCase().getBytes(StandardCharsets.UTF_8) : text.getBytes(StandardCharsets.UTF_8);
                    os.write(bytes, 0, bytes.length);
                    os.write(linesepBytes, 0, linesepBytes.length);
                }
            }
        } finally {
            if (zipContentWritten) {
                ((ZipOutputStream) os).closeEntry();
                ++currentArchiveSize;
                if (currentArchiveSize >= zipSize) {
                    os.close();
                    createNextArchiveStream();
                }
            } else if (!zip) {
                os.close();
            }
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

    private enum Mode {
        TOKEN, DOCUMENT
    }
}
