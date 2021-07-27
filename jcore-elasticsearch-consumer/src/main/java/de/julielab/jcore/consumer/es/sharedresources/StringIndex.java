package de.julielab.jcore.consumer.es.sharedresources;

public interface StringIndex {
    String get(String key);

    String[] getArray(String key);

    void put(String key, String value);

    void put(String key, String[] value);

    void commit();

    boolean requiresExplicitCommit();

    void close();

    void open();

    int size();

    default String getName() {
        return getClass().getSimpleName();
    }
}
