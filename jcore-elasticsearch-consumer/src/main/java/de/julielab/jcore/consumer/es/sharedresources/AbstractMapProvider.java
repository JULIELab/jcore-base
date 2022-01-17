package de.julielab.jcore.consumer.es.sharedresources;

import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Base class for resources that map one term to another. Uses a HashMap. The trivial instantiable subclass is {@link MapProvider}.</p>
 * <p>This class is abstract because it is generic. To work with other data types than strings, the {@link #getKey(String)} and {@link #getValue(String)}
 * methods are overridden by subclasses to deliver the correct data types from the string input.</p>
 * <p>Subclasses deal with maps where the keys and/or values are not strings but numbers. Other subclasses deal with
 * String but use a persistent data structure to deal with very large maps.</p>
 *
 * @param <K>
 * @param <V>
 */
public abstract class AbstractMapProvider<K, V> implements IMapProvider<K, V> {
    protected final Logger log;
    protected boolean reverse = false;
    protected Map<K, V> map;

    public AbstractMapProvider(Logger log) {
        this.log = log;
        map = new HashMap<>();
    }

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
//            map = new HashMap<>();
            String line;
            String splitExpression = "\t";
            int numEntries = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#"))
                    continue;
                ++numEntries;
                String[] split = line.split(splitExpression);
                if (split.length != 2) {
                    splitExpression = "\\s+";
                    split = line.split(splitExpression);
                }
                if (split.length != 2)
                    throw new IllegalArgumentException("Format error in map file: Expected format is 'originalValue<tab>mappedValue' but the input line '" + line
                            + "' has " + split.length + " columns.");
                if (reverse)
                    put(getKey(split[1]), getValue(split[0]));
                else
                    put(getKey(split[0]), getValue(split[1]));
            }
            log.info("Finished reading resource {} and got {} entries.", aData.getUri(), numEntries);
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

    protected abstract void put(K key, V value);

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
