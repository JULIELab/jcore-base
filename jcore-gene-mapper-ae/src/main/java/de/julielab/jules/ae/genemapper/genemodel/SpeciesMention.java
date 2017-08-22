package de.julielab.jules.ae.genemapper.genemodel;

public class SpeciesMention {

	private String taxId;
	private String text;
	
	public SpeciesMention(String taxId, String text) {
		this.taxId = taxId;
		this.text = text;
	}

	public String getTaxId() {
		return taxId;
	}
	
	public String getText() {
		return text;
	}

}
