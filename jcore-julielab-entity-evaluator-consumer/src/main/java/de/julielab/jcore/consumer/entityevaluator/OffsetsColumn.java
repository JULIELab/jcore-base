package de.julielab.jcore.consumer.entityevaluator;

import java.util.TreeMap;

import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

import de.julielab.jcore.consumer.entityevaluator.EntityEvaluatorConsumer.OffsetMode;

public class OffsetsColumn extends Column {

	private TreeMap<Integer, Integer> numWsMap;
	private OffsetMode offsetMode;

	public OffsetsColumn(TreeMap<Integer, Integer> numWsMap, OffsetMode offsetMode) {
		this.numWsMap = numWsMap;
		this.offsetMode = offsetMode;
	}

	@Override
	public String getValue(TOP a) {
		Annotation an = (Annotation) a;
		int beginOffset = an.getBegin();
		int endOffset = an.getEnd();
		switch (offsetMode) {
		case CHARACTER_SPAN:
			beginOffset = an.getBegin();
			endOffset = an.getEnd();
			break;
		case NON_WS_CHARACTERS:
			// for both offsets, subtract the number of preceding
			// white
			// spaces up to the respective offset
			beginOffset = an.getBegin() - numWsMap.floorEntry(an.getBegin()).getValue();
			// we even have to subtract one more because we count
			// actual
			// characters while UIMA counts spans
			endOffset = an.getEnd() - numWsMap.floorEntry(an.getEnd()).getValue() - 1;
			break;
		default:
			throw new IllegalArgumentException("Offset mode \"" + offsetMode + "\" is currently unsupported.");
		}
		String begin = String.valueOf(beginOffset);
		String end = String.valueOf(endOffset);
		return begin + "\t" + end;
	}

}
