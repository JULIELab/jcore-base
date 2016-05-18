package de.julielab.jcore.reader.dta.util;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public class DTAUtils {
	public static boolean hasAnyClassification(JCas jcas, Class<?>... classes){
		FSIterator<Annotation> it = jcas.getAnnotationIndex(DocumentClassification.type).iterator();
		while(it.hasNext()){
			DocumentClassification classification = (DocumentClassification) it.next();
			for(Class<?> c : classes)			
				if(c.isInstance(classification))
					return true;
		}
		return false;
	}
}
