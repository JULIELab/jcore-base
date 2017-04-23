package de.julielab.jcore.utility;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * A trivial subclass of {@link JCoReAnnotationIndex} that uses a HashMap index
 * and {@link ArrayList} instances to store annotations in the index and to
 * return search hits.
 * 
 * @author faessler
 *
 * @param <T>
 *            The annotation type the index is over.
 * @param <K>
 *            The key type used to index the annotations.
 */
public class SimpleJCoReAnnotationIndex<T extends Annotation, K extends Comparable<K>>
		extends JCoReAnnotationIndex<T, K, ArrayList<T>, ArrayList<T>> {

	public SimpleJCoReAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator) {
		super(HashMap::new, indexTermGenerator, searchTermGenerator, ArrayList::new, ArrayList::new);
	}

}
