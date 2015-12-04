/**
 * FileReader.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: muehlhausen
 *
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 27.08.2007
 *
 * A UIMA <code>CollectionReader</code> that reads in simple text from a file. Derived form Apache UIMA example <code>FileSystemCollectionReader</code>.
 **/

package de.julielab.jcore.reader.file.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import de.julielab.jcore.types.Date;
import de.julielab.jcore.types.pubmed.Header;

public class FileReader extends CollectionReader_ImplBase {

    public static final String DIRECTORY_INPUT = "InputDirectory";
    public static final String FILENAME_AS_DOC_ID = "UseFilenameAsDocId";
    public static final String PUBLICATION_DATES_FILE = "PublicationDatesFile";

    private ArrayList<File> files;

    private int fileIndex;

    private boolean useFilenameAsDocId;
    private File publicationDatesFile;

    /**
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    @Override
    public void initialize() throws ResourceInitializationException {

        File inputDirectory = new File(((String) getConfigParameterValue(DIRECTORY_INPUT)).trim());
        if (getConfigParameterValue(PUBLICATION_DATES_FILE) != null) {
            publicationDatesFile = new File(((String) getConfigParameterValue(PUBLICATION_DATES_FILE)).trim());
        }
        Boolean filenameAsDocId = (Boolean) getConfigParameterValue(FILENAME_AS_DOC_ID);
        if (null == filenameAsDocId) {
            useFilenameAsDocId = false;
        } else {
            useFilenameAsDocId = filenameAsDocId;
        }

        fileIndex = 0;

        if (!inputDirectory.exists() || !inputDirectory.isDirectory()) {
            throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
                    new Object[] { DIRECTORY_INPUT, this.getMetaData().getName(), inputDirectory.getPath() });
        }

        files = new ArrayList<File>();
        File[] f = inputDirectory.listFiles();
        for (int i = 0; i < f.length; i++) {
            if (!f[i].isDirectory()) {
                files.add(f[i]);
            }
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

        String text = FileUtils.readFileToString(file, "UTF-8");
        // String text = FileUtils.file2String(file);
        // put document in CAS
        jcas.setDocumentText(text);

        if (useFilenameAsDocId) {
            String filename = file.getName();
            int extDotIndex = filename.lastIndexOf('.');
            if (extDotIndex > 0) {
                filename = filename.substring(0, extDotIndex);
            }
            int extUnderScoreIndex = filename.lastIndexOf('_');
            if (extUnderScoreIndex > 0) {
                filename = filename.substring(0, extUnderScoreIndex);
            }

            Header header = new Header(jcas);

            // set ID
            header.setDocId(filename);

            // set publication date
            addDateForID(header, jcas, filename);

            header.addToIndexes();
        }
    }

    /**
     * if publicationDatesFile is available, this method retrieves the publication date for the given article id and
     * adds it to the passed header
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
        return new Progress[] { new ProgressImpl(fileIndex, files.size(), Progress.ENTITIES) };
    }
}