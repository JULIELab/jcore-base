package de.julielab.jcore.ae.checkpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>This is class is a synchronization point for JeDIS components to report documents as being completely finished
 * with processing.</p>
 * <p>Problem explanation: This synchronization is necessary because most database operating components work in batch mode for
 * performance reasons. However, if multiple components use batching with might be out of sync due to different
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
    private static DocumentReleaseCheckpoint checkpoint;
    private Map<Object, BlockingDeque<DocumentId>> releasedDocuments;

    private DocumentReleaseCheckpoint() {
        releasedDocuments = new ConcurrentHashMap<>();
    }

    public static DocumentReleaseCheckpoint get() {
        if (checkpoint == null)
            checkpoint = new DocumentReleaseCheckpoint();
        return checkpoint;
    }

    /**
     * <p>Registers a component that will add {@link DocumentId}s via the {@link #release(Object, Stream)} method.</p>
     * <p>The given object is commonly just the <tt>this</tt> reference of the UIMA component that needs
     * synchronization.</p>
     *
     * @param componentKey A canonical identifier of the component taking part in synchronization.
     */
    public void register(Object componentKey) {
        releasedDocuments.put(componentKey, new LinkedBlockingDeque<>());
    }

    /**
     * <p>Removes a component from the list of document ID releasing components.</p>
     * <p>The given object is commonly just the <tt>this</tt> reference of the UIMA component that needs
     * synchronization.</p>
     * <p>This method is not commonly required and only here for functional completeness.</p>
     *
     * @param componentKey The canonical identifier provided in {@link #register(Object)} earlier.
     */
    public void unregister(Object componentKey) {
        releasedDocuments.remove(componentKey);
    }

    /**
     * <p>To be called from synchronizing components. They send their registration key - commonly just the annotator object itself - and the document IDs they are positively finished with.</p>
     *
     * @param componentKey        The canonical identifier provided in {@link #register(Object)} earlier.
     * @param releasedDocumentIds The document IDs to be released.
     */
    public void release(Object componentKey, Stream<DocumentId> releasedDocumentIds) {
        releasedDocumentIds.forEach(d -> releasedDocuments.get(componentKey).add(d));
    }

    /**
     * <p>Used by the {@link DBCheckpointAE} to determine documents that can safely be marked as being finished with processing.</p>
     * <p>Get all the document IDs from all synchronizing components that those components have released. The returned list will
     * contain duplicates of document IDs when multiple components have released that document. The {@link DBCheckpointAE}
     * will only mark those documents as processed that have been released as often as synchronizing components have been
     * registered with {@link #register(Object)}.</p>
     *
     * @return The currently released document IDs.
     */
    public List<DocumentId> getReleasedDocumentIds() {
        List<DocumentId> releasedIds = new ArrayList<>(releasedDocuments.values().stream().mapToInt(Collection::size).sum());
        releasedDocuments.values().forEach(queue -> queue.drainTo(releasedIds));
        return releasedIds;
    }

    /**
     * <p>Returns the number of currently registered components.</p>
     *
     * @return The number of currently registered components.
     */
    public int getNumberOfRegisteredComponents() {
        return releasedDocuments.size();
    }

    /**
     * <p>For debugging purposes: Returns a map from registered keys to the number of document IDs currently released
     * for each key.</p>
     *
     * @return A map showing the number of released documents by key.
     */
    public Map<Object, Integer> getReleasedDocumentsState() {
        return releasedDocuments.keySet().stream().collect(Collectors.toMap(Function.identity(), o -> releasedDocuments.get(o).size()));
    }
}
