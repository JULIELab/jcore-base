package de.julielab.jules.ae.genemapper;

import java.util.Properties;

public class GeneMapperConfiguration extends Properties {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1636778667307698253L;
	public static final String SYNONYM_INDEX = "mention_index";
	public static final String CONTEXT_INDEX = "semantic_index";

	// Parameters for LuceneDisambiguation
	public static final String EXACT_LONG_LENGTH = "exact_long_length";
	public static final String LONG_EXACT_SEM_SCORE = "long_exact_sem_score";
	public static final String SHORT_EXACT_SEM_SCORE = "short_exact_sem_score";
	public static final String FIRST_APPROX_MIN_MENTION_SCORE = "first_approx_min_mention_score";
	public static final String FIRST_APPROX_MIN_SEM_SCORE = "first_approx_min_sem_score";
	public static final String SECOND_APPROX_MIN_MENTION_SCORE = "second_approx_min_mention_score";
	public static final String SECOND_APPROX_MIN_SEM_SCORE = "second_approx_min_sem_score";
	// End Parameters for LuceneDisambiguation

}
