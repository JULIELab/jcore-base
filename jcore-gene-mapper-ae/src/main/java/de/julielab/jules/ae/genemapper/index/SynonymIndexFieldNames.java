package de.julielab.jules.ae.genemapper.index;

public class SynonymIndexFieldNames {
	public static final String ID_FIELD = "entry_id";
	// For the old BC2 evaluation indexes
	public static final String ID_FIELD_LEGACY = "uniprot_id";
	// public static final String SYN_FIELD = "synonym";
	/**
	 * The index field name for the normalized gene / protein name.
	 */
	public static final String LOOKUP_SYN_FIELD = "indexed_syn";
	/**
	 * The index field name for the original, unnormalized (however,
	 * lower-cased) gene / protein name.
	 */
	public static final String ORIGINAL_NAME = "original_name";
	/**
	 * The index field name for the normalized variant of the original gene /
	 * protein name.
	 */
	public static final String VARIANT_NAME = "variant_name";
	/**
	 * The index field name for the normalized and then token-wise stemmed gene
	 * / protein name.
	 */
	public static final String STEMMED_NORMALIZED_NAME = "stemmed_normalized_name";
	public static final String TAX_ID_FIELD = "tax_id";
}
