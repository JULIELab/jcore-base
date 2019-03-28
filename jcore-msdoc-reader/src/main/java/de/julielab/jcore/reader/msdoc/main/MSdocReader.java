/**
 * MSdocReader.java
 *
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 *
 * @author Christina Lohr
 *
 * Current version: 1.2
 * Since version:   1.1
 *
 * Creation date: 11.04.2017
 * Update: 17.08.2017
 *
 * A UIMA <code>CollectionReader</code> that reads in simple text from a file.
 * Derived form Apache UIMA example <code>FileSystemCollectionReader</code>.
 **/

package de.julielab.jcore.reader.msdoc.main;

import de.julielab.jcore.types.pubmed.Header;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MSdocReader extends CollectionReader_ImplBase {
	public static final String DIRECTORY_INPUT = "InputDirectory";
	public static final String FILENAME_AS_DOC_ID = "UseFilenameAsDocId";
	public static final String ALLOWED_FILE_EXTENSIONS = "AllowedFileExtensions";
	public static final String DIRECTORY_SUBDIRS = "ReadSubDirs";

	private ArrayList<File> files;

	private int fileIndex;

	private File inputDirectory;
	private boolean useFilenameAsDocId;
	private boolean useSubDirs;

	/**
	 * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
	 */
	@Override
	public void initialize() throws ResourceInitializationException {
		inputDirectory = new File(((String) getConfigParameterValue(DIRECTORY_INPUT)).trim());

		Boolean filenameAsDocId = (Boolean) getConfigParameterValue(FILENAME_AS_DOC_ID);

		if (null == filenameAsDocId) {
			useFilenameAsDocId = false;
		} else {
			useFilenameAsDocId = filenameAsDocId;
		}

		String[] allowedExtensionsArray = (String[]) getConfigParameterValue(ALLOWED_FILE_EXTENSIONS);

		final Set<String> allowedExtensions = new HashSet<>();

		if (null != allowedExtensionsArray) {
			for (int i = 0; i < allowedExtensionsArray.length; i++) {
				allowedExtensions.add(allowedExtensionsArray[i]);
			}
		}

		Boolean subdir = (Boolean) getConfigParameterValue(DIRECTORY_SUBDIRS);

		if (null == subdir) {
			useSubDirs = false;
		} else {
			useSubDirs = subdir;
		}

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

		File file = files.get(fileIndex++);

		ReadSingleMSdoc.INPUT_FILE = file.getPath();
		ReadSingleMSdoc.doc2Text();

		/**
		 * Read the doc-file:
		 * 
		 * We implemented different ways to convert the *.doc-files, look into
		 * ReadSingleMSdoc.doc2Text(), this reads the structure of the
		 * *.doc-file.
		 */

		// String textOnly = ReadSingleMSdoc.CONTENT_NORMAL;
		String textWithMarkedTables = ReadSingleMSdoc.CONTENT_TAB_MARKED;
		// String labParams = ReadSingleMSdoc.LAB_PARAMS_NORMAL;
		// String textHMTL = ReadSingleMSdoc.CONTENT_HTML;

		jcas.setDocumentText(textWithMarkedTables);

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
			header.setDocId(filename);
			header.addToIndexes();
		}

		// writeArtifactIntoTXT(file.getPath(), textOnly);
//		writeArtifactIntoTXT(file.getPath(), textWithMarkedTables);
		// writeArtifactIntoTXT(file.getPath(), textHTML);

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

	private String[] createFileListByType(File inputDirectory, final Set<String> allowedExtensions) throws IOException {
		String[] path = new File(inputDirectory.getPath()).list();

		for (int i = 0; i < path.length; i++) {
			File file = new File(inputDirectory.getAbsolutePath() + "/" + path[i]);

			String CurrentExtension = path[i].substring(path[i].lastIndexOf('.') + 1);
			if (allowedExtensions.contains(CurrentExtension)) {
				files.add(file);
			}

			if (useSubDirs && file.isDirectory()) {
				createFileListByType(file, allowedExtensions);
			}
		}

		return path;
	}

	private static void writeArtifactIntoTXT(String file, String text) {
		file = file.substring(0, file.length() - 4) + ".txt";

		File artifactFile = new File(file); // new File(OUTDIR + File.separator
											// + fileName);
		try (FileOutputStream outputStream = new FileOutputStream(artifactFile)) {
			outputStream.write(text.getBytes());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}