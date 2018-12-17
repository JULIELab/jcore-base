package de.julielab.jcore.reader.pmc;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
}
