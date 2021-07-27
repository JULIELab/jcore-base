package de.julielab.jcore.consumer.es.sharedresources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReversedDoubleMapProvider extends AbstractMapProvider<String, Double> {
    private final static Logger log = LoggerFactory.getLogger(ReversedDoubleMapProvider.class);

    public ReversedDoubleMapProvider() {
        super(log);
        this.reverse = true;
    }

    @Override
    protected void put(String key, Double value) {
        map.put(key, value);
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
