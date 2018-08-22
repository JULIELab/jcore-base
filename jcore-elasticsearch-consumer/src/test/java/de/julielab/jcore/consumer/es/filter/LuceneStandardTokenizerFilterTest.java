package de.julielab.jcore.consumer.es.filter;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
public class LuceneStandardTokenizerFilterTest {
    @Test
    public void testTokenizer() {
        LuceneStandardTokenizerFilter filter = new LuceneStandardTokenizerFilter();
        List<String> tokens = filter.filter("A horse is-a animal.");
        assertThat(tokens).containsExactly("A", "horse", "is", "a", "animal");
    }
}
