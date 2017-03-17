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
