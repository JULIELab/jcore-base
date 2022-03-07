package de.julielab.jcore.reader;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;

@ResourceMetaData(name="JCoRe GNormPlus BioC Format Multiplier", description = "Multiplier for GNormPlusFormatMultiplierReader. Takes URIs pointing to BioC collection files that contain annotations created by GNormPlus. For each such file, reads all documents and returns CASes for them until all documents in all collections have been read into a CAS.")
public class GNormPlusFormatMultiplier extends JCasMultiplier_ImplBase {
    private final static Logger log = LoggerFactory.getLogger(GNormPlusFormatMultiplier.class);
    public static final String PARAM_COSTOSYS_CONFIG = "CostosysConfigFile";
    public static final String PARAM_XMI_DOCUMENTS_TABLE = "DocumentsTable";
    private Iterator<URI> currentUriBatch;
    private BioCCasPopulator casPopulator;

@ConfigurationParameter(name=PARAM_COSTOSYS_CONFIG, mandatory = false, description = "Path to the CoStoSys configuration file that is used by the XMI DB writer in the same pipeline, if any. The XMI DB writer requires information about the XMI documents that are already in the database and should be updated with new annotations. The current highest XMI ID must be known to avoid ID collisions. To obtain the ID, it must be received from the database beforehand. This allows to retrieve the information batch wise instead of one-by-one which would be much slower.")
    private String costosysConfiguration;
@ConfigurationParameter(name=PARAM_XMI_DOCUMENTS_TABLE, mandatory = false, description = "Required to retrieve the max XMI ID for use by the XMI DB writer. The schema-qualified name of the XMI document table that the XMI DB writer will write annotations into.")
    private String documentsTable;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        costosysConfiguration = (String) aContext.getConfigParameterValue(PARAM_COSTOSYS_CONFIG);
        documentsTable = (String) aContext.getConfigParameterValue(PARAM_XMI_DOCUMENTS_TABLE);
        if (costosysConfiguration == null ^ documentsTable == null)
            throw new ResourceInitializationException(new IllegalArgumentException("Either both or none parameters must be defined: " + PARAM_COSTOSYS_CONFIG + ", " + PARAM_XMI_DOCUMENTS_TABLE));
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        try {
            Collection<JCoReURI> jcoreUris = JCasUtil.select(jCas, JCoReURI.class);
            if (log.isDebugEnabled())
                log.debug("Received batch of {} BioC XML URIs", jcoreUris.size());
            currentUriBatch = jcoreUris.stream().map(JCoReURI::getUri).map(URI::create).iterator();
        } catch (Throwable e) {
            log.error("Unexpected error", e);
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public boolean hasNext() throws AnalysisEngineProcessException {
        if ((casPopulator == null || casPopulator.documentsLeftInCollection() == 0) && currentUriBatch.hasNext()) {
            URI nextUri = currentUriBatch.next();
            try {
                casPopulator = new BioCCasPopulator(Path.of(nextUri), costosysConfiguration != null ? Path.of(costosysConfiguration) : null, documentsTable);
            } catch (Exception e) {
                log.error("Could not read from {}", nextUri, e);
                throw new AnalysisEngineProcessException(e);
            }
        }
        return casPopulator != null && casPopulator.documentsLeftInCollection() > 0;
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        if (hasNext()) {
            JCas cas = getEmptyJCas();
            try {
                casPopulator.populateWithNextDocument(cas);
                return cas;
            } catch (Exception e) {
                log.error("Could not populate CAS with the next BioC document.", e);
                throw new AnalysisEngineProcessException(e);
            }
        }
        return null;
    }
}
