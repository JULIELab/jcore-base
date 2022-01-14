package de.julielab.jcore.consumer.es.sharedresources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

abstract public class PersistentStringIndexMapProvider extends AbstractMapProvider<String, String> {
    public static final int MAXIMUM_MEMCACHE_SIZE = 10000;
    private final LoadingCache<String, Optional<String>> cache;
    private StringIndex index;

    public PersistentStringIndexMapProvider(Logger log) {
        super(log);
        map = new Map<>() {
            @Override
            public int size() {
                return index.size();
            }

            @Override
            public boolean isEmpty() {
                throw new NotImplementedException();
            }

            @Override
            public boolean containsKey(Object key) {
                throw new NotImplementedException();
            }

            @Override
            public boolean containsValue(Object value) {
                throw new NotImplementedException();
            }

            @Override
            public String get(Object key) {
                try {
                    return cache.get((String) key).orElse(null);
                } catch (ExecutionException e) {
                    log.error("Could not retrieve value from the cache for key '{}'.", key);
                    throw new IllegalStateException();
                }
            }

            @Nullable
            @Override
            public String put(String key, String value) {
                throw new NotImplementedException();
            }

            @Override
            public String remove(Object key) {
                throw new NotImplementedException();
            }

            @Override
            public void putAll(@NotNull Map<? extends String, ? extends String> m) {
                throw new NotImplementedException();
            }

            @Override
            public void clear() {
                throw new NotImplementedException();
            }

            @NotNull
            @Override
            public Set<String> keySet() {
                throw new NotImplementedException();
            }

            @NotNull
            @Override
            public Collection<String> values() {
                throw new NotImplementedException();
            }

            @NotNull
            @Override
            public Set<Map.Entry<String, String>> entrySet() {
                throw new NotImplementedException();
            }
        };
        cache = CacheBuilder.newBuilder().maximumSize(MAXIMUM_MEMCACHE_SIZE).expireAfterAccess(Duration.ofHours(1)).build(new CacheLoader<>() {
            @Override
            public Optional<String> load(String s) {
                return Optional.ofNullable(index.get(s));
            }
        });
    }

    @Override
    protected void put(String key, String value) {
        index.put(key, value);
    }

    protected abstract StringIndex initializeIndex(String cachePath);

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
        // prepare the persistent index
        URI uri = aData.getUri();
        File indexFile;
        boolean loadData = true;
        try {
            File resourceFile = new File(uri);
            String resourceFileName = FilenameUtils.getName(uri.toURL().getPath());
            indexFile = new File("es-consumer-cache", resourceFileName);
            if (resourceFile.exists() && indexFile.exists() && resourceFile.lastModified() > indexFile.lastModified()) {
                log.info("Resource file {} is newer than the existing cached index at {}. Creating new index.", resourceFile, indexFile);
                if (indexFile.isDirectory())
                    FileUtils.deleteQuietly(indexFile);
                else
                    indexFile.delete();
            } else {
                boolean indexFileExisted = indexFile.exists();
                if (!indexFileExisted) {
                    log.info("Creating persistent cache for resource {} at {}.", uri, indexFile);
                } else {
                    log.info("Using existing persistent cache {} for resource {}.", indexFile, uri);
                    loadData = false;
                }
            }
            index = initializeIndex(indexFile.getAbsolutePath());
        } catch (MalformedURLException e) {
            log.error("Could obtain file name from resource URI '{}'", uri, e);
            throw new IllegalStateException(e);
        }
        if (loadData) {
            super.load(aData);
            if (index.requiresExplicitCommit())
                index.commit();
        }
        index.close();
        index.open();
        log.info("There are {} entries in the cache at {}.", index.size(), indexFile);
    }

    @Override
    protected String getValue(String valueString) {
        return valueString;
    }

    @Override
    protected String getKey(String keyString) {
        return keyString;
    }
}
