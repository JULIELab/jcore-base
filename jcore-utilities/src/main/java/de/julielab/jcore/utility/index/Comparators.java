package de.julielab.jcore.utility.index;

import java.util.Comparator;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * This class offers some commonly used comparators on annotations.
 * 
 * @author faessler
 *
 */
public class Comparators {
	/**
	 * Compares annotations strictly by their offsets. Two annotations are
	 * equal if both begin and end offsets are equal. If not, they are
	 * sorted by begin offset or, when those are equal, be end offset.
	 * 
	 * @return The comparison value.
	 */
	public static <T extends Annotation> Comparator<T> exactOffsetMatchComparator() {
		return (o1, o2) -> {

			if (o1.getBegin() == o2.getBegin() && o1.getEnd() == o2.getEnd())
				return 0;
			if (o1.getBegin() == o2.getBegin())
				return o1.getEnd() - o2.getEnd();
			return o1.getBegin() - o2.getBegin();
		};
	}

	/**
	 * Compares annotations for any kind of overlapping. As long as two
	 * annotations overlap for at least a single position, they are deemed
	 * "equal" by this comparator. Otherwise, they are sorted by begin
	 * offset.
	 * 
	 * @return 0 if annotations overlap, the start offset difference
	 *         otherwise.
	 */
	public static <T extends Annotation> Comparator<T> overlapComparator() {
		return (o1, o2) -> {
			int b1 = o1.getBegin();
			int e1 = o1.getEnd();
			int b2 = o2.getBegin();
			int e2 = o2.getEnd();

			if ((b1 <= b2) && (e1 >= e2)) {
				return 0;
			} else if ((b1 >= b2) && (e1 <= e2)) {
				return 0;
			} else if ((b1 < e2) && (e1 > e2)) {
				return 0;
			} else if ((b1 < b2) && (e1 > b2)) {
				return 0;
			}
			return b1 - b2;
		};
	}
	
	public static <T extends Annotation> Comparator<T> beginOffsetComparator() {
		return (o1, o2) -> {
			return o1.getBegin() - o2.getBegin();
		};
	}
	
	public static <T extends Annotation> Comparator<T> endOffsetComparator() {
		return (o1, o2) -> {
			return o1.getEnd() - o2.getEnd();
		};
	}
	
	public static Comparator<Long> longOverlapComparator() {
		return (o1, o2) -> {
			int b1 = (int)(o1 >> 32);
			int e1 = (int)(o1 >> 0);
			int b2 = (int)(o2 >> 32);
			int e2 = (int)(o2 >> 0);

			if ((b1 <= b2) && (e1 >= e2)) {
				return 0;
			} else if ((b1 >= b2) && (e1 <= e2)) {
				return 0;
			} else if ((b1 < e2) && (e1 > e2)) {
				return 0;
			} else if ((b1 < b2) && (e1 > b2)) {
				return 0;
			}
			return b1 - b2;
		};
	}
}