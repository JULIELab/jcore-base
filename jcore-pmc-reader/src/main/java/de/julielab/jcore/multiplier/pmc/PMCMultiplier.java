package de.julielab.jcore.multiplier.pmc;

import de.julielab.jcore.reader.pmc.CasPopulator;
import de.julielab.jcore.reader.pmc.parser.DocumentParsingException;
import de.julielab.jcore.reader.pmc.parser.ElementParsingException;
import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.analysis_component.JCasMultiplier_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

@ResourceMetaData(name = "JCoRe Pubmed Central NXML Multiplier", description = "This multiplier expect to receive URIs to NXML documents in the form of JCoReURI feature structures. All JCoReURI FS in the annotation indexes are read and output as new CASes.")
@OperationalProperties(outputsNewCases = true, multipleDeploymentAllowed = true, modifiesCas = false)
@TypeCapability(outputs = {"de.julielab.jcore.types.TitleType", "de.julielab.jcore.types.Title", "de.julielab.jcore.types.TextObject", "de.julielab.jcore.types.Table", "de.julielab.jcore.types.SectionTitle", "de.julielab.jcore.types.Section", "de.julielab.jcore.types.PubType", "de.julielab.jcore.types.Paragraph", "de.julielab.jcore.types.OtherPub", "de.julielab.jcore.types.pubmed.OtherID", "de.julielab.jcore.types.pubmed.ManualDescriptor", "de.julielab.jcore.types.Keyword", "de.julielab.jcore.types.Journal", "de.julielab.jcore.types.pubmed.Header", "de.julielab.jcore.types.Footnote", "de.julielab.jcore.types.Figure", "uima.tcas.DocumentAnnotation", "de.julielab.jcore.types.Date", "de.julielab.jcore.types.CaptionType", "de.julielab.jcore.types.Caption", "de.julielab.jcore.types.AutoDescriptor", "de.julielab.jcore.types.AuthorInfo", "de.julielab.jcore.types.AbstractText", "de.julielab.jcore.types.AbstractSectionHeading", "de.julielab.jcore.types.AbstractSection"})
public class PMCMultiplier extends JCasMultiplier_ImplBase {
    private final static Logger log = LoggerFactory.getLogger(PMCMultiplier.class);
    private Iterator<URI> currentUriBatch;
    private CasPopulator casPopulator;


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Collection<JCoReURI> jcoreUris = JCasUtil.select(aJCas, JCoReURI.class);
        if (log.isDebugEnabled())
            log.debug("Received batch of {} NXML URIs", jcoreUris.size());
        currentUriBatch = jcoreUris.stream().map(JCoReURI::getUri).map(URI::create).iterator();
        try {
            casPopulator = new CasPopulator(currentUriBatch);
        } catch (IOException e) {
            log.error("Exception occurred when trying to inizialize the NXML parser", e);
            throw new AnalysisEngineProcessException(e);
        }
    }


    @Override
    public boolean hasNext() {
        return currentUriBatch.hasNext();
    }

    @Override
    public AbstractCas next() throws AnalysisEngineProcessException {
        if (hasNext()) {
            JCas cas = getEmptyJCas();
            URI next = null;
            try {
                next = currentUriBatch.next();
                log.trace("Populating cas with data from {}", next);
                casPopulator.populateCas(next, cas);
                return cas;
            } catch (ElementParsingException e) {
                log.error("Exception occurred why trying to parse {}", next, e);
            }
        }
        return null;
    }
}
