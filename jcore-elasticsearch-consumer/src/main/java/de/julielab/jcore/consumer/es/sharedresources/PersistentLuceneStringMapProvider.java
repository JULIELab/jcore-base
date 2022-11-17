package de.julielab.jcore.consumer.es.sharedresources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentLuceneStringMapProvider extends PersistentStringIndexMapProvider {
    private final static Logger log = LoggerFactory.getLogger(PersistentLuceneStringMapProvider.class);

    public PersistentLuceneStringMapProvider() {
        super(log);
    }

    @Override
    protected StringIndex initializeIndex(String cachePath) {
        return new LuceneIndex(cachePath);
    }
}
