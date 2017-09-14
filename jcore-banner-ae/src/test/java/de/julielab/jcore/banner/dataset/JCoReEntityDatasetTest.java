package de.julielab.jcore.banner.dataset;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import banner.tokenization.SimpleTokenizer;

public class JCoReEntityDatasetTest {
	@Test
	public void testLoad() {
		JCoReEntityDataset ds = new JCoReEntityDataset(new SimpleTokenizer());
		ds.load(new File("src/test/resources/data/jcoreentitydataset/sentences.tsv"),
				new File("src/test/resources/data/jcoreentitydataset/entities.tsv"));
		assertNotNull(ds.getSentences());
		assertTrue(ds.getSentences().size() > 0);
		Set<String> mentionStrings = ds.getSentences().stream().map(s -> s.getMentions()).flatMap(ms -> ms.stream()).map(m -> m.getText()).collect(Collectors.toSet());
		assertTrue(mentionStrings.contains("lysophospholipase"));
		assertTrue(mentionStrings.contains("lysophospholipid-specific lysophospholipase"));
		assertTrue(mentionStrings.contains("hLysoPLA"));
	}
}
