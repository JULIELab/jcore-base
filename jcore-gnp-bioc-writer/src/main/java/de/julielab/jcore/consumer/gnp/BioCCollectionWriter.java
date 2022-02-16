package de.julielab.jcore.consumer.gnp;

import com.pengyifan.bioc.BioCCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes a collection of BioC documents into a single file. That file is created within a subdirectory of
 * some base directory und changes over time to avoid overflowing directories.
 */
public class BioCCollectionWriter {
    private final static Logger log = LoggerFactory.getLogger(BioCCollectionWriter.class);
    private int numFilesPerDir;
    private Path baseDir;
    private Path currentDir;
    private int numWrittenIntoCurrentDir;

    public BioCCollectionWriter(int numFilesPerDir, Path baseDir) {
        this.numFilesPerDir = numFilesPerDir;
        this.baseDir = baseDir;
    }

    public void writeBioCCollection(BioCCollection collection) throws XMLStreamException, IOException {
        Path collectionFile = null;
        synchronized (BioCCollectionWriter.class) {
            // currentDir is either null at the very beginning or after a batch of documents have been written
            if (currentDir == null) {
                int i = 0;
                do {
                    currentDir = Path.of(baseDir.toString(), "bioc_collections_" + i++);
                } while (Files.exists(currentDir));
            }
            int i = 0;
            do {
                collectionFile = Path.of(currentDir.toString(), "bioc_collection_" + i++ + ".xml");
            } while (Files.exists(collectionFile));
            if (!Files.exists(collectionFile.getParent())) {
                log.debug("Creating base BioC collection directory {}", baseDir);
                Files.createDirectories(collectionFile.getParent());
            }
        }
        if (collectionFile == null)
            throw new IllegalStateException("No file for the next collection was constructed. This is a programming error.");
        com.pengyifan.bioc.io.BioCCollectionWriter writer = new com.pengyifan.bioc.io.BioCCollectionWriter(collectionFile);
        writer.writeCollection(collection);
        ++numWrittenIntoCurrentDir;
        // "close" the current directory if the number of files for it has been reached
        if (numWrittenIntoCurrentDir >= numFilesPerDir) {
            currentDir = null;
            numWrittenIntoCurrentDir = 0;
        }
    }
}
