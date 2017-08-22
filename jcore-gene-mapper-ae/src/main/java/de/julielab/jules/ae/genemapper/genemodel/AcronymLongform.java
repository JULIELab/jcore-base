package de.julielab.jules.ae.genemapper.genemodel;

import org.apache.commons.lang3.Range;

public class AcronymLongform {
	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Range<Integer> getOffsets() {
		return offsets;
	}

	public void setOffsets(Range<Integer> offsets) {
		this.offsets = offsets;
	}

	private Range<Integer> offsets;
}
