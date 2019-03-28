/** 
 * DBStatusCallbackListener.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are protected. Please contact JULIE Lab for further information.  
 *
 * Author: landefeld
 * 
 * Current version: //TODO insert current version number 	
 * Since version:   //TODO insert version number of first appearance of this class
 *
 * Creation date: 16.09.2008 
 * 
 * //TODO insert short description
 **/

package de.julielab.jcore.cpe;

import de.julielab.jcore.reader.db.DBReader;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.commons.cli.*;
import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DBCPERunner {

	/**
	 * the descriptor file for the CPE;
	 */
	protected String descriptorFile;
	protected CpeDescription cpeDescription;
	protected Options options;
	protected CollectionProcessingEngine cpe;
	protected DBStatusCallbackListener statusCallbackListener;
	/**
	 * the configuration taken from the Medline Reader and used to instantiate
	 * the Database Connector
	 */
	protected String configuration;
	/**
	 * the Database Connector instantiated via the configuration taken from the
	 * Medline Reader
	 */
	protected DataBaseConnector dbc;
	/**
	 * the subset where the status of documents is updated
	 */
	public String subset;
	protected Integer processingUnitThreadCount;
	protected Integer casPoolSize;
	protected Integer numbersOfDocuments;
	protected Integer batchSize;
	protected boolean error;
	private final static Logger LOGGER = LoggerFactory
			.getLogger(DBCPERunner.class);

	public static void main(String[] args) {
		DBCPERunner runner = new DBCPERunner();
		runner.process(args);
	}

	public DBCPERunner() {
		options = new Options();
		options.addOption("d", true, "CPE descriptor file");
		options.addOption("n", true,
				"numbers of documents to process (optional)");
		options.addOption("t", true, "processing unit thread count (optional)");
		options.addOption("a", true, "CAS pool size (optional)");
		options.addOption("b", true, "batch size (optional)");
	}

	public void parseArguments(String[] args) {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			LOGGER.error("Can't parse arguments:", e);
			return;
		}

		descriptorFile = cmd.getOptionValue("d");
		if (descriptorFile == null) {
			System.err.println("-d option is missed");
			error = true;
		}

		String numbersOfDocumentsString = cmd.getOptionValue("n");
		if (numbersOfDocumentsString != null) {
			numbersOfDocuments = new Integer(numbersOfDocumentsString);
		}

		String processingUnitThreadCountString = cmd.getOptionValue("t");
		if (processingUnitThreadCountString != null) {
			processingUnitThreadCount = new Integer(
					processingUnitThreadCountString);
		}

		String casPoolSizeString = cmd.getOptionValue("a");
		if (casPoolSizeString != null) {
			casPoolSize = new Integer(casPoolSizeString);
		}

		String batchSizeString = cmd.getOptionValue("b");
		if (batchSizeString != null) {
			batchSize = new Integer(batchSizeString);
		}
	}

	/**
	 * @throws InvalidXMLException
	 * @throws IOException
	 * @throws CpeDescriptorException
	 */
	public void createCPEDescription() throws InvalidXMLException, IOException,
			CpeDescriptorException {
		LOGGER.info("Creating CPE description from file " + descriptorFile);
		cpeDescription = UIMAFramework.getXMLParser().parseCpeDescription(
				new XMLInputSource(descriptorFile));

		if (processingUnitThreadCount != null) {
			LOGGER.info("Setting processing unit thread count to "
					+ processingUnitThreadCount);
			cpeDescription
					.setProcessingUnitThreadCount(processingUnitThreadCount);
		}

		if (casPoolSize != null) {
			LOGGER.info("Setting cas pool size to " + casPoolSize);
			cpeDescription.getCpeCasProcessors().setPoolSize(casPoolSize);
		}

		if (numbersOfDocuments != null) {
			LOGGER.info("Setting number of documents to process to "
					+ numbersOfDocuments);
			cpeDescription.setNumToProcess(numbersOfDocuments);
		}

		if (batchSize != null) {
			LOGGER.info("Setting CPE checkpoint batch size to " + batchSize);
		} else {
			int descBatchSize = cpeDescription.getCpeConfiguration()
					.getCheckpoint().getBatchSize();
			if (descBatchSize != 0) {
				LOGGER.info("Setting CPE checkpoint batch size to "
						+ descBatchSize);
				batchSize = descBatchSize;
			} else {
				LOGGER.info("CPE Checkpoint batch size not set in CPE descriptor. Setting batch size to 2000");
				batchSize = 2000;
			}
		}
	}

	/**
	 * @throws InvalidXMLException
	 * @throws IOException
	 * @throws ResourceInitializationException
	 * @throws CpeDescriptorException
	 */
	public void createCPE() throws InvalidXMLException, IOException,
			ResourceInitializationException, CpeDescriptorException {
		LOGGER.info("Creating CPE... ");
		cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDescription);

		// get dbc configuration parameter from the DB Reader in the CPE;
		// the configuration is used to instantiate the Database Connector
		DBReader reader = (DBReader) cpe.getCollectionReader();
		ConfigurationParameterSettings parameterSettings = reader.getMetaData()
				.getConfigurationParameterSettings();
		configuration = (String) parameterSettings
				.getParameterValue(DBReader.PARAM_COSTOSYS_CONFIG_NAME);
		LOGGER.info("DBCConfiguration taken from Medline Reader: "
				+ configuration);

		InputStream is = null;
		File dbcConfigFile = new File(configuration);
		if (dbcConfigFile.exists())
			is = new FileInputStream(dbcConfigFile);
		else
			is = getClass().getResourceAsStream("/" + configuration);
		dbc = new DataBaseConnector(is);

		subset = (String) parameterSettings
				.getParameterValue(DBReader.PARAM_TABLE);
		LOGGER.info("Subset name: " + subset);

		statusCallbackListener = new DBStatusCallbackListener(cpe, dbc, subset,
				batchSize);
		cpe.addStatusCallbackListener(statusCallbackListener);
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public void showHelpText() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("DBCPERunner", options);
	}

	public void run() throws ResourceInitializationException {
		LOGGER.info("Start processing ..");
		cpe.process();
	}

	public void process(String[] args) {

		parseArguments(args);

		if (isError()) {
			showHelpText();
			System.exit(1);
		}

		runCPE();
	}

	/**
	 * 
	 */
	protected void runCPE() {
		try {
			createCPEDescription();
		} catch (Throwable e) {
			LOGGER.error("Error while creating the CPE description:", e);
			System.err.println("Can't create CPE description: " + e);
			System.exit(1);
		}

		try {
			createCPE();
		} catch (Throwable e) {
			LOGGER.error("Error while creating the CPE:", e);
			System.err.println("Can't create CPE: " + e);
			System.exit(1);
		}

		try {
			run();
		} catch (Throwable e) {
			LOGGER.error("Exception during processing:", e);
			System.err.println("Exception during processing: " + e);
			System.exit(1);
		}
	}

	public String getDescriptorFile() {
		return descriptorFile;
	}

	public CpeDescription getCpeDescription() {
		return cpeDescription;
	}

	public Options getOptions() {
		return options;
	}

	public CollectionProcessingEngine getCpe() {
		return cpe;
	}

	public DBStatusCallbackListener getStatusCallbackListener() {
		return statusCallbackListener;
	}

	public String getConfiguration() {
		return configuration;
	}

	public DataBaseConnector getDataBaseConnector() {
		return dbc;
	}

	public String getSubset() {
		return subset;
	}

	public Integer getProcessingUnitThreadCount() {
		return processingUnitThreadCount;
	}

	public Integer getCasPoolSize() {
		return casPoolSize;
	}

	public Integer getNumbersOfDocuments() {
		return numbersOfDocuments;
	}
}