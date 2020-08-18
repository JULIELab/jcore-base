package de.julielab.jcore.consumer.es.sharedresources;

import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public abstract class AbstractMapProvider<K, V> implements IMapProvider<K, V> {
    private final static Logger log = LoggerFactory.getLogger(AbstractMapProvider.class);
    protected boolean reverse = false;
    private HashMap<K, V> map;

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
        BufferedReader br = null;
        try {
            InputStreamReader is;
            try {
                is = new InputStreamReader(JCoReTools.resolveExternalResourceGzipInputStream(aData));
            } catch (Exception e) {
                throw new IOException("Resource " + aData.getUri() + " not found");
            }
            br = new BufferedReader(is);
            map = new HashMap<>();
            String line;
            String splitExpression = "\t";
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#"))
                    continue;
                String[] split = line.split(splitExpression);
                if (split.length != 2) {
                    splitExpression = "\\s+";
                    split = line.split(splitExpression);
                }
                if (split.length != 2)
                    throw new IllegalArgumentException("Format error in map file: Expected format is 'originalValue<tab>mappedValue' but the input line '" + line
                            + "' has " + split.length + " columns.");
                if (reverse)
                    map.put(getKey(split[1]), getValue(split[0]));
                else
                    map.put(getKey(split[0]), getValue(split[1]));
            }
            log.info("Finished reading resource {}", aData.getUri());
            log.info("Copying {} values into a fresh HashMap of the exact correct size", map.size());
            HashMap<K, V> tmp = new HashMap<>(map.size(), 1f);
            tmp.putAll(map);
            map = tmp;
            log.info("Done.");
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        } finally {
            try {
                if (null != br)
                    br.close();
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        }
    }

    protected abstract V getValue(String valueString);

    protected abstract K getKey(String keyString);

    /**
     * Returns the loaded map. All strings - keys and values - are internalized.
     */
    @Override
    public Map<K, V> getMap() {
        return map;
    }

}
