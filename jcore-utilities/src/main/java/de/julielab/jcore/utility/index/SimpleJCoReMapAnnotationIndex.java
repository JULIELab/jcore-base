package de.julielab.jcore.utility.index;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * A trivial subclass of {@link JCoReMapAnnotationIndex} that uses a HashMap index
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
public class SimpleJCoReMapAnnotationIndex<T extends Annotation, K extends Comparable<K>>
		extends JCoReMapAnnotationIndex<T, K, ArrayList<T>, ArrayList<T>> {

	public SimpleJCoReMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator) {
		super(HashMap::new, indexTermGenerator, searchTermGenerator, ArrayList::new, ArrayList::new);
	}
	
	public SimpleJCoReMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator, JCas jCas, Type type) {
		super(HashMap::new, indexTermGenerator, searchTermGenerator, ArrayList::new, ArrayList::new, jCas, type);
	}
	
	public SimpleJCoReMapAnnotationIndex(IndexTermGenerator<K> indexTermGenerator,
			IndexTermGenerator<K> searchTermGenerator, JCas jCas, int type) {
		super(HashMap::new, indexTermGenerator, searchTermGenerator, ArrayList::new, ArrayList::new, jCas, type);
	}

}
