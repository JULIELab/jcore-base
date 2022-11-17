package de.julielab.jcore.ae.flairner;

import java.util.List;

/**
 * <p>A class to assemble the response from FLAIR for a tagging request. The found entities are returned as
 * well as - depending on the annotator configuration - token embeddings created for the FLAIR sequence tagging.</p>
 */
public class NerTaggingResponse {
    private List<TaggedEntity> taggedEntities;
    private List<TokenEmbedding> tokenEmbeddings;

    public NerTaggingResponse(List<TaggedEntity> taggedEntities, List<TokenEmbedding> tokenEmbeddings) {

        this.taggedEntities = taggedEntities;
        this.tokenEmbeddings = tokenEmbeddings;
    }

    public List<TaggedEntity> getTaggedEntities() {
        return taggedEntities;
    }

    public void setTaggedEntities(List<TaggedEntity> taggedEntities) {
        this.taggedEntities = taggedEntities;
    }

    public List<TokenEmbedding> getTokenEmbeddings() {
        return tokenEmbeddings;
    }

    public void setTokenEmbeddings(List<TokenEmbedding> tokenEmbeddings) {
        this.tokenEmbeddings = tokenEmbeddings;
    }

    @Override
    public String toString() {
        return "NerTaggingResponse{" +
                "taggedEntities=" + taggedEntities +
                ", tokenEmbeddings=" + tokenEmbeddings +
                '}';
    }
}
