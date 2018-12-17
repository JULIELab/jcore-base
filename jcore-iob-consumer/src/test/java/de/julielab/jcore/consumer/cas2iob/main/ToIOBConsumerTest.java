/** 
 * ToIOBConsumerTest.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: faessler
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 06.09.2007 
 * 
 * Test of the ToIOBConsumer by employing the XMIToIOBApplication
 **/

/**
 * 
 */
package de.julielab.jcore.consumer.cas2iob.main;

import junit.framework.TestCase;
import de.julielab.jcore.consumer.cas2iob.application.XMIToIOBApplication;

/**
 * @author faessler
 * 
 */
public class ToIOBConsumerTest extends TestCase {

	final private String[] ARGS = { "src/test/resources/testxmis",
			"src/test/resources/de/julielab/jcore/consumer/cas2iob/types/TestTypeSystem.xml",
			"src/test/resources/de/julielab/jcore/consumer/cas2iob/desc/ToIOBConsumerTest.xml" };

	public void testToIOBConsumer() throws Exception {

		XMIToIOBApplication.main(ARGS);

	}
}
