package de.julielab.jcore.consumer.es;

import de.julielab.jcore.consumer.es.preanalyzed.Document;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Creates a list of Documents from a single {@link JCas} instance.
 * 
 * @author faessler
 *
 */
public abstract class DocumentGenerator {

	protected FilterRegistry filterRegistry;

	public DocumentGenerator(FilterRegistry filterRegistry) {
		this.filterRegistry = filterRegistry;
	}

	public abstract List<Document> createDocuments(JCas aJCas) throws CASException, FieldGenerationException;

}
