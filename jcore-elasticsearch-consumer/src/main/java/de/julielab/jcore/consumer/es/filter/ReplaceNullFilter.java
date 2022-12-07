package de.julielab.jcore.consumer.es.filter;

import java.util.List;
import java.util.Objects;

public class ReplaceNullFilter extends AbstractFilter {

    private final String replacement;

    public ReplaceNullFilter(String replacement) {
        this.replacement = Objects.requireNonNull(replacement);
    }

    @Override
    public List<String> filter(String input) {
        newOutput();
        if (null == input)
            output.add(replacement);
        else
            output.add(input);
        return output;
    }

    @Override
    public Filter copy() {
        return new ReplaceNullFilter(replacement);
    }

}
