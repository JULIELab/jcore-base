package de.julielab.jules.ae.genemapper.disambig;

import java.util.HashSet;
import java.util.Set;

import de.julielab.jules.ae.genemapper.disambig.DypsisDisambiguation.NameLevelUnificationStrategy;
import de.julielab.jules.ae.genemapper.genemodel.GeneDocument;

public class DypsisDocumentDisambiguationData implements DocumentDisambiguationData {
	private GeneDocument geneDocument;
	private Set<NameLevelUnificationStrategy> nameLevelUnificationStrategies;

	public void addNameLevelUnificationStrategy(NameLevelUnificationStrategy... strategies) {
		if (null == nameLevelUnificationStrategies)
			nameLevelUnificationStrategies = new HashSet<>();

		for (int i = 0; i < strategies.length; i++) {
			nameLevelUnificationStrategies.add(strategies[i]);
		}
	}

	public GeneDocument getGeneDocument() {
		return geneDocument;
	}

	public void setGeneDocument(GeneDocument geneDocument) {
		this.geneDocument = geneDocument;
	}

	public Set<NameLevelUnificationStrategy> getNameLevelUnificationStrategies() {
		return nameLevelUnificationStrategies;
	}

	public void setNameLevelUnificationStrategies(Set<NameLevelUnificationStrategy> nameLevelUnificationStrategies) {
		this.nameLevelUnificationStrategies = nameLevelUnificationStrategies;
	}

}
