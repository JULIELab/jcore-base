package de.julielab.jcore.consumer.es.preanalyzed;

public interface IToken {
	public enum TokenType {PREANALYZED, RAW}
	TokenType getTokenType();
	Object getTokenValue();
}
