package de.julielab.jcore.consumer.es.sharedresources;

import com.google.gson.Gson;
import de.julielab.java.utilities.IOStreamUtilities;
import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Base class for addon terms (i.e. terms to be added to some key term, like synonyms or hypernyms) that uses a HashMap.</p>
 * <p>Subclasses of this class use other data structures to store and retrieve the addon terms. Useful for large numbers of such terms.</p>
 */
public class AddonTermsProvider implements IAddonTermsProvider {
    protected final Logger log;

    protected Map<String, String[]> addonTerms;

    public AddonTermsProvider(Logger log) {
        this.log = log;
        addonTerms = new HashMap<>();
    }

    protected void put(String term, String[] addonArray) {
        addonTerms.put(term, addonArray);
    }

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
        try {
            URI uri = aData.getUri();
            log.info("Loading addon terms from " + uri);
            int addons = 0;
            InputStream inputStream;
            try {
                inputStream = JCoReTools.resolveExternalResourceGzipInputStream(aData);
            } catch (Exception e) {
                throw new IOException("Could not read from " + aData.getUri() + ": Resource not found.");
            }
            List<String> addonLines = IOStreamUtilities.getLinesFromInputStream(inputStream);
            for (String line : addonLines) {
                if (line.trim().length() == 0 || line.startsWith("#"))
                    continue;
                String[] mapping = line.split("\t");
                if (mapping.length != 2)
                    throw new IllegalArgumentException("Format problem with addon terms line " + line + ": Not exactly one tab character.");
                // we use internalization to reduce memory
                // requirements
                String term = mapping[0].trim().intern();
                String[] addonArray;
                if (mapping[1].startsWith("[") && mapping[1].endsWith("]")) {
                    // This looks like a JSON array
                    Gson gson = new Gson();
                    addonArray = gson.fromJson(mapping[1], String[].class);
                } else {
                    addonArray = mapping[1].split("\\|");
                }
                for (int i = 0; i < addonArray.length; i++) {
                    String trimmedAddon = addonArray[i].trim();
                    // we use internalization to reduce memory
                    // requirements
                    addonArray[i] = trimmedAddon.intern();
                    addons++;
                }
                put(term, addonArray);
            }
            log.info("Loaded {} addons for {} terms.", addons, addonTerms.size());
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public Map<String, String[]> getAddonTerms() {
        return addonTerms;
    }

}
