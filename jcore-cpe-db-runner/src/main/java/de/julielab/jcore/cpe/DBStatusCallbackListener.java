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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.ext.DBProcessingMetaData;
import de.julielab.xmlData.dataBase.DataBaseConnector;

public class DBStatusCallbackListener implements StatusCallbackListener {

	private CollectionProcessingEngine cpe;
	private DataBaseConnector dbc;
	private String subset;
	/**
	 * list holding primary keys of documents that have been successfully
	 * processed
	 */
	private ArrayList<byte[][]> processed = new ArrayList<byte[][]>();
	/**
	 * list holding primary keys of documents during the processing of which an
	 * exception occured
	 */
	private ArrayList<byte[][]> exceptions = new ArrayList<byte[][]>();
	/**
	 * matches primary keys of unsuccessfully processed documents and exceptions
	 * that occured during the processing
	 */
	private HashMap<byte[][], String> logException = new HashMap<byte[][], String>();
	int entityCount = 0;
	/**
	 * Start time of the processing
	 */
	private long mInitCompleteTime;
	private long mBatchTime;
	private Integer batchSize;
	private final static Logger LOGGER = LoggerFactory.getLogger(DBStatusCallbackListener.class);

	public DBStatusCallbackListener(CollectionProcessingEngine cpe, DataBaseConnector dbc,
			String subset, Integer batchSize) {
		this.cpe = cpe;
		this.dbc = dbc;
		this.subset = subset;
		this.batchSize = batchSize;
	}

	/**
	 * Called when the initialization is completed.
	 * 
	 * @see org.apache.uima.collection.processing.StatusCallbackListener#initializationComplete()
	 */
	public void initializationComplete() {
		LOGGER.info("CPE Initialization complete");
		mInitCompleteTime = System.currentTimeMillis();
		mBatchTime = System.currentTimeMillis();
	}

	/**
	 * Called when the batchProcessing is completed.
	 * 
	 * @see org.apache.uima.collection.processing.StatusCallbackListener#batchProcessComplete()
	 * 
	 */
	public synchronized void batchProcessComplete() {
		dbc.setProcessed(subset, processed);
		processed.clear();
		LOGGER.info("Completed " + entityCount + " documents");
	}

	/**
	 * Called when the collection processing is completed. Exits the application
	 * in case it doesn't exit on itself (happens e.g. with JREX because of the
	 * ExecutorService; we have problems to shut all of them down).
	 * 
	 * @see org.apache.uima.collection.processing.StatusCallbackListener#collectionProcessComplete()
	 */
	public synchronized void collectionProcessComplete() {
		dbc.setProcessed(subset, processed);
		processed.clear();

		long time = System.currentTimeMillis();
		LOGGER.info("Completed " + entityCount + " documents");
		long processingTime = time - mInitCompleteTime;
		LOGGER.info("Processing Time: " + processingTime + " ms");
		LOGGER.info("\n\n ------------------ PERFORMANCE REPORT ------------------\n");
		LOGGER.info(cpe.getPerformanceReport().toString());
		System.exit(0);
	}

	/**
	 * Called when the CPM is paused.
	 * 
	 * @see org.apache.uima.collection.processing.StatusCallbackListener#paused()
	 */
	public void paused() {
		LOGGER.info("Paused");
	}

	/**
	 * Called when the CPM is resumed after a pause.
	 * 
	 * @see org.apache.uima.collection.processing.StatusCallbackListener#resumed()
	 */
	public void resumed() {
		LOGGER.info("Resumed");
	}

	/**
	 * Called when the CPM is stopped abruptly due to errors.
	 * 
	 * @see org.apache.uima.collection.processing.StatusCallbackListener#aborted()
	 */
	public void aborted() {
		LOGGER.info("The CPE has been aborted by the framework. The JVM ist forcibly quit to avoid the application getting stuck on some threads that could not be stopped.");
		System.exit(1);
	}

	/**
	 * Called when the processing of a document is completed. <br>
	 * The process status can be looked at and corresponding actions taken.
	 * 
	 * @param aCas
	 *            CAS corresponding to the completed processing
	 * @param aStatus
	 *            EntityProcessStatus that holds the status of all the events
	 *            for an entity
	 */
	public synchronized void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
		JCas jCas;
		entityCount++;

		try {
			byte[][] primaryKey = null;
			// There are errors that cause the CAS to be null.
			if (null != aCas) {
				jCas = aCas.getJCas();

				@SuppressWarnings("rawtypes")
				// get FSArray holding primary key elements as a feature of type
				// Header and
				// create byte[][] primary key that contains these elements;
				// primary keys are used to update the status of processing in
				// the
				// subset
				// via the Database Connector
				FSIterator it = jCas.getAnnotationIndex(Header.type).iterator();

				if (it.hasNext()) {
					try {
						DBProcessingMetaData metaData = JCasUtil.selectSingle(jCas, DBProcessingMetaData.class);
						StringArray array = metaData.getPrimaryKey();
						primaryKey = new byte[array.size()][];

						for (int i = 0; i < array.size(); i++) {
							primaryKey[i] = array.get(i).getBytes();
						}
					} catch (IllegalArgumentException e) {
						LOGGER.error("Error while reading primary key from CAS:", e);
						throw new IllegalStateException(
								"The database primary key to update the document "
										+ "processing state was not found in the document's "
										+ "Header type (de.julielab.jules.types.Header#getPkElements). "
										+ "You must use the jules-medline-reader as of version 2.4 "
										+ "for this information to be set.");
					}
				} else {
					LOGGER.warn(
							"CAS has no header, thus the document cannot be marked as processed. The beginning of the CAS text is: \"{}\".",
							jCas != null && jCas.getDocumentText() != null ? jCas.getDocumentText()
									.substring(0, 20) : "<no text>");
				}
			} else {
				LOGGER.error(
						"An error was raised that lead to a CAS being null. Thus, the error can't be logged back into the database because the original document cannot be identified. The error was: {}",
						createLog(aStatus));
			}

			String primaryKeyString = getPrimaryKeyString(primaryKey);
			if (aStatus.isException()) {
				if (null != primaryKey) {
					exceptions.add(primaryKey);

					String log = createLog(aStatus);
					logException.put(primaryKey, log);

					LOGGER.warn("processing of {}.document (ID: {}) raised exception: {}",
							new Object[] { entityCount, primaryKeyString, log });

					dbc.setProcessed(subset, processed);
					processed.clear();
					dbc.setException(subset, exceptions, logException);
					exceptions.clear();
					logException.clear();
				} else {
					String docId = "<unknown>";
					if (null != aCas) {
						jCas = aCas.getJCas();
						FSIterator<Annotation> it = jCas.getAnnotationIndex(Header.type).iterator();
						if (it.hasNext()) {
							Header header = (Header) it.next();
							docId = header.getDocId();
						}
					}
					LOGGER.error("Document with ID \"{}\" raised exception: {}", docId,
							createLog(aStatus));
				}
			} else {
				processed.add(primaryKey);
				LOGGER.debug("{}.document processed (ID: {})", entityCount, primaryKeyString);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (entityCount % batchSize == 0) {
			dbc.setProcessed(subset, processed);
			processed.clear();
			LOGGER.info("Completed " + entityCount + " documents");
			long time = System.currentTimeMillis() - mBatchTime;
			LOGGER.info("----------- Performance: it took " + time + "ms to process the last "
					+ batchSize + " documents ----------- \n");
			mBatchTime = System.currentTimeMillis();
		}
	}

	/**
	 * @param primaryKey
	 * @return
	 */
	private String getPrimaryKeyString(byte[][] primaryKey) {
		if (null == primaryKey)
			return "<null primary key>";
		String[] strings = new String[primaryKey.length];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = new String(primaryKey[i]);
		}
		return StringUtils.join(strings, ", ");
	}

	/**
	 * Create log entry for an exception that occured during the processing of a
	 * document
	 * 
	 * @param status
	 *            the status that is an exception
	 */
	public String createLog(EntityProcessStatus status) {
		StringBuilder builder = new StringBuilder();

		builder.append("Error happened on: " + new Date());
		builder.append("-------------- Failed Components -------------- \n");
		@SuppressWarnings("rawtypes")
		List componentNames = status.getFailedComponentNames();
		for (int i = 0; i < componentNames.size(); i++) {
			builder.append((i + 1) + ". " + componentNames.get(i) + "\n");
		}

		builder.append("-------------- Stack Traces -------------- \n");
		@SuppressWarnings("rawtypes")
		List exceptions = status.getExceptions();
		for (int i = 0; i < exceptions.size(); i++) {
			Throwable throwable = (Throwable) exceptions.get(i);
			StringWriter writer = new StringWriter();
			throwable.printStackTrace(new PrintWriter(writer));
			builder.append(writer.toString());
		}

		return builder.toString();
	}

	public CollectionProcessingEngine getCpe() {
		return cpe;
	}

	public DataBaseConnector getDbc() {
		return dbc;
	}

	public String getSubset() {
		return subset;
	}
}
