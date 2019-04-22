package de.julielab.jcore.consumer.es.sharedresources;

import de.julielab.java.utilities.IOStreamUtilities;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ListProvider implements IListProvider {
    private final static Logger log = LoggerFactory.getLogger(ListProvider.class);
    private List<String> list;
    private Set<String> set;

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
        try {
            list = new ArrayList<>();
            try (BufferedReader br = IOStreamUtilities.getReaderFromInputStream(JCoReTools.resolveExternalResourceGzipInputStream(aData))) {
                br.lines().filter(Predicate.not(String::isBlank)).filter(line -> !line.startsWith("#")).map(String::intern).forEach(list::add);
            }
            ((ArrayList<String>) list).trimToSize();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        } catch (NullPointerException e) {
            log.error("Could not read file from {}", aData.getUri());
            throw e;
        }

    }

    /**
     * Returns the loaded list of strings where all strings are internalized.
     */
    @Override
    public List<String> getList() {
        return list;
    }

    /**
     * Returns the loaded set of strings where all strings are internalized.
     */
    @Override
    public Set<String> getAsSet() {
        if (null == set)
            set = new HashSet<>(list);
        return set;

    }

}
