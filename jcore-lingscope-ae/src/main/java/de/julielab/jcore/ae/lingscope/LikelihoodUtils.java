package de.julielab.jcore.ae.lingscope;

import de.julielab.java.utilities.FileUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LikelihoodUtils {
    private final static Logger log = LoggerFactory.getLogger(LikelihoodUtils.class);

    /**
     * Reads the dictionary the maps negation or hedge cues to a likelihood. This work is taken from the jcore-likelihood-detection-ae and might not always fit to the cues identified by Lingscope.
     *
     * @param dictFilePath
     * @param likelihoodDict
     * @throws IOException
     */
    public static void loadLikelihoodDict(String dictFilePath, Map<String, String> likelihoodDict) {
        try {
            InputStream resource = FileUtilities.findResource(dictFilePath);
            if (resource == null) {
                log.error("ERR: Could not find likelihood dictionary file (path: "
                        + dictFilePath + ")");
                throw new IllegalArgumentException("Could not find likelihood dictionary at " + dictFilePath);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource))) {
                String line = "";

                while ((line = reader.readLine()) != null) {
                    String[] entry = line.split("\t");

                    if (entry.length != 2) {
                        log.error("ERR: Likelihood dictionary file not in expected format. Critical line: "
                                + line);
                        throw new IllegalArgumentException("Likelihood dictionary has the wrong format (expected: two tab-separated columns). Critical line: " + line);
                    }

                    String indicator = entry[0].trim();
                    String category = entry[1].trim();
                    likelihoodDict.put(indicator, category);
                }

                reader.close();
                log.info("Done loading likelihood dictionary.");
            } catch (IOException e) {
                log.error("Could not read the likelihood dictionary from {}", dictFilePath, e);
            }
        } catch (IOException e) {
            log.error("Could not read the likelihood dictionary from {}", dictFilePath, e);
        }
    }
}
