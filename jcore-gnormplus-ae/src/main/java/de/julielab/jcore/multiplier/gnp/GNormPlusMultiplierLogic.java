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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        if (bioCCasPopulator == null || bioCCasPopulator.documentsLeftInCollection() == 0) {
            final BioCCollection gnormPlusInputCollection = GNormPlusProcessing.createEmptyJulieLabBioCCollection();
            while (baseMultiplierHasNext.get()) {
                final JCas jCas = baseMultiplierNext.get();
                final BioCDocument bioCDocument = bioCDocumentPopulator.populate(jCas);
                gnormPlusInputCollection.addDocument(bioCDocument);
                try {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final GZIPOutputStream os = new GZIPOutputStream(baos);
                    XmiCasSerializer.serialize(jCas.getCas(), os);
                    cachedCasData.add(baos.toByteArray());
                    jCas.release();
                } catch (IOException | SAXException e) {
                    log.error("Error when serializing CAS data for caching purposes.");
                    throw new AnalysisEngineProcessException(e);
                }
            }
            currentCollectionIndex = 0;
            final Path outputFilePath = GNormPlusProcessing.processWithGNormPlus(gnormPlusInputCollection, outputDirectory);
            try {
                bioCCasPopulator = new BioCCasPopulator(outputFilePath);
            } catch (XMLStreamException | IOException e) {
                log.error("Could not read GNormPlus output from {}", outputFilePath);
                throw new AnalysisEngineProcessException(e);
            }
        }
        byte[] currentCasData = cachedCasData.get(currentCollectionIndex);
        final JCas jCas = multiplierGetEmptyCas.get();
        try {
            XmiCasDeserializer.deserialize(new GZIPInputStream(new ByteArrayInputStream(currentCasData)), jCas.getCas());
        } catch (SAXException | IOException e) {
            log.error("Could not deserialize cached CAS data");
            throw new AnalysisEngineProcessException(e);
        }
        bioCCasPopulator.populateWithNextDocument(jCas, true);
        bioCCasPopulator.clearDocument(currentCollectionIndex);
        cachedCasData.set(currentCollectionIndex, null);
        ++currentCollectionIndex;

        return jCas;
    }

    public boolean hasNext() {
        return bioCCasPopulator != null && bioCCasPopulator.documentsLeftInCollection() > 0 || baseMultiplierHasNext.get();
    }
}
