package de.julielab.jules.reader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class XmiCollectionReader extends CollectionReader_ImplBase {

	/**
	 * Name of configuration parameter that must be set to the path of a directory containing the XMI files.
	 */
	public static final String PARAM_INPUTDIR = "InputDir";

	/**
	 * Name of configuration parameter which indicates if subdirectories (recursively) of PARAM_INPUTDIR are to be
	 * searched for XMI files as well.
	 */
	public static final String PARAM_RECURSIVE = "SearchRecursively";

	private ArrayList<File> mFiles;
	private int mCurrentIndex;

	@ConfigurationParameter(
			name = PARAM_RECURSIVE,
			mandatory = false,
			defaultValue = "false",
			description = "If set to true, also searches subdirectories of the input directory for XMI files to read.")
	private boolean searchRecursively;

	@ConfigurationParameter(
			name = PARAM_INPUTDIR,
			mandatory = true,
			description = "File path to the directory to read XMI files from.")
	private String file;

	/**
	 * @see com.ibm.uima.collection.CollectionReader_ImplBase#initialize()
	 */
	public void initialize() throws ResourceInitializationException {

		file = (String) getConfigParameterValue(PARAM_INPUTDIR);
		searchRecursively = false;
		if (getConfigParameterValue(PARAM_RECURSIVE) != null)
			searchRecursively = (Boolean) getConfigParameterValue(PARAM_RECURSIVE);

		File directory = new File((file).trim());
		mCurrentIndex = 0;

		// if input directory does not exist or is not a directory, throw
		// exception
		if (!directory.exists() || !directory.isDirectory()) {
			throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND, new Object[] {
					PARAM_INPUTDIR, this.getMetaData().getName(), directory.getPath() });
		}

		// get list of .xmi files in the specified directory
		mFiles = new ArrayList<File>();
		File[] mDirectories = directory.listFiles(new FileFilter() {
			public boolean accept(File arg0) {
				return arg0.isDirectory();
			}
		});
		readXmiFilesInDir(mFiles, directory);
		if (searchRecursively) {
			for (File subDir : mDirectories)
				readXmiFilesInDir(mFiles, subDir);
		}
	}

	private void readXmiFilesInDir(List<File> files, File directory) {
		File[] xmiFiles = directory.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String fileName = pathname.getName();
				return fileName.endsWith("xmi") || fileName.endsWith("gz")
						|| fileName.endsWith("gzip")
						|| fileName.endsWith("zip");
			}
		});
		files.addAll(Arrays.asList(xmiFiles));
	}

	/**
	 * @see com.ibm.uima.collection.CollectionReader#hasNext()
	 */
	public boolean hasNext() {
		return mCurrentIndex < mFiles.size();
	}

	/**
	 * @see com.ibm.uima.collection.CollectionReader#getNext(com.ibm.uima.cas.CAS)
	 */
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		File currentFile = (File) mFiles.get(mCurrentIndex++);
		InputStream is = new FileInputStream(currentFile);
		String fileName = currentFile.getName();
		// check if the files in zipped in any way and create an
		// appropriate InputStream
		if (fileName.endsWith("gz") || fileName.endsWith("gzip"))
			is = new GZIPInputStream(is);
		else if (fileName.endsWith("zip")) {
			is = new ZipInputStream(is);
			((ZipInputStream) is).getNextEntry();
		} // else: do nothing, the original FileInputStream suffices.

		try {
			XmiCasDeserializer.deserialize(is, aCAS);
		} catch (SAXException e) {
			throw new CollectionException(e);
		} finally {
			is.close();
		}
	}

	/**
	 * @see com.ibm.uima.collection.base_cpm.BaseCollectionReader#close()
	 */
	public void close() throws IOException {
	}

	/**
	 * @see com.ibm.uima.collection.base_cpm.BaseCollectionReader#getProgress()
	 */
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(mCurrentIndex, mFiles.size(), Progress.ENTITIES) };
	}

}
