package de.julielab.jcore.multiplier.gnp;

import de.julielab.jcore.ae.gnp.GNormPlusAnnotator;
import de.julielab.jcore.consumer.gnp.BioCDocumentPopulator;
import de.julielab.jcore.multiplier.pmc.PMCDBMultiplier;
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

import static de.julielab.jcore.ae.gnp.GNormPlusAnnotator.*;

@ResourceMetaData(name = "JCoRe GNormPlus PMC Database Multiplier", description = "A CAS multiplier to be used with the DB PMC multiplier reader in place of the DB PMC multiplier. It wraps the JULIE Lab variant of the GNormPlus gene ID mapper. It is a multiplier because this enables batch-processing of documents with GNormPlus which makes the processing more efficient.", vendor = "JULIE Lab Jena, Germany")
@TypeCapability(inputs = {}, outputs = {"de.julielab.jcore.types.ConceptMention", "de.julielab.jcore.types.Organism"})
public class GNormPlusPMCDBMultiplier extends PMCDBMultiplier {
    public static final String PARAM_SKIP_UNCHANGED_DOCUMENTS = "SkipUnchangedDocuments";
    private final static Logger log = LoggerFactory.getLogger(GNormPlusPMCDBMultiplier.class);
    private static boolean shutdownHookInstalled = false;
    @ConfigurationParameter(name = PARAM_USE_EXISTING_GENE_ANNOTATIONS, mandatory = false, defaultValue = "false", description = GNormPlusAnnotator.DESC_USE_EXISTING_GENES)
    private boolean addExistingGenes;
    @ConfigurationParameter(name = PARAM_OUTPUT_GENE_TYPE_NAME, mandatory = false, defaultValue = "de.julielab.jcore.types.Gene", description = DESC_OUTPUT_GENE_TYPE_NAME)
    private String outputGeneTypeName;
    @ConfigurationParameter(name = PARAM_GNP_SETUP_FILE, mandatory = false, description = GNormPlusAnnotator.DESC_GNP_SETUP_FILE)
    private String setupFile;
    @ConfigurationParameter(name = PARAM_INPUT_GENE_TYPE_NAME, mandatory = false, defaultValue = "de.julielab.jcore.types.Gene", description = GNormPlusAnnotator.DESC_INPUT_GENE_TYPE_NAME)
    private String geneTypeName;
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR, mandatory = false, description = GNormPlusAnnotator.DESC_OUTPUT_DIR)
    private String outputDirectory;
    @ConfigurationParameter(name = PARAM_FOCUS_SPECIES, mandatory = false, description = DESC_FOCUS_SPECIES)
    private String focusSpecies;
    @ConfigurationParameter(name = PARAM_SKIP_UNCHANGED_DOCUMENTS, mandatory = false, description = "Whether to omit GNormPlus processing on documents that already exist in the XMI database table and whose document text has not changed.")
    private boolean skipUnchangedDocuments;
    private BioCDocumentPopulator bioCDocumentPopulator;
    private GNormPlusMultiplierLogic multiplierLogic;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        addExistingGenes = (boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_USE_EXISTING_GENE_ANNOTATIONS)).orElse(false);
        geneTypeName = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_INPUT_GENE_TYPE_NAME)).orElse(Gene.class.getCanonicalName());
        skipUnchangedDocuments = (boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_SKIP_UNCHANGED_DOCUMENTS)).orElse(false);
        try {
            bioCDocumentPopulator = new BioCDocumentPopulator(addExistingGenes, geneTypeName);
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
            }, () -> getEmptyJCas(),
                skipUnchangedDocuments);
        } catch (IOException e) {
            log.error("Could not initialize GNormPlus", e);
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return multiplierLogic.hasNext();
        } catch (Throwable t) {
            log.error("Error when checking hasNext() on multiplier", t);
        }
        return false;
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        try {
            return multiplierLogic.next();
        } catch (Throwable t) {
            log.error("Error when retrieving next multiplier CAS", t);
            throw new AnalysisEngineProcessException(t);
        }
    }
}
