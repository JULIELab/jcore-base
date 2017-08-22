package de.julielab.jules.ae.genemapper.genemodel;

import org.apache.commons.lang3.Range;

public class Acronym {
	private String acronym;
	private Range<Integer> offsets;
	private AcronymLongform longform;

	public String getAcronym() {
		return acronym;
	}

	public void setAcronym(String acronym) {
		this.acronym = acronym;
	}

	public Range<Integer> getOffsets() {
		return offsets;
	}

	public void setOffsets(Range<Integer> offsets) {
		this.offsets = offsets;
	}

	public AcronymLongform getLongform() {
		return longform;
	}

	public void setLongform(AcronymLongform longform) {
		this.longform = longform;
	}
}
