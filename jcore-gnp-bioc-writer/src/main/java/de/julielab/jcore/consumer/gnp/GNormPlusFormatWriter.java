package de.julielab.jcore.consumer.gnp;

import com.pengyifan.bioc.BioCCollection;
import com.pengyifan.bioc.BioCDocument;
import de.julielab.jcore.types.Gene;
import de.julielab.jcore.utility.JCoReTools;
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

import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

@ResourceMetaData(name = "JCoRe GNormPlus BioC Writer", description = "Writes CAS documents into the BioC XML format used by the gene tagger and normalizer GNormPlus.", vendor = "JULIE Lab Jena, Germany")
@TypeCapability(inputs = {}, outputs = {})
public class GNormPlusFormatWriter extends JCasAnnotator_ImplBase {

    public static final String PARAM_NUM_DOCS_PER_FILE = "NumDocsPerFile";
    public static final String PARAM_NUM_FILES_PER_DIR = "NumFilesPerDir";
    public static final String PARAM_BASE_DIR = "BaseDirectory";
    public static final String PARAM_ADD_GENES = "AddGenes";
    public static final String PARAM_GENE_TYPE_NAME = "GeneTypeName";
    private final static Logger log = LoggerFactory.getLogger(GNormPlusFormatWriter.class);
    @ConfigurationParameter(name = PARAM_NUM_DOCS_PER_FILE, description = "The number of documents (i.e. CASes) that should be written into a single BioC XML file.")
    private int numDocsPerFile;
    @ConfigurationParameter(name = PARAM_NUM_FILES_PER_DIR, description = "The number of files that should be put in a directory before a new one is created.")
    private int numDocsPerDir;
    @ConfigurationParameter(name = PARAM_BASE_DIR, description = "The base directory into which to create new directories that contain the actual BioC collection files.")
    private String baseDirectory;
    @ConfigurationParameter(name=PARAM_ADD_GENES, mandatory = false, defaultValue = "false", description = "If set to true, all Gene annotations in the CAS will be added to the BioC documents. The default type used is de.julielab.jcore.types.Gene. This can be changed with the " + PARAM_GENE_TYPE_NAME + " parameter.")
    private boolean addGenes;
    @ConfigurationParameter(name=PARAM_GENE_TYPE_NAME, mandatory = false, defaultValue = "de.julielab.jcore.types.Gene", description = "The UIMA type denoting gene annotations that should be written into the BioC format when the " + PARAM_ADD_GENES + " parameter is set to true.")
    private String geneTypeName;

    private BioCDocumentPopulator bioCDocumentPopulator;
    private BioCCollectionWriter bioCCollectionWriter;
    private BioCCollection currentCollection;

    /**
     * This method is called a single time by the framework at component
     * creation. Here, descriptor parameters are read and initial setup is done.
     */
    @Override
    public void initialize(final UimaContext aContext) throws ResourceInitializationException {
        numDocsPerFile = (int) aContext.getConfigParameterValue(PARAM_NUM_DOCS_PER_FILE);
        numDocsPerDir = (int) aContext.getConfigParameterValue(PARAM_NUM_FILES_PER_DIR);
        baseDirectory = (String) aContext.getConfigParameterValue(PARAM_BASE_DIR);
        addGenes = (boolean) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_ADD_GENES)).orElse(false);
        geneTypeName = (String) Optional.ofNullable(aContext.getConfigParameterValue(PARAM_GENE_TYPE_NAME)).orElse(Gene.class.getCanonicalName());

        try {
            bioCDocumentPopulator = new BioCDocumentPopulator(addGenes, geneTypeName);
            bioCCollectionWriter = new BioCCollectionWriter(numDocsPerDir, Path.of(baseDirectory));

            currentCollection = new BioCCollection("UTF-8", "1.0", new Date().toString(), true, "JCoRe GNormPlus BioC Writer", "PubTator.key");
        } catch (ClassNotFoundException e) {
            log.error("Gene annotation class {} could not be found.", geneTypeName, e);
            throw new ResourceInitializationException(e);
        }
    }

    /**
     * This method is called for each document going through the component. This
     * is where the actual work happens.
     */
    @Override
    public void process(final JCas jCas) throws AnalysisEngineProcessException {
        try {
            BioCDocument doc = bioCDocumentPopulator.populate(jCas);
            if (doc.getPassageCount() > 0)
                currentCollection.addDocument(doc);
            if (currentCollection.getDocmentCount() >= numDocsPerFile) {
                bioCCollectionWriter.writeBioCCollection(currentCollection);
                currentCollection.clearDocuments();
                currentCollection.clearInfons();
            }
        } catch (Exception e) {
            log.error("Exception was raised for document {}", JCoReTools.getDocId(jCas));
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
                bioCCollectionWriter.writeBioCCollection(currentCollection);
        } catch (Exception e) {
            log.error("Could not write final batch of BioCDocuments.", e);
            throw new AnalysisEngineProcessException(e);
        }
    }
}

