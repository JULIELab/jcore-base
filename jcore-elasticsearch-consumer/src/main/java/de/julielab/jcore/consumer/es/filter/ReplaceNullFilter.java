package de.julielab.jcore.consumer.es.filter;

import java.util.List;
import java.util.Objects;

public class ReplaceNullFilter extends AbstractFilter {

    private final String replacement;
    private boolean replaceBlankValues;

    public ReplaceNullFilter(String replacement, boolean replaceBlankValues) {
        this.replacement = Objects.requireNonNull(replacement);
        this.replaceBlankValues = replaceBlankValues;
    }

    public ReplaceNullFilter(String replacement) {
        this(replacement, false);
    }

    @Override
    public List<String> filter(String input) {
        newOutput();
        if (null == input || (replaceBlankValues &&input.isBlank()))
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
