package de.julielab.jcore.ae.fvr;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FeatureValueReplacementsProvider implements IFeatureValueReplacementsProvider, SharedResourceObject {

    private final static Logger log = LoggerFactory.getLogger(FeatureValueReplacementsProvider.class);

    private Map<String, String> replacements;


    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
        try {
            InputStream is;
            try {
                is = aData.getInputStream();
            } catch (NullPointerException e) {
                log.error("Could not load the replacement file from {}", aData.getUri(), e);
                throw e;
            }
            replacements = readReplacementsFromInputStream(is);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

    }

    /**
     * Reads a replacement file with lines of the form <code>originalValue=replacementValue</code> and returns the
     * respective map.
     *
     * @param is
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Map<String, String> readReplacementsFromInputStream(InputStream is) throws FileNotFoundException,
            IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try {
            Map<String, String> replacements = new HashMap<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0 || line.startsWith("#"))
                    continue;
                String[] split = line.split("\t");
                if (split.length != 2)
                    throw new IllegalArgumentException(
                            "Format error in replacements file: Expected format is 'originalValue<tab>replacementValue' but the input line '" + line
                                    + "' has "
                                    + split.length
                                    + " columns.");
                replacements.put(split[0].trim(), split[1].trim());
            }
            return replacements;
        } finally {
            br.close();
        }
    }

    @Override
    public Map<String, String> getFeatureValueReplacements() {
        return replacements;
    }


}
