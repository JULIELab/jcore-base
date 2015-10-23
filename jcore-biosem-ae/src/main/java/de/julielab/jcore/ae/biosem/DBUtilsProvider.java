package de.julielab.jcore.ae.biosem;

import org.apache.uima.resource.SharedResourceObject;

import utils.DBUtils;

public interface DBUtilsProvider extends SharedResourceObject {
	DBUtils getTrainedDatabase();
	void closeDatabase();
}
