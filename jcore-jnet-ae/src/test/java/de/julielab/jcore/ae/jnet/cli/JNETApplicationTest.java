/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 */

package de.julielab.jcore.ae.jnet.cli;

import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class JNETApplicationTest {
	private static final String PREFIX = "src/test/resources/de/julielab/jcore/ae/jnet/cli/";
	
	private static final File FEATURE_CONFIG_FILE_DEFAULT_STEMMER = new File(PREFIX+"testFeatureConf.conf");
	private static final File FEATURE_CONFIG_FILE_SNOWBALL_STEMMER = new File(PREFIX+"testFeatureConfSnowball.conf");
	private static final File FEATURE_CONFIG_FILE_POS = new File(PREFIX+"testFeatureConfPos.conf");

	private static final String UNITTEST_MODEL_DATA = PREFIX+"testModel_traindata.ppd";
	private static final String UNITTEST_MODEL = PREFIX+"unittest_model";
	private static final String UNITTEST_MODEL_GZ = UNITTEST_MODEL + ".gz";
	
	private static final String IGN_DATA = PREFIX+"ign-train.ppd";
	private static final String IGN_MODEL = PREFIX+"ign-train-model";
	private static final String IGN_MODEL_GZ =  IGN_MODEL+".gz";
	
	private static final String PREDICT_OUT = PREFIX+"predict-out.txt";
	

	
    @After 
    public void deleteModel() {
    	File modelFile = new File(UNITTEST_MODEL_GZ);
		if (modelFile.exists())
			modelFile.delete();
		File ignFile = new File(IGN_MODEL_GZ);
		if (ignFile.exists())
			ignFile.delete();
		File outFile = new File(PREDICT_OUT);
		if(outFile.exists())
			outFile.delete();
    }
    
	@Test
	public void testTrainAndWriteModel() throws Exception{
		File modelOutFile = new File(UNITTEST_MODEL);
		File effectiveModelFile = new File(UNITTEST_MODEL_GZ);
		if (effectiveModelFile.exists())
			effectiveModelFile.delete();
		JNETApplication.train(new File(UNITTEST_MODEL_DATA),
				modelOutFile, FEATURE_CONFIG_FILE_SNOWBALL_STEMMER, 2, false);
		assertTrue(effectiveModelFile.exists());
	}
	
	@Test
	public void testReadModelAndPredictUEAStemmer() {
		File outFile = new File(PREDICT_OUT);
		
		if (outFile.exists())
			outFile.delete();
		
		File modelOutFile = new File(UNITTEST_MODEL);
		File effectiveModelFile = new File(UNITTEST_MODEL_GZ);
		if (effectiveModelFile.exists())
			effectiveModelFile.delete();
		File trainDataFile = new File(UNITTEST_MODEL_DATA);
		JNETApplication.train(trainDataFile,
				modelOutFile, FEATURE_CONFIG_FILE_DEFAULT_STEMMER, 2, false);
		assertTrue(effectiveModelFile.exists());
		
		JNETApplication.predict(trainDataFile, effectiveModelFile, outFile, false);
		assertTrue(outFile.exists());
	}
	
	@Test
	public void testReadModelAndPredictSnowballStemmer() {
		File outFile = new File(PREDICT_OUT);
		
		if (outFile.exists())
			outFile.delete();
		
		File modelOutFile = new File(UNITTEST_MODEL);
		File effectiveModelFile = new File(UNITTEST_MODEL_GZ);
		if (effectiveModelFile.exists())
			effectiveModelFile.delete();
		File trainDataFile = new File(UNITTEST_MODEL_DATA);
		JNETApplication.train(trainDataFile,
				modelOutFile, FEATURE_CONFIG_FILE_SNOWBALL_STEMMER, 2, false);
		assertTrue(effectiveModelFile.exists());
		
		JNETApplication.predict(trainDataFile, effectiveModelFile, outFile, false);
		assertTrue(outFile.exists());
	}
	
	@Test
	public void testReadModelAndPredictWithPos() {
		File outFile = new File(PREDICT_OUT);
		
		if (outFile.exists())
			outFile.delete();
		
		File modelOutFile = new File(IGN_MODEL);
		File effectiveModelFile = new File(IGN_MODEL_GZ);
		if (effectiveModelFile.exists())
			effectiveModelFile.delete();
		File trainDataFile = new File(IGN_DATA);
		JNETApplication.train(trainDataFile,
				modelOutFile, FEATURE_CONFIG_FILE_POS, 5, false);
		assertTrue(effectiveModelFile.exists());
		
		JNETApplication.predict(trainDataFile, effectiveModelFile, outFile,true);
		assertTrue(outFile.exists());
	}
}
