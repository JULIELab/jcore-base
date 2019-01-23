package de.julielab.jcore.consumer.es.sharedresources;

public class MapProvider extends AbstractMapProvider<String, String> {
    @Override
    protected String getValue(String valueString) {
        return valueString.trim().intern();
    }

    @Override
    protected String getKey(String keyString) {
        return keyString.trim().intern();
    }
}
