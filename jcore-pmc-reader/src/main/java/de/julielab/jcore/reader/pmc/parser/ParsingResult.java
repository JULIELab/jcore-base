package de.julielab.jcore.reader.pmc.parser;

public abstract class ParsingResult {

	private int begin;
	private int end;
	private ResultType resultType;

	public enum ResultType {
		ELEMENT, TEXT, NONE
	}

	/**
	 * The begin offset of this parsing result in the original XML file. This is
	 * not the begin offset in the CAS.
	 * 
	 * @return The XML begin offset associated with this result.
	 */
	public int getBegin() {
		return begin;
	}

	/**
	 * The end offset of this parsing result in the original XML file. This is
	 * not the end offset in the CAS.
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

	public abstract String toString(int indentLevel);
}
