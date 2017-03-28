	package de.julielab.jcore.reader.pmc.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.uima.jcas.tcas.Annotation;

public class ElementParsingResult extends ParsingResult {
	public static final ElementParsingResult NONE = new ElementParsingResult("none", 0, 0);
	private Annotation annotation;
	private String elementName;

	private List<ParsingResult> subResults = Collections.emptyList();
	private int lastTokenIndex;

	/**
	 * The UIMA annotation corresponding to the element represented by this
	 * ParsingResult. May be null in which case there will be no specific
	 * annotation for this element in the CAS.
	 * 
	 * @return The UIMA annotation for element that is represented by this
	 *         ParsingResult.
	 */
	public Annotation getAnnotation() {
		return annotation;
	}

	public void setAnnotation(Annotation annotation) {
		this.annotation = annotation;
	}

	public ElementParsingResult(String elementName, int begin, int end) {
		super(begin, end, ResultType.ELEMENT);
		this.elementName = elementName;
	}

	public void addSubResult(ParsingResult subResult) {
		if (subResults.isEmpty())
			subResults = new ArrayList<>();
		subResults.add(subResult);
	}

	public String getElementName() {
		return elementName;
	}

	/**
	 * The parsing results of child elements of the element this
	 * ElementParsingResult object stands for. The subresults include text
	 * results as well as results for child elements in document order.
	 * 
	 * @return The results for the contents of the element this result
	 *         represents.
	 */
	public List<ParsingResult> getSubResults() {
		return subResults;
	}

	public void setSubResults(List<ParsingResult> subResults) {
		this.subResults = subResults;
	}

	public String toString(int indentLevel) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < indentLevel*2; ++i)
			sb.append(" ");
		sb.append(elementName);
		sb.append(":\n");
		for (ParsingResult result : subResults)
			sb.append(result.toString(indentLevel+1));
		return sb.toString();
	}

	@Override
	public String toString() {
		return toString(0);
	}

	public void setLastTokenIndex(int lastTokenIndex) {
		this.lastTokenIndex = lastTokenIndex;
		
	}

	public int getLastTokenIndex() {
		return lastTokenIndex;
	}	
	
}
