package de.julielab.jcore.utility.index;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

public interface JCoReAnnotationIndex<T extends Annotation> {
	void index(T a);
	void index(JCas jCas, int type);
	void index(JCas jcas, Type type);
	void add(T a);
}
