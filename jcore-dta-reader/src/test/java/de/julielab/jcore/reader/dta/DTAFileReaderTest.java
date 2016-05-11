package de.julielab.jcore.reader.dta;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class DTAFileReaderTest {
	static final String TEST_FILE = "src/test/resources/testfiles/arnim_wunderhorn01_1806.tcf.xml.short";

	@Test
	public void testGetDocumentText() {
		try {
			assertEquals(
					"Des Knaben Wunderhorn."
							+ "\nAlte deutſche Lieder geſammelt von L. A. v. Arnim und Clemens Brentano."
							+ "\nDes Knaben Wunderhorn Alte deutſche Lieder L. Achim v. Arnim."
							+ "\nClemens Brentano."
							+ "\nHeidelberg, beÿ Mohr u. Zimmer.",
					DTAFileReader.getDocumentText(TEST_FILE, false));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testGetDocumentTextWithCorrection() {
		try {
			assertEquals(
					"Des Knaben Wunderhorn."
							+ "\nAlte deutsche Lieder gesammelt von L. A. v. Arnim und Clemens Brentano."
							+ "\nDes Knaben Wunderhorn Alte deutsche Lieder L. Achim v. Arnim."
							+ "\nClemens Brentano."
							+ "\nHeidelberg, bei Mohr u. Zimmer.",
					DTAFileReader.getDocumentText(TEST_FILE, true));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

}
