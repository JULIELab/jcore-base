package de.julielab.jcore.consumer.entityevaluator;

import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

public class OffsetsColumn extends Column {

	@Override
	public String getValue(TOP a) {
		Annotation an = (Annotation) a;
		return an.getBegin() + "\t" + an.getEnd();
	}
	
}
