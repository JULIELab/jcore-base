package de.julielab.jcore.multiplier.gnp;

import de.julielab.jcore.ae.gnp.GNormPlusAnnotator;
import de.julielab.jcore.consumer.gnp.BioCDocumentPopulator;
import de.julielab.jcore.reader.GNormPlusFormatMultiplier;
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

@ResourceMetaData(name = "JCoRe GNormPlus BioC Multiplier", description = "A CAS multiplier to be used with the GNormPlus BioC Format multiplier reader. It wraps the JULIE Lab variant of the GNormPlus gene ID mapper. It is a multiplier because this enables batch-processing of documents with GNormPlus which makes the processing more efficient.", vendor = "JULIE Lab Jena, Germany")
@TypeCapability(inputs = {}, outputs = {"de.julielab.jcore.types.ConceptMention", "de.julielab.jcore.types.Organism"})
public class GNormPlusBioCMultiplier extends GNormPlusFormatMultiplier {
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
            multiplierLogic = new GNormPlusMultiplierLogic(aContext, bioCDocumentPopulator, () -> {
                try {
                    return super.hasNext();
                } catch (AnalysisEngineProcessException e) {
                    log.error("Error when calling hasNext() of the base multiplier");
                    throw new RuntimeException(e);
                }
            }, () -> {
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
        return multiplierLogic.hasNext();
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        return multiplierLogic.next();
    }
}