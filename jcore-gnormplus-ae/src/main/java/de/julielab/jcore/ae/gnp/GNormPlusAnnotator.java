package de.julielab.jcore.ae.gnp;

import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.BioCDocument;
import de.julielab.jcore.consumer.gnp.BioCDocumentPopulator;
import de.julielab.jcore.reader.BioCCasPopulator;
import de.julielab.jcore.types.Gene;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@ResourceMetaData(name = "JCoRe GNormPlus Annotator", description = "Wrapper for the JULIE Lab variant of the GNormPlus gene ID mapper.", vendor = "JULIE Lab Jena, Germany")
@TypeCapability(inputs = {}, outputs = {"de.julielab.jcore.types.ConceptMention", "de.julielab.jcore.types.Organism"})
public class GNormPlusAnnotator extends JCasAnnotator_ImplBase {

    public static final String PARAM_ADD_GENES = "AddGenes";
    public static final String DESC_GENE_TYPE_NAME = "The UIMA type denoting gene annotations that should be written into the BioC format when the " + PARAM_ADD_GENES + " parameter is set to true.";
    public static final String PARAM_GENE_TYPE_NAME = "GeneTypeName";
    public static final String DESC_ADD_GENES = "If set to true, all Gene annotations in the CAS will be added to the BioC documents. The default type used is de.julielab.jcore.types.Gene. This can be changed with the " + PARAM_GENE_TYPE_NAME + " parameter.";
    public static final String PARAM_GNP_SETUP_FILE = "GNormPlusSetupFile";
    public static final String PARAM_FOCUS_SPECIES = "FocusSpecies";
    public static final String PARAM_OUTPUT_DIR = "OutputDirectory";
    public static final String DESC_GNP_SETUP_FILE = "File path or class path resource path to the setup.txt file for GNormPlus. If not specified, a default setup file is loaded that expects the Dictionary/ directory directly under the working directory, performs gene recognition with the CRF and thus expects the GNormPlus CRF directory directly under the working directory and maps the found genes to NCBI gene IDs for all organisms.";
    public static final String DESC_FOCUS_SPECIES = "If given, all gene mentions are assigned to this NCBI taxonomy ID, i.e. species recognition is omitted.";
    public static final String DESC_OUTPUT_DIR = "Optional. If specified, the GNormPlus output files in BioC format will be saved to the given directory. In this way, this component can be used directly as a BioC XML writer through the GNormPlus algorithm.";
    private final static Logger log = LoggerFactory.getLogger(GNormPlusAnnotator.class);
    @ConfigurationParameter(name = PARAM_ADD_GENES, mandatory = false, defaultValue = "false", description = DESC_ADD_GENES)
    private boolean addGenes;
    @ConfigurationParameter(name = PARAM_GENE_TYPE_NAME, mandatory = false, defaultValue = "de.julielab.jcore.types.Gene", description = DESC_GENE_TYPE_NAME)
    private String geneTypeName;
    @ConfigurationParameter(name = PARAM_GNP_SETUP_FILE, mandatory = false, description = DESC_GNP_SETUP_FILE)
    private String setupFile;
    @ConfigurationParameter(name = PARAM_FOCUS_SPECIES, mandatory = false, description = DESC_FOCUS_SPECIES)
    private String focusSpecies;
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR, mandatory = false, description = DESC_OUTPUT_DIR)
    private String outputDirectory;

    private BioCDocumentPopulator bioCDocumentPopulator;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        addGenes = (boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ADD_GENES)).orElse(false);
        geneTypeName = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_GENE_TYPE_NAME)).orElse(Gene.class.getCanonicalName());
        setupFile = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_GNP_SETUP_FILE)).orElse("/de/julielab/jcore/ae/gnp/config/setup_do_ner.txt");
        focusSpecies = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_FOCUS_SPECIES)).orElse("");
        outputDirectory = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_OUTPUT_DIR)).orElse("");

        try {
            GNormPlusProcessing.initializeGNormPlus(setupFile, focusSpecies);
        } catch (IOException e) {
            log.error("Could not find resource {}", setupFile);
            throw new ResourceInitializationException(e);
        }
        try {
            bioCDocumentPopulator = new BioCDocumentPopulator(addGenes, geneTypeName);
        } catch (ClassNotFoundException e) {
            log.error("Gene annotation class {} could not be found.", geneTypeName, e);
            throw new ResourceInitializationException(e);
        }

        try {
            if (!outputDirectory.isBlank())
                Files.createDirectories(Path.of(outputDirectory));
        } catch (IOException e) {
            log.error("Could not create the output directory {}", outputDirectory);
            throw new ResourceInitializationException(e);
        }
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas aJCas) throws AnalysisEngineProcessException {
        final BioCDocument bioCDocument = bioCDocumentPopulator.populate(aJCas);
        BioCCollection bioCCollection = GNormPlusProcessing.createEmptyJulieLabBioCCollection();
        bioCCollection.addDocument(bioCDocument);
        String outputDirectory = this.outputDirectory;
        final Path outputFilePath = GNormPlusProcessing.processWithGNormPlus(bioCCollection, outputDirectory);

        try {
            final BioCCasPopulator bioCCasPopulator = new BioCCasPopulator(outputFilePath);
            bioCCasPopulator.populateWithNextDocument(aJCas, true);
        } catch (XMLStreamException | IOException e) {
            log.error("Could not read GNormPlus output file {}");
            throw new AnalysisEngineProcessException(e);
        }
//        try {
//            Files.delete(filePath);
//        } catch (IOException e) {
//            log.error("Could not delete temporary file {}", filePath);
//            throw new AnalysisEngineProcessException(e);
//        }
        try {
            if (outputDirectory.isBlank() && Files.exists(outputFilePath))
                Files.delete(outputFilePath);
        } catch (IOException e) {
            log.error("Could not delete temporary file {}", outputFilePath);
            throw new AnalysisEngineProcessException(e);
        }

    }


}
