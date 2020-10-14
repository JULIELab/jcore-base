package de.julielab.jcore.reader.pmc;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertTrue;

public class NXMLURIIteratorTest {
    @Test
    public void testWhitelist() throws FileNotFoundException {
        Iterator<URI> recursiveIt = new NXMLURIIterator(new File("src/test/resources/documents-recursive"),
                new HashSet<>(Arrays.asList("PMC2970367", "PMC2847692")),
                true, true);
        assertTrue(recursiveIt.hasNext());
        assertTrue(recursiveIt.hasNext());
        Set<String> expectedFiles = new HashSet<>();
        while (recursiveIt.hasNext()) {
            URI uri = recursiveIt.next();
            String filename = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
            expectedFiles.add(filename);
        }
        assertThat(expectedFiles).containsExactlyInAnyOrder("PMC2847692.nxml.gz", "PMC2970367.nxml.gz");
    }

    @Test
    public void testGetPmcFiles() throws Exception {
        Iterator<URI> recursiveIt = new NXMLURIIterator(new File("src/test/resources/documents-recursive"),
                null,
                true,
                false);
        assertTrue(recursiveIt.hasNext());
        // check that multiple calls to hasNext() don't cause trouble
        assertTrue(recursiveIt.hasNext());
        assertTrue(recursiveIt.hasNext());
        assertTrue(recursiveIt.hasNext());
        assertTrue(recursiveIt.hasNext());
        assertTrue(recursiveIt.hasNext());
        Set<String> expectedFiles = new HashSet<>();
        while (recursiveIt.hasNext()) {
            URI uri = recursiveIt.next();
            String filename = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
            // just to try causing trouble
            expectedFiles.add(filename);
        }
        assertThat(expectedFiles).containsExactlyInAnyOrder("PMC2847692.nxml.gz", "PMC2758189.nxml.gz",
                "PMC2970367.nxml.gz", "PMC3201365.nxml.gz", "PMC4257438.nxml.gz");
    }

    @Test
    public void testXmlEntities() throws MalformedURLException {
        String s = "jar:file:/data/data_corpora/PMC/non_comm_use.O-Z.xml.zip!/P&#x000e4;diatrische_Gastroenterologie,_Hepatologie_und_Ern&#x000e4;hrung/PMC7498810.nxml";
        s = StringEscapeUtils.unescapeXml(s);
        assertThat(s).doesNotContain("&#x000e4;");
        URL url = new URL(s);
        assertThat(url).isNotNull();
        assertThatCode(() -> url.toURI()).doesNotThrowAnyException();
    }
}
