package de.julielab.jules.ae.genemapper.genemodel;

import java.util.Set;

import de.julielab.jules.ae.genemapper.utils.OffsetMap;

public class SpeciesCandidates {

	private Set<String> titleCandidates;
	private Set<String> meshCandidates;
	private OffsetMap<SpeciesMention> textCandidates;
	
	public SpeciesCandidates(Set<String> titleCandidates, 
			Set<String> meshCandidates, OffsetMap<SpeciesMention> textCandidates) {
		this.titleCandidates = titleCandidates;
		this.meshCandidates = meshCandidates;
		this.textCandidates = textCandidates;
	}
	
	public Set<String> getTitleCandidates() {
		return titleCandidates;
	}
	
	public void setTitleCandidates(Set<String> titleCandidates) {
		this.titleCandidates = titleCandidates;
	}

	public Set<String> getMeshCandidates() {
		return meshCandidates;
	}
	
	public void setMeshCandidates(Set<String> meshCandidates) {
		this.meshCandidates = meshCandidates;
	}
	
	public OffsetMap<SpeciesMention> getTextCandidates() {
		return textCandidates;
	}
	
	public void setTextCandidates(OffsetMap<SpeciesMention> textCandidates) {
		this.textCandidates = textCandidates;
	}
}
