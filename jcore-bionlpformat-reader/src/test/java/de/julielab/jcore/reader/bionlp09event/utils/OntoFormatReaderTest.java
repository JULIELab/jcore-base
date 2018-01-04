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
package de.julielab.jcore.reader.bionlp09event.utils;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import de.julielab.jcore.reader.bionlpformat.utils.OntoFormatReader;

public class OntoFormatReaderTest {

	@Test
	public void testReadFile() throws IOException {
		OntoFormatReader ontoReader = new OntoFormatReader();
		ontoReader.readFile(new File("src/test/resources/de/julielab/jcore/reader/test_ontos/annotation.conf"));
	}
}
