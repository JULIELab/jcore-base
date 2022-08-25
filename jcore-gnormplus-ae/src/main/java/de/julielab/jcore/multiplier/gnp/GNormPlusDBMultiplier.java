package de.julielab.jcore.multiplier.gnp;

import com.pengyifan.bioc.BioCCollection;
import de.julielab.jcore.ae.gnp.GNormPlusAnnotator;
import de.julielab.jcore.consumer.gnp.BioCDocumentPopulator;
import de.julielab.jcore.reader.xmi.XmiDBMultiplier;
import de.julielab.jcore.types.Gene;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

@ResourceMetaData(name = "JCoRe GNormPlus Database Multiplier", description = "A CAS multiplier to be used with the DB XMI multiplier reader. It wraps the JULIE Lab variant of the GNormPlus gene ID mapper. It is a multiplier because this enables batch-processing of documents with GNormPlus which makes the processing more efficient.", vendor = "JULIE Lab Jena, Germany")
@TypeCapability(inputs = {}, outputs = {"de.julielab.jcore.types.ConceptMention", "de.julielab.jcore.types.Organism"})
public class GNormPlusDBMultiplier extends XmiDBMultiplier {
    public static final String PARAM_ADD_GENES = GNormPlusAnnotator.PARAM_ADD_GENES;
    public static final String PARAM_GENE_TYPE_NAME = GNormPlusAnnotator.PARAM_GENE_TYPE_NAME;
    public static final String PARAM_OUTPUT_DIR = GNormPlusAnnotator.PARAM_OUTPUT_DIR;
    public static final String PARAM_GNP_SETUP_FILE = GNormPlusAnnotator.PARAM_GNP_SETUP_FILE;
    private final static Logger log = LoggerFactory.getLogger(GNormPlusDBMultiplier.class);
    @ConfigurationParameter(name = PARAM_ADD_GENES, mandatory = false, defaultValue = "false", description = GNormPlusAnnotator.DESC_ADD_GENES)
    private boolean addGenes;
    @ConfigurationParameter(name = PARAM_GNP_SETUP_FILE, mandatory = false, description = GNormPlusAnnotator.DESC_GNP_SETUP_FILE)
    private String setupFile;
    @ConfigurationParameter(name = PARAM_GENE_TYPE_NAME, mandatory = false, defaultValue = "de.julielab.jcore.types.Gene", description = GNormPlusAnnotator.DESC_GENE_TYPE_NAME)
    private String geneTypeName;
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR, mandatory = false, description = GNormPlusAnnotator.DESC_OUTPUT_DIR)
    private String outputDirectory;

    private BioCDocumentPopulator bioCDocumentPopulator;
//    private BioCCasPopulator bioCCasPopulator;

    private BioCCollection currentGNormPlusProcessedCollection;
//    private int currentCollectionIndex;
//    private List<byte[]> cachedCasData;

    private GNormPlusMultiplierLogic multiplierLogic;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        addGenes = (boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ADD_GENES)).orElse(false);
        geneTypeName = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_GENE_TYPE_NAME)).orElse(Gene.class.getCanonicalName());
        try {
            bioCDocumentPopulator = new BioCDocumentPopulator(addGenes, geneTypeName);
        } catch (ClassNotFoundException e) {
            log.error("Gene annotation class {} could not be found.", geneTypeName, e);
            throw new ResourceInitializationException(e);
        }
        try {
            multiplierLogic = new GNormPlusMultiplierLogic(aContext, bioCDocumentPopulator, () -> super.hasNext(), () -> {
                try {
                    return (JCas) super.next();
                } catch (AnalysisEngineProcessException e) {
                    log.error("Error when calling next() of the base multiplier.");
                    throw new RuntimeException(e);
                }
            }, () -> getEmptyJCas());
        } catch (IOException e) {
            log.error("Could not initialize GNormPlus", e);
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public boolean hasNext() {
//        return currentCollectionIndex < currentGNormPlusProcessedCollection.getDocmentCount() || super.hasNext();
        return multiplierLogic.hasNext();
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        return multiplierLogic.next();
//        if (bioCCasPopulator == null || bioCCasPopulator.documentsLeftInCollection() == 0) {
//            final BioCCollection gnormPlusInputCollection = GNormPlusProcessing.createEmptyJulieLabBioCCollection();
//            while (super.hasNext()) {
//                final JCas jCas = (JCas) super.next();
//                final BioCDocument bioCDocument = bioCDocumentPopulator.populate(jCas);
//                gnormPlusInputCollection.addDocument(bioCDocument);
//                try {
//                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    final GZIPOutputStream os = new GZIPOutputStream(baos);
//                    XmiCasSerializer.serialize(jCas.getCas(), os);
//                    cachedCasData.add(baos.toByteArray());
//                    jCas.release();
//                } catch (IOException | SAXException e) {
//                    log.error("Error when serializing CAS data for caching purposes.");
//                    throw new AnalysisEngineProcessException(e);
//                }
//            }
//            currentCollectionIndex = 0;
//            final Path outputFilePath = GNormPlusProcessing.processWithGNormPlus(gnormPlusInputCollection, outputDirectory);
//            try {
//                bioCCasPopulator = new BioCCasPopulator(outputFilePath);
//            } catch (XMLStreamException | IOException e) {
//                log.error("Could not read GNormPlus output from {}", outputFilePath);
//                throw new AnalysisEngineProcessException(e);
//            }
//        }
//        byte[] currentCasData = cachedCasData.get(currentCollectionIndex);
//        final JCas jCas = getEmptyJCas();
//        try {
//            XmiCasDeserializer.deserialize(new GZIPInputStream(new ByteArrayInputStream(currentCasData)), jCas.getCas());
//        } catch (SAXException | IOException e) {
//            log.error("Could not deserialize cached CAS data");
//            throw new AnalysisEngineProcessException(e);
//        }
//        bioCCasPopulator.populateWithNextDocument(jCas, true);
//        bioCCasPopulator.clearDocument(currentCollectionIndex);
//        cachedCasData.set(currentCollectionIndex, null);
//        ++currentCollectionIndex;
//
//        return jCas;
    }
}
