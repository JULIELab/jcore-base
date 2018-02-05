package de.julielab.jcore.consumer.es;

import java.util.List;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import de.julielab.jcore.consumer.es.preanalyzed.Document;

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
