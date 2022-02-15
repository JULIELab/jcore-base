package de.julielab.jcore.consumer.gnp;

import com.pengyifan.bioc.BioCCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

/**
 * Writes a collection of BioC documents into a single file. That file is created within a subdirectory of
 * some base directory und changes over time to avoid overflowing directories.
 */
public class BioCCollectionWriter {
    private final static Logger log = LoggerFactory.getLogger(BioCCollectionWriter.class);
    private int numFilesPerDir;
    private File baseDir;
    private File currentDir;
    private int numWrittenIntoCurrentDir;

    public BioCCollectionWriter(int numFilesPerDir, File baseDir) {
        this.numFilesPerDir = numFilesPerDir;
        this.baseDir = baseDir;
    }

    public void writeBioCCollection(BioCCollection collection) throws XMLStreamException, IOException {
        File collectionFile = null;
        synchronized (BioCCollectionWriter.class) {
            if (!baseDir.exists()) {
                log.debug("Creating base BioC collection directory {}", baseDir);
                baseDir.mkdirs();
            }
            if (currentDir == null) {
                int i = 0;
                do {
                    currentDir = new File(baseDir, "bioc_collections_" + i++);
                } while (currentDir.exists());
                i = 0;
                do {
                    collectionFile = new File(currentDir, "bioc_collection_" + i++ + ".xml");
                } while (collectionFile.exists());
            }
        }

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
