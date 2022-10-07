package de.julielab.jcore.multiplier.gnp;

import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.BioCDocument;
import de.julielab.jcore.ae.gnp.GNormPlusProcessing;
import de.julielab.jcore.consumer.gnp.BioCDocumentPopulator;
import de.julielab.jcore.reader.BioCCasPopulator;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static de.julielab.jcore.ae.gnp.GNormPlusAnnotator.*;

public class GNormPlusMultiplierLogic {
    private final static Logger log = LoggerFactory.getLogger(GNormPlusMultiplierLogic.class);
    private BioCDocumentPopulator bioCDocumentPopulator;
    private BioCCasPopulator bioCCasPopulator;
    private String outputDirectory;
    private Supplier<Boolean> baseMultiplierHasNext;
    private Supplier<JCas> baseMultiplierNext;
    private Supplier<JCas> multiplierGetEmptyCas;
    private int currentCollectionIndex;
    private List<byte[]> cachedCasData;

    public GNormPlusMultiplierLogic(UimaContext aContext, BioCDocumentPopulator bioCDocumentPopulator, Supplier<Boolean> baseMultiplierHasNext, Supplier<JCas> baseMultiplierNext, Supplier<JCas> multiplierGetEmptyCas) throws IOException {
        String setupFile = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_GNP_SETUP_FILE)).orElse("/de/julielab/jcore/ae/gnp/config/setup_do_ner.txt");
        String focusSpecies = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_FOCUS_SPECIES)).orElse("");
        outputDirectory = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_OUTPUT_DIR)).orElse("");
        this.bioCDocumentPopulator = bioCDocumentPopulator;
        this.baseMultiplierHasNext = baseMultiplierHasNext;
        this.baseMultiplierNext = baseMultiplierNext;
        this.multiplierGetEmptyCas = multiplierGetEmptyCas;
        cachedCasData = new ArrayList<>();
        currentCollectionIndex = 0;

        GNormPlusProcessing.initializeGNormPlus(setupFile, focusSpecies);
    }

    public AbstractCas next() throws AnalysisEngineProcessException {
        try {
            // Process the incoming documents batch-wise (this is why we use a multiplier here so we have access
            // to whole batches). This checks if we still have processed documents or if we need to process the next
            // batch.
            if (bioCCasPopulator == null || bioCCasPopulator.documentsLeftInCollection() == 0) {
                System.out.println("Memory before batch processing:");
                final Runtime rt = Runtime.getRuntime();
                final long totalMemory = rt.totalMemory();
                final long freeMemory = rt.freeMemory();
                final long maxMemory = rt.maxMemory();
                Function<Long, Double> b2g = bytes -> bytes / 1000000000d;
                System.out.println("[GNPMultiplierLogic] Free memory: " + freeMemory + "bytes (" + b2g.apply(freeMemory) + "GB), max memory: " + maxMemory + "bytes ("+b2g.apply(maxMemory) + "GB), total memory: " + totalMemory + "bytes ("+b2g.apply(totalMemory) + "GB)");
                currentCollectionIndex = 0;
                final BioCCollection gnormPlusInputCollection = GNormPlusProcessing.createEmptyJulieLabBioCCollection();
                // We first retrieve the whole current batch from the super multiplier and serialize the CASes
                // to XMI. We do that because we only have one CAS at a time and, thus, must store the data
                // of the whole batch. We can then later deserialize the documents and add the GNP annotations to it.
                // This allows batch-processing within GNP which reduces file writes and reads (GNP internally
                // writes a lot of temporary files that contain all the documents given to it in one single batch file).
                cachedCasData.clear();
                while (baseMultiplierHasNext.get()) {
                    final JCas jCas = baseMultiplierNext.get();
                    final BioCDocument bioCDocument = bioCDocumentPopulator.populate(jCas);
                    gnormPlusInputCollection.addDocument(bioCDocument);
                    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        try (final GZIPOutputStream os = new GZIPOutputStream(baos)) {
                            XmiCasSerializer.serialize(jCas.getCas(), os);
                        }
                        cachedCasData.add(baos.toByteArray());
                        jCas.release();
                    } catch (IOException | SAXException e) {
                        log.error("Error when serializing CAS data for caching purposes.");
                        throw new AnalysisEngineProcessException(e);
                    }
                }
                // now process the whole batch with GNP
                final Path outputFilePath = GNormPlusProcessing.processWithGNormPlus(gnormPlusInputCollection, outputDirectory);
                try {
                    bioCCasPopulator = new BioCCasPopulator(outputFilePath);
                    // delete the GNP output if we don't want to keep it
                    if(outputDirectory.isBlank()) {
                        Files.delete(outputFilePath);
                    }
                } catch (XMLStreamException | IOException e) {
                    log.error("Could not read GNormPlus output from {}", outputFilePath);
                    throw new AnalysisEngineProcessException(e);
                }
            }
            // Now we have a batch of documents processed with GNP. Get the next document from the cache and
            // add the GNP annotations to it.
            byte[] currentCasData = cachedCasData.get(currentCollectionIndex);
            final JCas jCas = multiplierGetEmptyCas.get();
            try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(currentCasData))) {
                XmiCasDeserializer.deserialize(is, jCas.getCas());
            } catch (SAXException | IOException e) {
                log.error("Could not deserialize cached CAS data");
                throw new AnalysisEngineProcessException(e);
            }
            bioCCasPopulator.populateWithNextDocument(jCas, true);
            bioCCasPopulator.clearDocument(currentCollectionIndex);
            cachedCasData.set(currentCollectionIndex, null);
            ++currentCollectionIndex;

            return jCas;
        } catch (AnalysisEngineProcessException e) {
            log.error("Error while retrieving or processing data for/with GNormPlus", e);
            throw e;
        }
    }

    public boolean hasNext() {
        try {
            return bioCCasPopulator != null && bioCCasPopulator.documentsLeftInCollection() > 0 || baseMultiplierHasNext.get();
        } catch (Throwable t) {
            log.error("Could not determine hasNext()", t);
            throw t;
        }
    }
}
