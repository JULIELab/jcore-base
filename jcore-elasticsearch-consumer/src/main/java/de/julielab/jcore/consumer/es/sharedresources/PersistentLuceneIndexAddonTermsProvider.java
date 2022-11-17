package de.julielab.jcore.consumer.es.sharedresources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentLuceneIndexAddonTermsProvider extends PersistentIndexAddonTermsProvider{
    private final static Logger log = LoggerFactory.getLogger(PersistentLuceneIndexAddonTermsProvider.class);

    public PersistentLuceneIndexAddonTermsProvider() {
        super(log);
    }

    @Override
    protected StringIndex initializeIndex(String cachePath) {
        return new LuceneIndex(cachePath);
    }
}
