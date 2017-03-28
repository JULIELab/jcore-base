package de.julielab.jcore.reader.pmc.parser;

public class TextParsingResult extends ParsingResult {
	private String text;
	
	public TextParsingResult(String text, int begin, int end) {
		super(begin, end, ResultType.TEXT);
		this.text = text;
 	}

	public String getText() {
		return text;
	}

	@Override
	public String toString(int indentLevel) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indentLevel*2; ++i)
			sb.append(" ");
		sb.append(text);
		sb.append("\n");
		return sb.toString();
	}
}
