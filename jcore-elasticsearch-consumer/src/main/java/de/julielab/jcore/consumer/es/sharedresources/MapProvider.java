package de.julielab.jcore.consumer.es.sharedresources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapProvider extends AbstractMapProvider<String, String> {
    private final static Logger log = LoggerFactory.getLogger(MapProvider.class);

    public MapProvider() {
        super(log);
    }

    @Override
    protected void put(String key, String value) {
        map.put(key, value);
    }

    @Override
    protected String getValue(String valueString) {
        return valueString.trim().intern();
    }

    @Override
    protected String getKey(String keyString) {
        return keyString.trim().intern();
    }
}
