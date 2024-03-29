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
package de.julielab.jcore.ae.banner;

import banner.eval.BANNER;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelTrainTest {
	@Test
	public void testTrainModel() throws Exception {
		XMLConfiguration config = new XMLConfiguration("src/test/resources/config/banner_BC2GM_test.xml");
		BANNER.train(config, null);
		File modelFile = new File("src/test/resources/output/model_BC2GM_small.bin");
		assertTrue(modelFile.exists());
		assertTrue(modelFile.length() > 0);
	}
}
