package de.julielab.jcore.reader.dta;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;


public class DTAFileReaderTest {
static final String TEST_FILE= "src/test/resources/testfiles/arnim_wunderhorn01_1806.tcf.xml";
	@Test
	public void testGetDocumentText(){
		try {
			assertEquals("",DTAFileReader.getDocumentText(TEST_FILE, true));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

}
