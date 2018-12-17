package de.julielab.jcore.consumer.es;

import org.apache.commons.lang.NotImplementedException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;

import de.julielab.jcore.consumer.es.preanalyzed.Document;
import de.julielab.jcore.consumer.es.preanalyzed.IFieldValue;

/**
 * A class that creates a {@link IFieldValue} instance corresponding to a
 * <tt>FeatureStructure</tt> (e.g. an annotation) or a <tt>JCas</tt> or parts
 * thereof. The returned value may be used as a field value in another document.
 * In case the returned value is a Document, this field will be mapped to the
 * <tt>object</tt> type by ElasticSearch or could be mapped to the
 * <tt>nested</tt> type, for example.
 * <p>
 * The difference to the {@link FieldGenerator} class is that the field
 * generator adds <em>fields</em> to a given {@link Document} instance whereas
 * this class creates <em>field values</em> which then have to be added to a
 * Document by custom code.
 * </p>
 * 
 * @author faessler
 * 
 */
public abstract class FieldValueGenerator extends AbstractFieldGenerator {

	public FieldValueGenerator(FilterRegistry filterRegistry) {
		super(filterRegistry);
	}

	public FieldValueGenerator() {
		this(null);
	}

	public IFieldValue generateFieldValue(FeatureStructure fs) throws FieldGenerationException {
		throw new NotImplementedException(
				"You have to override either generateDocument(FeatureStructure) or generateDocument(JCas).");
	}

	public IFieldValue generateFieldValue(JCas aCas) throws FieldGenerationException {
		throw new NotImplementedException(
				"You have to override either generateDocument(FeatureStructure) or generateDocument(JCas).");
	};

}
