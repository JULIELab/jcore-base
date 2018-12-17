package de.julielab.jcore.consumer.es.preanalyzed;

import com.google.gson.annotations.SerializedName;

public class PreanalyzedToken implements IToken {
	/**
	 * The actual term as it will be put verbatim into the index. The value is a UTF-8 string that represents the
	 * current token.
	 * <p>
	 * Preanalyzed format key: <tt>t</tt>
	 * </p>
	 */
	@SerializedName("t")
	public String term;
	/**
	 * (optional) The value is the start offset of the token, a non-negative integer.
	 * <p>
	 * Preanalyzed format key: <tt>s</tt>
	 * </p>
	 */
	@SerializedName("s")
	public int start;
	/**
	 * (optional) The value is the end offset of the token, a both non-negative integer.
	 * <p>
	 * Preanalyzed format key: <tt>e</tt>
	 * </p>
	 */
	@SerializedName("e")
	public int end;
	/**
	 * (optional - if missing a value of 1 is assumed) The value is non-negative integer that represent the position
	 * increment attribute.
	 * <p>
	 * Preanalyzed format key: <tt>i</tt>
	 * </p>
	 */
	@SerializedName("i")
	public int positionIncrement = 1;
	/**
	 * (optional) The value is a string, which is the token type name.
	 * <p>
	 * Preanalyzed format key: <tt>y</tt>
	 * </p>
	 */
	@SerializedName("y")
	public String type;
	/**
	 * Base64-encoded payload
	 * <p>
	 * Preanalyzed format key: <tt>p</tt>
	 * </p>
	 */
	@SerializedName("p")
	public String payload;
	/**
	 * (optional) The value is a string representing integer value in hexadecimal format.
	 * <p>
	 * Preanalyzed format key: <tt>f</tt>
	 * </p>
	 */
	@SerializedName("f")
	public String flags;

	@Override
	public TokenType getTokenType() {
		return TokenType.PREANALYZED;
	}

	@Override
	public Object getTokenValue() {
		return term;
	}
}
