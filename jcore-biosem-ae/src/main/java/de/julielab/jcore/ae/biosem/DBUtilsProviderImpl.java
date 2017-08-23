/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.biosem;

import java.util.Properties;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.DBUtils;

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
										+ " does define the property \""
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
