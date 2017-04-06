package de.julielab.jcore.reader.pmc.parser;

public abstract class ParsingResult {

	private int begin;
	private int end;
	private ResultType resultType;

	public enum ResultType {
		ELEMENT, TEXT, NONE
	}

	/**
	 * The begin offset of in bytes this parsing result in the original XML
	 * file. This is not the begin offset in the CAS.
	 * 
	 * @return The XML begin offset associated with this result.
	 */
	public int getBegin() {
		return begin;
	}

	/**
	 * The end offset in bytes of this parsing result in the original XML file.
	 * This is not the end offset in the CAS.
	 * 
	 * @return The XML end offset associated with this result.
	 */
	public int getEnd() {
		return end;
	}

	public ParsingResult(int begin, int end, ResultType resultType) {
		this.begin = begin;
		this.end = end;
		this.resultType = resultType;
	}

	public ResultType getResultType() {
		return resultType;
	}

	public void setResultType(ResultType resultType) {
		this.resultType = resultType;
	}

	/**
	 * Returns a pretty-printed representation of this result and all its sub
	 * results, recursively.
	 * 
	 * @param indentLevel
	 *            The indentation level to start with.
	 * @return A textual representation of this parsing result.
	 */
	public abstract String toString(int indentLevel);

	/**
	 * Returns the text of text nodes of this parsing element and its sub
	 * elements as a single string.
	 * 
	 * @return The element text.
	 */
	public abstract String getResultText();
}
