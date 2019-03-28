package de.julielab.jcore.consumer.es;

import de.julielab.jcore.consumer.es.preanalyzed.Document;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

/**
 * Extend this class to create custom fields generators. Field generators are
 * meant to be used on an external Document instances to add fields on them.
 * 
 * @author faessler
 *
 */
public abstract class FieldGenerator extends AbstractFieldGenerator {

	public FieldGenerator() {
		super();
	}

	public FieldGenerator(FilterRegistry filterRegistry) {
		super(filterRegistry);
	}

	public abstract Document addFields(JCas aJCas, Document doc) throws CASException, FieldGenerationException;

}
