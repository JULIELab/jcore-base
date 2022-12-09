package de.julielab.jcore.consumer.es.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
class ReplaceNullFilterTest {
    @Test
    public void filter() {
        final ReplaceNullFilter filter = new ReplaceNullFilter("null-replacement-value");
        assertThat(filter.filter("somevalue")).containsExactly("somevalue");
        assertThat(filter.filter(null)).containsExactly("null-replacement-value");
    }

    @Test
    public void filterBlank() {
        final ReplaceNullFilter filter = new ReplaceNullFilter("null-replacement-value", true);
        assertThat(filter.filter("somevalue")).containsExactly("somevalue");
        assertThat(filter.filter("")).containsExactly("null-replacement-value");
    }
}