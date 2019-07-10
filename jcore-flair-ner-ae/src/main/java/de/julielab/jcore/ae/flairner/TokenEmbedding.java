package de.julielab.jcore.ae.flairner;

/**
 * <p>
 * A simple wrapper class around a 1-based token index (relative to the sent sentence) and the embedding vector
 * for the respective token.
 * </p>
 */
public class TokenEmbedding {
    private String sentenceId;
    private int tokenId;
    private double[] vector;

    public TokenEmbedding(String sentenceId, int tokenId, double[] vector) {
        this.sentenceId = sentenceId;

        this.tokenId = tokenId;
        this.vector = vector;
    }

    public String getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(String sentenceId) {
        this.sentenceId = sentenceId;
    }

    /**
     * @return The 1-based token index of the sentence the token for this embedding belongs to.
     */
    public int getTokenId() {
        return tokenId;
    }

    public void setTokenId(int tokenId) {
        this.tokenId = tokenId;
    }

    public double[] getVector() {
        return vector;
    }

    public void setVector(double[] vector) {
        this.vector = vector;
    }
}