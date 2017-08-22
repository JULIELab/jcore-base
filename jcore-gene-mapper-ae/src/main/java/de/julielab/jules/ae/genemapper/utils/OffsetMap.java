package de.julielab.jules.ae.genemapper.utils;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.lang3.Range;

public class OffsetMap<V> extends TreeMap<Range<Integer>, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8336911838492274123L;

	public OffsetMap() {
		super(new Comparator<Range<Integer>>() {
	
			@Override
			public int compare(Range<Integer> gm1, Range<Integer> gm2) {
				if (gm1.getMinimum() == gm2.getMinimum()) {
					if (gm1.getMaximum() == gm2.getMaximum()) {
						return 0;
					} else if (gm1.getMaximum() < gm2.getMaximum()) {
						return -1;
					} else {
						return 1;
					}
				} else if (gm1.getMinimum() < gm2.getMinimum()) {
					return -1;
				} else {
					return 1;
				}
			}
			
		});
	}

	public NavigableMap<Range<Integer>, V> restrictTo(Range<Integer> range) {
		Range<Integer> begin = Range.between(range.getMinimum(), range.getMinimum());
		Range<Integer> end = Range.between(range.getMaximum(), range.getMaximum());
		return this.subMap(begin, true, end, true);
	}
}
