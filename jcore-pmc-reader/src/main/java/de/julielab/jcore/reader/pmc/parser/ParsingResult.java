package de.julielab.jcore.reader.pmc.parser;

public abstract class ParsingResult {
	
	private int begin;
	private int end;
	private ResultType resultType;
	
	public enum ResultType { ELEMENT, TEXT }
	
	public int getBegin() {
		return begin;
	}

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
	
	public abstract String toString(int indentLevel);
}
