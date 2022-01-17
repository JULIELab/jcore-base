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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Reads the original input file and converts it into a persistent index. This index is re-used in subsequent pipeline runs.
 */
abstract public class PersistentIndexAddonTermsProvider extends AddonTermsProvider {
    public static final int MAXIMUM_MEMCACHE_SIZE = 10000;
    private final LoadingCache<String, Optional<String[]>> cache;
    private StringIndex index;

    public PersistentIndexAddonTermsProvider(Logger log) {
        super(log);
        addonTerms = new Map<>() {
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
            public String[] get(Object key) {
                try {
                    return cache.get((String) key).orElse(null);
                } catch (ExecutionException e) {
                    log.error("Could not retrieve value from the cache for key '{}'.", key);
                    throw new IllegalStateException();
                }
            }

            @Nullable
            @Override
            public String[] put(String key, String[] value) {
                throw new NotImplementedException();
            }

            @Override
            public String[] remove(Object key) {
                throw new NotImplementedException();
            }

            @Override
            public void putAll(@NotNull Map<? extends String, ? extends String[]> m) {
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
            public Collection<String[]> values() {
                throw new NotImplementedException();
            }

            @NotNull
            @Override
            public Set<Entry<String, String[]>> entrySet() {
                throw new NotImplementedException();
            }
        };
        cache = CacheBuilder.newBuilder().maximumSize(MAXIMUM_MEMCACHE_SIZE).expireAfterAccess(Duration.ofHours(1)).build(new CacheLoader<>() {
            @Override
            public Optional<String[]> load(String s) {
                return Optional.ofNullable(index.getArray(s));
            }
        });
    }

    protected abstract StringIndex initializeIndex(String cachePath);

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
        // prepare the persistent index
        URI uri = aData.getUri();
        File indexFile = null;
        boolean loadData = true;
        try {
            File resourceFile = new File(uri);
            String resourceFileName = FilenameUtils.getName(uri.toURL().getPath());
            indexFile = new File("es-consumer-cache", resourceFileName);
            if (resourceFile.exists() && indexFile.exists() && resourceFile.lastModified() > indexFile.lastModified()) {
                log.info("Resource file {} is newer than the existing cached index at {}. Creating new index.", resourceFile, indexFile);
                if (indexFile.isDirectory()) {
                    log.info("Deleting index directory {}", indexFile);
                    FileUtils.deleteDirectory(indexFile);
                } else {
                    log.info("Deleting index file {}", indexFile);
                    indexFile.delete();
                }
            } else {
                boolean indexFileExisted = indexFile.exists();
                if (!indexFileExisted) {
                    log.info("Creating persistent cache for resource {} at {}.", uri, indexFile);
                }
                else {
                    log.info("Using existing persistent cache {} for resource {}.", indexFile, uri);
                    loadData = false;
                }
            }
            index = initializeIndex(indexFile.getAbsolutePath());
        } catch (MalformedURLException e) {
            log.error("Could obtain file name from resource URI '{}'", uri, e);
            throw new IllegalStateException(e);
        } catch (IOException e) {
            log.error("Could not delete index file {}", indexFile, e);
            throw new ResourceInitializationException(e);
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
    protected void put(String term, String[] addonArray) {
        index.put(term, addonArray);
    }
}
