package de.julielab.jcore.consumer.es.filter;

import java.util.List;

/**
 * Always returns the same output value that has been given to the constructor.
 */
public class ConstantOutputFilter extends AbstractFilter {

    private String outputTerm;

    public ConstantOutputFilter(String outputTerm) {

        this.outputTerm = outputTerm;
    }
    @Override
    public List<String> filter(String input) {
        newOutput();
        output.add(outputTerm);
        return output;
    }

    @Override
    public Filter copy() {
        return new ConstantOutputFilter(outputTerm);
    }
}
