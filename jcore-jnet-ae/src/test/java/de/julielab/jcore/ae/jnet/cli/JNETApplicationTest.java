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
package de.julielab.jcore.ae.jnet.cli;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import de.julielab.jcore.ae.jnet.cli.JNETApplication;

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
