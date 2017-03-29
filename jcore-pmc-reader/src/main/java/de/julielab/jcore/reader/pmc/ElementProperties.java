package de.julielab.jcore.reader.pmc;

public class ElementProperties {
	/**
	 * Block elements are enclosed by line breaks in the CAS document text.
	 */
	public static final String BLOCK_ELEMENT = "block-element";
	/**
	 * Only text body elements may add to the CAS document text.
	 */
	public static final String TEXT_BODY_ELEMENT = "text-body-element";
	/**
	 * The UIMA annotation type that should be used to annotate the described element.
	 */
	public static final String TYPE = "type";
	
	public static final String TYPE_NONE = "none";
}
