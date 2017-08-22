package de.julielab.jules.ae.genemapper.genemodel;

import java.util.HashSet;

import de.julielab.jules.ae.genemapper.SynHit;

public class GeneSet extends HashSet<GeneMention> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4038206150665551536L;
	private SynHit setId;

	public GeneSet(HashSet<GeneMention> genes, SynHit setId) {
		super(genes);
		this.setId = setId;
	}

	public GeneSet() {
		super();
	}

	/**
	 * The set ID represent the gene ID that all elements in the set belong to
	 * 
	 * @return The ID of the elements in this set.
	 */
	public SynHit getSetId() {
		return setId;
	}

	public void setSetId(SynHit setId) {
		this.setId = setId;
	}
}
