package de.julielab.jcore.ae.checkpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>This is class is a synchronization point for JeDIS components to report documents as being completely finished
 * with processing.</p>
 * <p>Problem explanation: This synchronization is necessary because most database operating components work in batch mode for
 * performance reasons. However, if multiple components use batching wich might be out of sync due to different
 * batch sizes and possibly other factors, one component may have sent a batch of document data to the database
 * while other components have not at a particular point in time. If at such a time point the pipeline crashes
 * or is manually interrupted, the actually written data is incoherent in the sense that some components have sent
 * their data for a particular document and others have not.</p>
 * <p>This class does not completely resolve this issue, i.e. asynchronously sending of batches is still an issue
 * when using this class. However, this class is used by the {@link DBCheckpointAE} to determine if a set
 * of registered components have all released a given {@link DocumentId} before marking it as successfully
 * <em>processed</em> in the JeDIS database subset table. In this way, an uncoherent state can be seen in the database
 * by items that are <em>in process</em> but have not been <em>processed</em> after the pipeline finishes.</p>
 * <p>Those documents can then easily be reprocessed by removing the <em>in process</em> mark with <em>CoStoSys</em>.</p>
 * <p>Note that this requires that the <tt>DBCheckpointAE</tt> marking documents as <em>processed</em>
 * is the last component in the pipeline</p>
 */
public class DocumentReleaseCheckpoint {
    public static final String SYNC_PARAM_DESC = "If set, the value of this parameter is used to synchronize the 'processed' mark in the subset table documents processed by the pipeline. " +
            "This is useful when document data is sent batchwise to the database by multiple components: In the case of a crash or manual cancellation of a pipeline run without synchronization is might happen " +
            "that some components have sent their data and others haven't at the time of termination. To avoid an inconsistent database state," +
            "a document will only be marked as finished " +
            "processed in the JeDIS subset table if all synchronized components in the pipeline have released the document. " +
            "This is done by the DBCheckpointAE which must be at the end of the pipeline and have the 'IndicateFinished' parameter set to 'true'. " +
            "Synchronized components are those that disclose this parameter and have a value set to it.";
    public static final String PARAM_JEDIS_SYNCHRONIZATION_KEY = "JedisSynchronizationKey";
    private final static Logger log = LoggerFactory.getLogger(DocumentReleaseCheckpoint.class);
    private static DocumentReleaseCheckpoint checkpoint;
    private Map<DocumentId, Set<String>> releasedDocuments;
    private Set<String> registeredComponents;
    private long lastwarning = 1000;

    private DocumentReleaseCheckpoint() {
        releasedDocuments = new HashMap<>();
        registeredComponents = new HashSet<>();
    }

    public static DocumentReleaseCheckpoint get() {
        if (checkpoint == null)
            checkpoint = new DocumentReleaseCheckpoint();
        return checkpoint;
    }

    /**
     * <p>Registers a component that will add {@link DocumentId}s via the {@link #release(String, Stream)} method.</p>
     *
     * @param componentKey A canonical identifier of the component taking part in synchronization.
     */
    public void register(String componentKey) {
        registeredComponents.add(componentKey);
    }

    /**
     * <p>Removes a component from the list of document ID releasing components.</p>
     * <p>This method is not commonly required and only here for functional completeness.</p>
     *
     * @param componentKey The canonical identifier provided in {@link #register(String)} earlier.
     */
    public void unregister(String componentKey) {
        registeredComponents.remove(componentKey);
    }

    /**
     * <p>To be called from synchronizing components. They send their registration key and the document IDs they are positively finished with.</p>
     *
     * @param componentKey        The canonical identifier provided in {@link #register(String)} earlier.
     * @param releasedDocumentIds The document IDs to be released.
     */
    public void release(String componentKey, Stream<DocumentId> releasedDocumentIds) {
        if (!registeredComponents.contains(componentKey))
            throw new IllegalArgumentException("No component is registered for key " + componentKey);
        synchronized (releasedDocuments) {
            releasedDocumentIds.forEach(d -> releasedDocuments.compute(d, (k, v) -> {
                if (v == null) {
                    Set<String> ret = new HashSet<>();
                    ret.add(componentKey);
                    return ret;
                }
                v.add(componentKey);
                return v;
            }));
        }
    }

    /**
     * <p>Used by the {@link DBCheckpointAE} to determine documents that can safely be marked as being finished with processing.</p>
     * <p>Gets all the document IDs from all synchronizing components that those components have released. The returned list will
     * contain duplicates of document IDs when multiple components have released that document. The {@link DBCheckpointAE}
     * will only mark those documents as processed that have been released as often as synchronizing components have been
     * registered with {@link #register(String)}.</p>
     *
     * @return The currently released document IDs.
     */
    public Set<DocumentId> getReleasedDocumentIds() {
        // Get all documents released by all components
        Set<DocumentId> returnedIds;
        synchronized (releasedDocuments) {
            log.trace("The following {} components are registered for document release: {}", getNumberOfRegisteredComponents(), registeredComponents);
            log.trace("Released document counts: {}", this.releasedDocuments);
            returnedIds = this.releasedDocuments.keySet().stream().filter(k -> this.releasedDocuments.get(k).containsAll(this.registeredComponents)).collect(Collectors.toSet());
            // Remove the completely released documents from the pool of potentially not yet completely released documents.
            returnedIds.forEach(id -> this.releasedDocuments.remove(id));
        }
        log.debug("Returning {} documents released by all registered components. {} document IDs remain that have not yet been released by all registered components.", returnedIds.size(), this.releasedDocuments.size());
        if (this.releasedDocuments.size() > lastwarning) {
            log.warn("The number of document IDs that have not been released by all registered components has grown to {}. If it does not decrease again, there is likely an errorneous component which does not release its documents. Currently registered components: {}", releasedDocuments.size(), registeredComponents);
            lastwarning *= 2;
        } else if (this.releasedDocuments.size() < 50) {
            lastwarning = 1000;
        }
        return returnedIds;
    }

    /**
     * <p>Returns the number of currently registered components.</p>
     *
     * @return The number of currently registered components.
     */
    public int getNumberOfRegisteredComponents() {
        return registeredComponents.size();
    }

}
