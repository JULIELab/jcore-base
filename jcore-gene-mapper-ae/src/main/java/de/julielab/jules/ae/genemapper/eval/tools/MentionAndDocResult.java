package de.julielab.jules.ae.genemapper.eval.tools;

public class MentionAndDocResult {
	public MentionAndDocResult(DocResult mentionWise, DocResult docWise) {
		this.mentionWise = mentionWise;
		this.docWise = docWise;
	}
	public DocResult mentionWise;
	public DocResult docWise;
}
