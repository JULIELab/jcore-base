package de.julielab.jcore.misc;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import de.julielab.java.utilities.IOStreamUtilities;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DescriptorCreatorTest {
	
	@BeforeClass
	@AfterClass
	public static void shutdown() throws IOException {
		//FileUtils.deleteDirectory(new File(Arrays.asList("src", "test", "resources", "de").stream().collect(joining(File.separator))));
	}
	@Test
	public void testRun() throws Exception {
		DescriptorCreator creator = new DescriptorCreator();
		String outputRoot = "src" + File.separator + "test" + File.separator + "resources" + File.separator;
		creator.run(outputRoot);	
		File crDir = new File(outputRoot + Stream.of("de", "julielab", "jcore", "reader", "testreader", "desc").collect(joining(File.separator)));
		File aeDir = new File(outputRoot + Stream.of("de", "julielab", "jcore", "ae", "testae", "desc").collect(joining(File.separator)));
		File consumerDir = new File(outputRoot + Stream.of("de", "julielab", "jcore", "consumer", "testconsumer", "desc").collect(joining(File.separator)));
		File multiplierDir = new File(outputRoot + Stream.of("de", "julielab", "jcore", "multiplier", "testmultiplier", "desc").collect(joining(File.separator)));
        File abstractAeDir = new File(outputRoot + Stream.of("de", "julielab", "jcore", "ae", "abstractae", "desc").collect(joining(File.separator)));
		
		assertTrue(crDir.exists());
		assertTrue(aeDir.exists());
		assertTrue(consumerDir.exists());
		assertTrue(multiplierDir.exists());
        assertFalse(abstractAeDir.exists());
		
		assertTrue(containsDescriptor(crDir));
		assertTrue(containsDescriptor(aeDir));
		assertTrue(containsDescriptor(consumerDir));
		assertTrue(containsDescriptor(multiplierDir));

        // Make sure that the type systems are imported and not pasted into the descriptor.
        final String content = IOStreamUtilities.getStringFromInputStream(new FileInputStream(Path.of(aeDir.getCanonicalPath(), "de.julielab.jcore.ae.testae.TestAE.xml").toFile()));
        assertTrue(content.contains("<import name=\"de.julielab.jcore.types.jcore-morpho-syntax-types\"/>"));
    }

	private boolean containsDescriptor(File dir) {
		return Stream.of(dir.list()).filter(f -> f.contains(".xml")).findAny().isPresent();
	}
}
