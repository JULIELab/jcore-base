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
package de.julielab.jcore.ae.biosem;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.DBUtils;

import java.util.Properties;

public class DBUtilsProviderImpl implements DBUtilsProvider {

	private static final Logger log = LoggerFactory
			.getLogger(DBUtilsProviderImpl.class);

	public static final String CONFIG_TRAINED_DB = "biosem.db.trained.url";
	private DBUtils trainedDb;
	private boolean dbClosed = false;

	@Override
	public void load(DataResource aData) throws ResourceInitializationException {

		Properties config = new Properties();
		try {
			config.load(aData.getInputStream());
			String dbName = config.getProperty(CONFIG_TRAINED_DB);
			if (null == dbName)
				throw new ResourceInitializationException(
						new IllegalArgumentException(
								"The passed configuration file at "
										+ aData.getUri()
										+ " does not define the property \""
										+ CONFIG_TRAINED_DB
										+ "\" for the trained BioSem database"));
			// per default, assume a file path
			String protocol = "file";
			String dbPath = dbName;
			if (dbName.contains(":")) {
				protocol = dbName.substring(0, dbName.indexOf(':'));
				dbPath = dbName.substring(dbName.indexOf(':') + 1);
			}
			log.info(
					"Trying to find trained BioSem database using protocol {} at location {}.",
					protocol, dbPath);
			trainedDb = new DBUtils(dbPath, protocol, "readonly=true");
			trainedDb.openDB();
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}

		// This code could be used to read the database directly without the
		// properties file. It assumes that one of the database files is given,
		// e.g. the '.data' file. The suffix is removed and from the path the
		// "....jar!..." portion is removed, resulting in the classpath address.
		// This is working, but in the end the configuration file seems the more
		// appropriate way of doing it.
		// URI uri = aData.getUri();
		// log.info("Loading trained BioSem database from {}.", uri);
		// System.out.println("Fragment: " + uri.getFragment());
		// System.out.println("Query: " + uri.getQuery());
		// System.out.println("Path: " + uri.getPath());
		// String res = uri.toString();
		// res = res.substring(0, res.lastIndexOf('.'));
		// res = res.substring(res.indexOf('!') + 1);
		// System.out.println("Das ist jetzt die adresse: " + res);
		// trainedDb = new DBUtils(res, "res", "readonly=true");
		// trainedDb.openDB();
	}

	@Override
	public DBUtils getTrainedDatabase() {
		return trainedDb;
	}

	@Override
	public synchronized void closeDatabase() {
		if (!dbClosed) {
			trainedDb.closeDB();
			dbClosed = true;
		}

	}

}
