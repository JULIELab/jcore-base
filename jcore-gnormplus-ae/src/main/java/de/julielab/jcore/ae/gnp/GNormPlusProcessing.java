package de.julielab.jcore.ae.gnp;

import GNormPluslib.GNormPlus;
import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.io.BioCCollectionWriter;
import de.julielab.java.utilities.FileUtilities;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class GNormPlusProcessing {
    private final static Logger log = LoggerFactory.getLogger(GNormPlusProcessing.class);

    public static synchronized void initializeGNormPlus(String setupFileResourcePath, String focusSpecies) throws IOException {
        if (!GNormPlus.initialized) {
            final InputStream setupFileStream = FileUtilities.findResource(setupFileResourcePath);
            if (setupFileStream == null)
                throw new IOException("Could not find resource as file or classpath resource " + setupFileResourcePath);
            GNormPlus.loadConfiguration(setupFileStream, focusSpecies);
            GNormPlus.loadResources(focusSpecies, System.currentTimeMillis());
        }
    }

    public static BioCCollection createEmptyJulieLabBioCCollection() {
        final BioCCollection bioCCollection = new BioCCollection();
        bioCCollection.setDate(new Date().toString());
        bioCCollection.setEncoding("UTF-8");
        bioCCollection.setKey("BioC.key");
        bioCCollection.setSource("JULIE Lab GNormPlus");
        return bioCCollection;
    }

    /**
     * @param bioCCollection
     * @param outputDirectory
     * @return The path of the GNormPlus output file.
     * @throws AnalysisEngineProcessException
     */
    public static Path processWithGNormPlus(BioCCollection bioCCollection, String outputDirectory) throws AnalysisEngineProcessException {
        String collectionId = "collection_including_" + bioCCollection.getDocument(0).getID();
        final Path filePath = Path.of("tmp", collectionId + ".xml");
        final Path outputFilePath = Path.of(outputDirectory.isBlank() ? "tmp" : outputDirectory, collectionId + "processed.xml");
        try {
            if (!Files.exists(filePath.getParent()))
                Files.createDirectory(filePath.getParent());
            try (BioCCollectionWriter w = new BioCCollectionWriter(filePath)) {
                w.writeCollection(bioCCollection);
            }
            GNormPlus.processFile(filePath.toString(), filePath.getFileName().toString(), outputFilePath.toString(), System.currentTimeMillis(), "Test");
        } catch (IOException | XMLStreamException e) {
            log.error("Could not process document {}", collectionId);
            throw new AnalysisEngineProcessException(e);
        }
        return outputFilePath;
    }
}
