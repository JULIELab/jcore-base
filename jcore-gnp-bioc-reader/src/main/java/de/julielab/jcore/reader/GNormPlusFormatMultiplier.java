package de.julielab.jcore.reader;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;

@ResourceMetaData(name="GNormPlusFormatMultiplier", description = "Multiplier for GNormPlusFormatMultiplierReader. Takes URIs pointing to BioC collection files that contain annotations created by GNormPlus. For each such file, reads all documents and returns CASes for them until all documents in all collections have been read into a CAS.")
public class GNormPlusFormatMultiplier extends JCasMultiplier_ImplBase {
    private final static Logger log = LoggerFactory.getLogger(GNormPlusFormatMultiplier.class);
    private Iterator<URI> currentUriBatch;
    private BioCCasPopulator casPopulator;

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
                casPopulator = new BioCCasPopulator(Path.of(nextUri));
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
