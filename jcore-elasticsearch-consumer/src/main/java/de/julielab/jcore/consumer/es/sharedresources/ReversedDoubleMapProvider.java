package de.julielab.jcore.consumer.es.sharedresources;

public class ReversedDoubleMapProvider extends AbstractMapProvider<String, Double> {

    public ReversedDoubleMapProvider() {
        this.reverse = true;
    }

    @Override
    protected Double getValue(String valueString) {
        return Double.parseDouble(valueString.trim());
    }

    @Override
    protected String getKey(String keyString) {
        return keyString.trim().intern();
    }
}
