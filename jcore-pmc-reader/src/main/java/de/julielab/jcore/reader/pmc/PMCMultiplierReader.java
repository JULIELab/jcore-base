package de.julielab.jcore.reader.pmc;

import de.julielab.jcore.types.casmultiplier.JCoReURI;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.ducc.Workitem;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

@ResourceMetaData(name="JCoRe Pubmed Central Multiplier Reader", description = "Reads a directory of NXML files, possibly assembled into ZIP archives. Requires the Pubmed Central Multiplier to follow in the pipeline. This reader only sends URIs referencing the NXML files to the multiplier that then does the parsing.")
@TypeCapability(outputs = {"de.julielab.jcore.types.casmultiplier.JCoReURI", "org.apache.uima.ducc.Workitem" })
public class PMCMultiplierReader extends PMCReaderBase {
    public static final String PARAM_INPUT = PMCReaderBase.PARAM_INPUT;
    public static final String PARAM_RECURSIVELY = PMCReaderBase.PARAM_RECURSIVELY;
    public static final String PARAM_SEARCH_ZIP = PMCReaderBase.PARAM_SEARCH_ZIP;
    public static final String PARAM_WHITELIST = PMCReaderBase.PARAM_WHITELIST;
    public static final String PARAM_SEND_CAS_TO_LAST = "SendCasToLast";
    public static final String PARAM_BATCH_SIZE = "BatchSize";
    private final static Logger log = LoggerFactory.getLogger(PMCMultiplierReader.class);
    @ConfigurationParameter(name = PARAM_SEND_CAS_TO_LAST, mandatory = false, defaultValue = "false", description = "UIMA DUCC relevant parameter when using a CAS multiplier. When set to true, the worker CAS from the collection reader is forwarded to the last component in the pipeline. This can be used to send information about the progress to the CAS consumer in order to have it perform batch operations. For this purpose, a feature structure of type WorkItem from the DUCC library is added to the worker CAS. This feature structure has information about the current progress.")
    private boolean sendCasToLast;
    @ConfigurationParameter(name = PARAM_BATCH_SIZE, mandatory = false, defaultValue = "20", description = "The number of NXML URI references to send to the CAS multipliers in each work assignment. Defaults to 20.")
    private int batchSize;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        sendCasToLast = (boolean) Optional.ofNullable(getConfigParameterValue(PARAM_SEND_CAS_TO_LAST)).orElse(false);
        batchSize = (int) Optional.ofNullable(getConfigParameterValue(PARAM_BATCH_SIZE)).orElse(20);
        if (batchSize <= 0)
            throw new ResourceInitializationException(new IllegalArgumentException("The given batch size is " + batchSize + ", but it is required to be a positive number."));
    }

    @Override
    public void getNext(JCas jCas) throws CollectionException {
        for (int i = 0; i < batchSize && pmcFiles.hasNext(); i++) {
            URI uri = pmcFiles.next();
            try {
                JCoReURI fileType = new JCoReURI(jCas);
                fileType.setUri(uri.toString());
                fileType.addToIndexes();
            } catch (Exception e) {
                log.error("Exception with URI: " + uri.toString(), e);
                throw new CollectionException(e);
            }

            completed++;
        }
        if (sendCasToLast) {
            Workitem workitem = new Workitem(jCas);
            // Send the work item CAS also to the consumer. Normally, only the CASes emitted by the CAS multiplier
            // will be routed to the consumer. We do this to let the consumer know that the work item has been
            // finished.
            workitem.setSendToLast(true);
            workitem.setBlockindex(completed / batchSize);
            if (!hasNext())
                workitem.setLastBlock(true);
            workitem.addToIndexes();
        }
    }
}
