package de.julielab.jules.ae.genemapper.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import org.apache.commons.lang3.Range;

public class OffsetSet extends TreeSet<Range<Integer>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -90885720823317587L;

	public OffsetSet() {
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

	public OffsetSet(Collection<Range<Integer>> collection) {
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
		this.addAll(collection);
	}

	public Range<Integer> locate(Range<Integer> offsets) {
		return this.floor(offsets);
	}

}
