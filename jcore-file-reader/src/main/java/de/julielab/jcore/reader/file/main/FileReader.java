/**
 * FileReader.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: muehlhausen
 * <p>
 * Current version: 1.0
 * Since version:   1.0
 * <p>
 * Creation date: 27.08.2007
 * <p>
 * A UIMA <code>CollectionReader</code> that reads in simple text from a file. Derived form Apache UIMA example <code>FileSystemCollectionReader</code>.
 **/

package de.julielab.jcore.reader.file.main;

import de.julielab.java.utilities.FileUtilities;
import de.julielab.java.utilities.IOStreamUtilities;
import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.*;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileReader extends CollectionReader_ImplBase {

    /**
     *
     */
    public static final String DIRECTORY_INPUT = "InputDirectory";
    /**
     *
     */
    public static final String FILENAME_AS_DOC_ID = "UseFilenameAsDocId";
    /**
     *
     */
    public static final String PUBLICATION_DATES_FILE = "PublicationDatesFile";
    /**
     *
     */
    public static final String ALLOWED_FILE_EXTENSIONS = "AllowedFileExtensions";
    /**
     *
     */
    public static final String SENTENCE_PER_LINE = "SentencePerLine";
    /**
     *
     */
    public static final String TOKEN_BY_TOKEN = "TokenByToken";
    /**
     *
     */
    public static final String FILE_NAME_SPLIT_UNDERSCORE = "FileNameSplitUnderscore";
    /**
     *
     */
    public static final String DIRECTORY_SUBDIRS = "ReadSubDirs";
    /**
     *
     */
    public static final String DIRECTORY_ORIG_FILES = "OriginalFolder";
    /**
     *
     */
    public static final String ORIG_FILES_EXT = "OriginalFileExt";

    public static final String REMOVE_FILE_NAME_EXTENSION_FOR_DOC_ID = "RemoveFileNameExtensionForDocId";

    private ArrayList<File> files;

    private int fileIndex;

    @ConfigurationParameter(name = DIRECTORY_INPUT)
    private File inputDirectory;
    @ConfigurationParameter(name = FILENAME_AS_DOC_ID, mandatory = false)
    private boolean useFilenameAsDocId;
    @ConfigurationParameter(name = PUBLICATION_DATES_FILE, mandatory = false)
    private File publicationDatesFile;
    @ConfigurationParameter(name = SENTENCE_PER_LINE, mandatory = false)
    private boolean sentencePerLine;
    @ConfigurationParameter(name = TOKEN_BY_TOKEN, mandatory = false)
    private boolean tokenByToken;
    @ConfigurationParameter(name = FILE_NAME_SPLIT_UNDERSCORE, mandatory = false)
    private boolean fileNameSplitUnderscore;
    @ConfigurationParameter(name = ALLOWED_FILE_EXTENSIONS, mandatory = false)
    private String[] allowedExtensionsArray;
    @ConfigurationParameter(name = DIRECTORY_SUBDIRS, mandatory = false)
    private boolean useSubDirs;
    @ConfigurationParameter(name = DIRECTORY_ORIG_FILES, mandatory = false)
    private File origFolder;
    @ConfigurationParameter(name = ORIG_FILES_EXT, mandatory = false)
    private String origFileExt;
    @ConfigurationParameter(name = REMOVE_FILE_NAME_EXTENSION_FOR_DOC_ID, mandatory = false, defaultValue = "true")
    private boolean removeFileNameExtensionForDocId;

    /**
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    @Override
    public void initialize() throws ResourceInitializationException {

        inputDirectory = new File(((String) getConfigParameterValue(DIRECTORY_INPUT)).trim());
        if (getConfigParameterValue(PUBLICATION_DATES_FILE) != null) {
            publicationDatesFile = new File(((String) getConfigParameterValue(PUBLICATION_DATES_FILE)).trim());
        }

        Boolean spl = (Boolean) getConfigParameterValue(SENTENCE_PER_LINE);
        if (spl == null) {
            sentencePerLine = false;
        } else {
            sentencePerLine = spl;
        }

        Boolean tokspl = (Boolean) getConfigParameterValue(TOKEN_BY_TOKEN);
        if (null == tokspl) {
            tokenByToken = false;
        } else {
            tokenByToken = tokspl;
        }

        Boolean fnsu = (Boolean) getConfigParameterValue(FILE_NAME_SPLIT_UNDERSCORE);
        if (null == fnsu) {
            fileNameSplitUnderscore = false;
        } else {
            fileNameSplitUnderscore = fnsu;
        }

        Boolean filenameAsDocId = (Boolean) getConfigParameterValue(FILENAME_AS_DOC_ID);
        if (null == filenameAsDocId) {
            useFilenameAsDocId = true;
        } else {
            useFilenameAsDocId = filenameAsDocId;
        }
        removeFileNameExtensionForDocId = Optional.ofNullable((Boolean) getConfigParameterValue(REMOVE_FILE_NAME_EXTENSION_FOR_DOC_ID)).orElse(true);

        allowedExtensionsArray = (String[]) getConfigParameterValue(ALLOWED_FILE_EXTENSIONS);
        final Set<String> allowedExtensions = new HashSet<>();
        if (null != allowedExtensionsArray) {
            for (int i = 0; i < allowedExtensionsArray.length; i++) {
                String allowedExtension = allowedExtensionsArray[i];
                allowedExtensions.add(allowedExtension);
            }
        }

        Boolean subdir = (Boolean) getConfigParameterValue(DIRECTORY_SUBDIRS);
        if (null == subdir) {
            useSubDirs = false;
        } else {
            useSubDirs = subdir;
        }

        String sentfoo = (String) getConfigParameterValue(DIRECTORY_ORIG_FILES);
        if (null == sentfoo) {
            origFolder = null;
        } else {
            origFolder = new File(sentfoo.trim());
        }

        String sentfile_ext = (String) getConfigParameterValue(ORIG_FILES_EXT);
        if (null == sentfile_ext) {
            origFileExt = "txt";
        } else {
            origFileExt = sentfile_ext;
            if (sentfile_ext.startsWith(".")) {
                origFileExt = sentfile_ext.substring(1);
            }
        }

        if (!inputDirectory.exists())
            throw new ResourceInitializationException(AnnotatorConfigurationException.RESOURCE_NOT_FOUND, new Object[]{inputDirectory.getAbsolutePath()});

        fileIndex = 0;
        files = new ArrayList<File>();

        try {
            createFileListByType(inputDirectory, allowedExtensions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#hasNext()
     */
    @Override
    public boolean hasNext() {
        return fileIndex < files.size();
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
     */
    @Override
    public void getNext(CAS aCAS) throws IOException, CollectionException {
        JCas jcas;
        try {
            jcas = aCAS.getJCas();
        } catch (CASException e) {
            throw new CollectionException(e);
        }

        // open input stream to file
        File file = files.get(fileIndex++);

        String text = IOStreamUtilities.getStringFromInputStream(FileUtilities.getInputStreamFromFile(file));

        Pattern nws = Pattern.compile("[^\\s]+", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

        String origText = null;
        if (origFolder != null) {
            File origFile = new File(origFolder, getFileName(file, true) + "." + origFileExt);
            origText = IOStreamUtilities.getStringFromInputStream(FileUtilities.getInputStreamFromFile(origFile));
        }

        // sentence per line mode
        if (sentencePerLine) {
            BufferedReader rdr = new BufferedReader(new StringReader(text));
            List<String> lines = new ArrayList<String>();
            List<Integer> start = new ArrayList<Integer>();
            List<Integer> end = new ArrayList<Integer>();
            Integer tmp = 0;
            String line;
            while ((line = rdr.readLine()) != null) {
                if (!Pattern.matches("\\s*", line)) {
                    lines.add(line);
                    start.add(tmp);
                    end.add(tmp + line.length());
                }
                tmp += (line.length() + 1);
            }
            rdr.close();

            int index_tmp = 0;
            Optional<String> newLine;
            for (Integer i = 0; i < lines.size(); i++) {
                boolean addSent2index = true;
                Sentence sent = new Sentence(jcas);
                if (origText != null) {
                    newLine = Stream
                            .of(lines.get(i).split("\\s+"))
                            .map(x -> Pattern.quote(x))
                            .reduce((x, y) -> x + "\\s*" + y);
                    Pattern p = Pattern.compile(newLine.get(), Pattern.UNICODE_CHARACTER_CLASS);
                    Matcher m = p.matcher(origText);
                    if (m.find(index_tmp)) {
                        int newStart = m.start();
                        int newEnd = m.end();
                        index_tmp = m.end() + 1;
                        sent.setBegin(newStart);
                        sent.setEnd(newEnd);
                    } else {
                        addSent2index = false;
                    }
                } else {
                    sent.setBegin(start.get(i));
                    sent.setEnd(end.get(i));
                }
                sent.setComponentId(this.getClass().getName() + " : Sentence per Line Mode");
                if (addSent2index) {
                    sent.addToIndexes();
                }
            }
        }
        //token by token mode
        if (tokenByToken) {
            List<String> tokensList = new ArrayList<>();
            List<Integer> tokStart = new ArrayList<>();
            List<Integer> tokEnd = new ArrayList<>();


            Integer numberOfTokens = 0;
            Matcher m = nws.matcher(text);
            while (m.find()) {
                String token = m.group();
                int start = m.start();
                int end = m.end();
                tokensList.add(token);
                tokStart.add(start);
                tokEnd.add(end);
                numberOfTokens++;
            }


            int index_tmp = 0;
            for (Integer j = 0; j < tokensList.size(); j++) {
                boolean addToken2index = true;
                Token token = new Token(jcas);
                if (origText != null) {
                    String tok = tokensList.get(j);
                    int newStart = origText.indexOf(tok, index_tmp);
                    int newEnd = newStart + tok.length();
                    index_tmp = newEnd;
                    token.setBegin(newStart);
                    token.setEnd(newEnd);
                } else {
                    token.setBegin(tokStart.get(j));
                    token.setEnd(tokEnd.get(j));
                }
                token.setComponentId(this.getClass().getName() + " : Tokenized Mode");
                if (addToken2index) {
                    token.addToIndexes();
                }
            }
        }

        // put document in CAS
        if (origText != null) {
            jcas.setDocumentText(origText);
        } else {
            jcas.setDocumentText(text);
        }

        if (useFilenameAsDocId) {

            String filename = getFileName(file, removeFileNameExtensionForDocId);

            Header header = new Header(jcas);

            // set ID
            header.setDocId(filename);

            // set publication date
            addDateForID(header, jcas, filename);

            header.addToIndexes();
        }
    }

    /**
     * if publicationDatesFile is available, this method retrieves the
     * publication date for the given article id and adds it to the passed
     * header
     *
     * @param header
     * @param id
     */
    private void addDateForID(Header header, JCas jCas, String id) {
        if (publicationDatesFile != null && publicationDatesFile.exists() && publicationDatesFile.isFile()) {
            // read the file and search for the given id
            try {
                BufferedReader br = new BufferedReader(new java.io.FileReader(publicationDatesFile));
                String line = "";
                while ((line = br.readLine()) != null) {
                    // split line at whitespace
                    String[] tokens = line.split("\\s+");
                    if (tokens.length == 2 && tokens[0].equals(id) && tokens[1].length() == 7) {
                        Date pubDate = new Date(jCas);
                        // extract year and month from token of format 2002-01
                        int year = 0;
                        int month = 0;
                        try {
                            year = Integer.parseInt(tokens[1].substring(0, 4));
                            month = Integer.parseInt(tokens[1].substring(5));
                        } catch (NumberFormatException e) {
                        }
                        if (month != 0) {
                            pubDate.setMonth(month);
                        }
                        if (year != 0) {
                            pubDate.setYear(year);
                            pubDate.addToIndexes();
                            // TODO, why doesn't this work??
                            // header.setDate(pubDate);
                        }
                        break;
                    }
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
     */
    @Override
    public void close() throws IOException {
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
     */
    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(fileIndex, files.size(), Progress.ENTITIES)};
    }

    private void createFileListByType(File inputDirectory, final Set<String> allowedExtensions) throws IOException {
        Files.walk(inputDirectory.toPath(), useSubDirs ? Integer.MAX_VALUE : 1, FileVisitOption.FOLLOW_LINKS)
                .filter(p -> { if (allowedExtensions.isEmpty()) return true; for (String ext : allowedExtensions) if (p.toString().endsWith(ext)) return true; return false;})
                .map(Path::toFile)
                .filter(File::isFile)
                .forEach(files::add);
    }

    private String getFileName(File fi, boolean removeExtension) {
        String filename = fi.getName();
        int extDotIndex = filename.lastIndexOf('.');
        if (extDotIndex > 0) {
            filename = filename.substring(0, extDotIndex);
        }
        if (fileNameSplitUnderscore) {
            int extUnderScoreIndex = filename.lastIndexOf('_');
            if (extUnderScoreIndex > 0) {
                filename = filename.substring(0, extUnderScoreIndex);
            }
        }
        return filename;
    }


}