/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.consumer.xmi;

/** 
 * CasToXMIConsumer.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek, muehlhausen
 * 
 * Current version: 2.1.1
 * Since version:   0.1
 *
 * Creation date: Dec 14, 2006 
 * 
 * A consumer that writes the complete CAS to XMI using UIMA's cas serialize function.
 **/

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.*;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

/**
 * Write a XMI to a file.
 * 
 * @author tomanek, muehlhausen
 */
public class CasToXmiConsumer extends JCasAnnotator_ImplBase {

	private Logger LOGGER = LoggerFactory.getLogger(CasToXmiConsumer.class);

	public static final String PARAM_OUTPUTDIR = "OutputDirectory";
	public static final String CREATE_BATCH_SUBDIRS = "CreateBatchSubDirs";
	public static final String PARAM_COMPRESS = "Compress";
	public static final String PARAM_COMPRESS_SINGLE = "CompressSingle";
	public static final String PARAM_FILE_NAME_TYPE = "FileNameType";
	public static final String PARAM_FILE_NAME_FEATURE = "FileNameFeature";
	private static final String XMI_EXTENSION = ".xmi";
	private static final String GZIP_EXTENSION = ".gz";

	private final static String DEFAULT_FILE_NAME_TYPE = "de.julielab.jcore.types.Header";
	private final static String DEFAULT_FILE_NAME_FEATURE = "docId";
	private final static boolean DEFAULT_COMPRESS = false;
	private final static boolean DEFAULT_COMPRESS_SINGLE = false;
	private final static boolean DEFAULT_CREATE_BATCH_SUBDIRS = false;

	private static Set<Integer> randomNumbers = new HashSet<Integer>();

	@ConfigurationParameter(name = PARAM_OUTPUTDIR, mandatory = true)
	private File outputDir;
	@ConfigurationParameter(name = PARAM_COMPRESS, mandatory = false)
	private boolean compress;
	@ConfigurationParameter(name = PARAM_COMPRESS_SINGLE, mandatory = false)
	private boolean compressSingle;
	@ConfigurationParameter(name = CREATE_BATCH_SUBDIRS, mandatory = false)
	private boolean createBatchSubdirs;
	@ConfigurationParameter(name = PARAM_FILE_NAME_TYPE, mandatory = false)
	private String fileNameTypeName;
	@ConfigurationParameter(name = PARAM_FILE_NAME_FEATURE, mandatory = false)
	private String fileNameFeatureName;
	
	private File currentSubDir;
	private static AtomicInteger doc = new AtomicInteger();

	private ZipOutputStream zipOutStream;
	private BufferedOutputStream outStream;
	private boolean zipReady = false;

	// private SimpleDateFormat simpleDateFormat = new
	// SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		LOGGER.info("initializing CasToXmiConsumer...");
		outputDir = new File((String) aContext.getConfigParameterValue(PARAM_OUTPUTDIR));
		if (outputDir == null) {
			LOGGER.error("Mandatory parameter " + PARAM_OUTPUTDIR
					+ " is missing.");
			throw new ResourceInitializationException();
		}
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		LOGGER.info("Writing XMI files to output directory '" + outputDir + "'");

		if (aContext.getConfigParameterValue(CREATE_BATCH_SUBDIRS) != null) {
			createBatchSubdirs = (Boolean) aContext.getConfigParameterValue(CREATE_BATCH_SUBDIRS);
		} else {
			createBatchSubdirs = DEFAULT_CREATE_BATCH_SUBDIRS;
		}
		LOGGER.info("creating subdirectories / giant zip files for each batch: "
				+ createBatchSubdirs);

		fileNameTypeName = (String) aContext.getConfigParameterValue(PARAM_FILE_NAME_TYPE);
		if (fileNameTypeName == null) {
			fileNameTypeName = DEFAULT_FILE_NAME_TYPE;
		}

		fileNameFeatureName = (String) aContext.getConfigParameterValue(PARAM_FILE_NAME_FEATURE);
		if (fileNameFeatureName == null) {
			fileNameFeatureName = DEFAULT_FILE_NAME_FEATURE;
		}

		LOGGER.info("trying to read file name from " + fileNameTypeName + "."
				+ fileNameFeatureName);

		if (aContext.getConfigParameterValue(PARAM_COMPRESS_SINGLE) != null) {
			compressSingle = (Boolean) aContext.getConfigParameterValue(PARAM_COMPRESS_SINGLE);
		} else {
			compressSingle = DEFAULT_COMPRESS_SINGLE;
		}
		LOGGER.info("compressing XMIs in one batch in single gzip: "
				+ compressSingle);

		// if compressSingle is true, switch off 'compress' parameter and set up
		// giant zip file
		if (compressSingle) {
			compress = false;
			LOGGER.info("ignoring 'compress' parameter");
			zipReady = false; // new zip file is set up in process method
		} else {
			// get compress parameter
			if (aContext.getConfigParameterValue(PARAM_COMPRESS) != null) {
				compress = (Boolean) aContext.getConfigParameterValue(PARAM_COMPRESS);
			} else {
				compress = DEFAULT_COMPRESS;
			}
			LOGGER.info("compressing XMIs with gzip (only plays a role because CompressSingle is false): "
					+ compress);
			// prepare subdirectory
			if (createBatchSubdirs) {
				String subDirName;
				try {
					subDirName = getNewUniqueFileName();
				} catch (ResourceProcessException e) {
					throw new ResourceInitializationException(e);
				}
				currentSubDir = new File(outputDir, subDirName);
				currentSubDir.mkdirs();
				LOGGER.info("writing XMIs to subdir " + currentSubDir.getPath());
			}
		}
		doc.set(0); // counter for documents
	}

	private String getNewUniqueFileName() throws ResourceProcessException {
		// create unique index path
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOGGER.error(
					"getNewUniqueFileName() - could not create unique subdirectory name",
					e);
			throw new ResourceProcessException(e);
		}
		String newUniqueFileName = "xmis-" + hostName + "-" + pid + "-"
				+ createRandom();
		return newUniqueFileName;
	}

	private void setUpNewGiantZipFile() throws ResourceProcessException {
		String zipName = getNewUniqueFileName() + ".zip";
		LOGGER.info("creating giant zip file " + zipName);
		FileOutputStream fs;
		try {
			File zipFile = new File(outputDir, zipName);
			fs = new FileOutputStream(zipFile);
			LOGGER.debug("Preparing giant zip file " + zipFile.getPath());
		} catch (FileNotFoundException e) {
			LOGGER.error(
					"setUpNewGiantZipFile(): could not prepare output file", e);
			throw new ResourceProcessException(e);
		}
		CheckedOutputStream csum = new CheckedOutputStream(fs, new Adler32());
		zipOutStream = new ZipOutputStream(csum);
		outStream = new BufferedOutputStream(zipOutStream);
	}

	private synchronized int createRandom() {
		Random randomGenerator = new Random();
		int randomInt = randomGenerator.nextInt(1000000);
		while (randomNumbers.contains(randomInt)) {
			randomInt = randomGenerator.nextInt(1000000);
		}
		randomNumbers.add(randomInt);
		return randomInt;
	}

	/**
	 * @param jcas
	 *            a CAS which has been populated by the TAEs
	 * @throws ResourceProcessException
	 *             if there is an error in processing the Resource
	 */
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		doc.incrementAndGet();
		CAS aCAS;
		aCAS = jcas.getCas();
		StringBuilder outFileName = new StringBuilder();
		Type fileNameType = aCAS.getTypeSystem().getType(fileNameTypeName);
		if (fileNameType != null) {
			Feature fileNameFeature = fileNameType
					.getFeatureByBaseName(fileNameFeatureName);
			if (fileNameFeature != null) {
				// get filename from fileNameType.fileNameFeature
				JFSIndexRepository indexes = jcas.getJFSIndexRepository();
				FSIterator iter = indexes.getAllIndexedFS(fileNameType);
				if (iter.hasNext()) {
					FeatureStructure fs = (FeatureStructure) iter.next();
					try {
						String value = fs.getStringValue(fileNameFeature);
						if (value != null) {
							value = value.trim();
							if (!value.isEmpty()) {
								if (value.contains(File.separator)) {
									// In case the file name is a whole path,
									// just take the original filename
									String[] path = value.split(File.separator);
									outFileName.append(path[path.length - 1]);
								} else {
									outFileName.append(value);
								}
							}
						} else {
							LOGGER.debug("No feature value found of type.feature "
									+ fileNameTypeName
									+ "."
									+ fileNameFeatureName);
						}
					} catch (CASRuntimeException e) { // unschoen
						LOGGER.warn("Choose feature with String value!");
						try {
							throw new ResourceProcessException();
						} catch (ResourceProcessException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				} else {
					LOGGER.debug("No annotation found of type "
							+ fileNameTypeName);
				}
			} else {
				LOGGER.debug("No feature of type " + fileNameTypeName
						+ " found with name " + fileNameFeatureName);
			}
		} else {
			LOGGER.debug("No type found with name " + fileNameTypeName);
		}
		// if outFileName has not been set successfully, use number as file name
		if (outFileName.length() == 0) {
			outFileName.append(doc);
		}
		// add xmi extension
		outFileName.append(XMI_EXTENSION);
		// if compress is true, add gzip extension
		if (compress) {
			outFileName.append(GZIP_EXTENSION);
		}
		String fileName = outFileName.toString();
		try {
			writeXmi(jcas.getCas(), fileName);
			LOGGER.debug(" Wrote file " + fileName);
		} catch (IOException e) {
			try {
				throw new ResourceProcessException(e);
			} catch (ResourceProcessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (SAXException e) {
			try {
				throw new ResourceProcessException(e);
			} catch (ResourceProcessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (ResourceProcessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Serialize a CAS to a file in XMI format. If parameter compress is true,
	 * gzip the XMI. if parameter compressSingel is true, add CAS to giant zip
	 * file.
	 * 
	 * @param aCas
	 *            CAS to serialize
	 * @param fileName
	 *            output file
	 * @throws SAXException
	 * @throws ResourceProcessException
	 * @throws Exception
	 * @throws ResourceProcessException
	 */
	private void writeXmi(CAS aCas, String fileName) throws IOException,
			SAXException, ResourceProcessException {

		// write one big compressed file
		if (compressSingle) {
			if (!zipReady) {
				setUpNewGiantZipFile();
				zipReady = true;
			}
			zipOutStream.putNextEntry(new ZipEntry(fileName));
			XmiCasSerializer.serialize(aCas, outStream);
			outStream.flush();
		} else {
			File outFile;
			if (createBatchSubdirs) {
				outFile = new File(currentSubDir, fileName);
			} else {
				outFile = new File(outputDir, fileName);
			}
			if (compress) {
				GZIPOutputStream out = new GZIPOutputStream(
						new FileOutputStream(outFile));
				XmiCasSerializer.serialize(aCas, out);
				out.finish();
				out.close();
			} else {
				FileOutputStream out = new FileOutputStream(outFile);
				XmiCasSerializer.serialize(aCas, out);
				out.close();
			}
		}

	}

	public void batchProcessComplete(ProcessTrace processTrace)
			throws IOException, ResourceProcessException {
		if (createBatchSubdirs) {
			if (compressSingle) {
				// close stream
				try {
					outStream.close();
					zipReady = false;
				} catch (IOException e) {
					LOGGER.error(
							"batchProcessComplete() - problems closing the output stream",
							e);
					throw new IOException(e);
				}
			} else {
				// prepare subdirectory
				String subDirName;
				try {
					subDirName = getNewUniqueFileName();
					currentSubDir = new File(outputDir, subDirName);
					currentSubDir.mkdirs();
					LOGGER.info("writing XMIs to subdir "
							+ currentSubDir.getPath());
				} catch (ResourceProcessException e) {
					LOGGER.error("batchProcessComplete(processTrace) - "
							+ "problems creating new unique subdirectory", e);
					throw new ResourceProcessException(e);
				}
			}
		}
	}

	public void collectionProcessComplete(ProcessTrace processTrace)
			throws IOException {
		if (compressSingle) {
			try {
				outStream.close();
				zipReady = false;
			} catch (IOException e) {
				LOGGER.error("collectionProcessComplete() - "
						+ "problems closing the output stream", e);
				throw new IOException(e);
			}
		}
	}
}
