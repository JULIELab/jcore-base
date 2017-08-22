package de.julielab.jules.ae.genemapper.genemodel;

import de.julielab.jules.ae.genemapper.utils.norm.TermNormalizer;

public class GeneName {
	private String normalizedText;

	private String normalizedTextVariant;

	private TermNormalizer normalizer;

	private String text;
	
	public GeneName(String text, TermNormalizer normalizer) {
		this.text = text;
		this.normalizer = normalizer;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GeneName other = (GeneName) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	public String getNormalizedText() {

		if (null == normalizedText) {
			normalizedText = getNormalizer().normalize(text);
		}
		return normalizedText;
	}

	public String getNormalizedTextVariant() {
		if (null == normalizedTextVariant) {
			normalizedTextVariant = getNormalizer().normalize(getNormalizer().generateVariants(text));
		}
		return normalizedTextVariant;
	}

	public TermNormalizer getNormalizer() {
		return normalizer;
	}

	public String getText() {
		return text;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	public void setNormalizer(TermNormalizer normalizer) {
		this.normalizer = normalizer;
	}

	public void setText(String text) {
		this.text = text;
	}
}
