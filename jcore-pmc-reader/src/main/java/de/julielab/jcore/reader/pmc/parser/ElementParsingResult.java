/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.reader.pmc.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.jcas.tcas.Annotation;

public class ElementParsingResult extends ParsingResult {

	private Annotation annotation;
	private String elementName;
	private boolean addAnnotationToIndexes;
	private boolean blockElement;
	private boolean textBodyElement;

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
		this.addAnnotationToIndexes = true;
		this.blockElement = false;
		this.textBodyElement = true;
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

	/**
	 * Iterates through all sub results and looks for element parsing results
	 * with an annotation. If an annotation encountered is an instance (i.e.
	 * also subclasses count) of <tt>annotationClass</tt>, it is added to the
	 * returned list.
	 * 
	 * @param annotationClass
	 *            The class of the returned sub result annotations.
	 * @return Sub result annotations that are instances of
	 *         <tt>annotationClass</tt>
	 */
	public <T extends Annotation> List<T> getSubResultAnnotations(Class<T> annotationClass) {
		return subResults.stream().filter(ElementParsingResult.class::isInstance).map(ElementParsingResult.class::cast)
				.filter(r -> r.getAnnotation() != null).filter(r -> annotationClass.isInstance(r.getAnnotation()))
				.map(r -> r.getAnnotation()).map(annotationClass::cast).collect(Collectors.toList());
	}

	public List<ParsingResult> getSubResults(final String elementName) {
		return subResults.stream().filter(ElementParsingResult.class::isInstance).map(ElementParsingResult.class::cast)
				.filter(r -> r.getElementName().equals(elementName)).collect(Collectors.toList());
	}

	public String toString(int indentLevel) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indentLevel * 2; ++i)
			sb.append(" ");
		sb.append(elementName);
		sb.append(":\n");
		for (ParsingResult result : subResults)
			sb.append(result.toString(indentLevel + 1));
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

	public void setAddAnnotationToIndexes(boolean addToIndexes) {
		this.addAnnotationToIndexes = addToIndexes;
	}

	/**
	 * Determines if the annotation of this result, as retrieved by
	 * {@link #getAnnotation()}, should be added to CAS indexes or not.
	 * 
	 * @return True if this result's annotation should be added to CAS indexes.
	 *         Default to true.
	 */
	public boolean addAnnotationToIndexes() {
		return addAnnotationToIndexes;
	}

	/**
	 * Block elements should be rendered with newlines before and after them.
	 * 
	 * @return True if this result corresponds to a block element, like a
	 *         paragraph, a caption etc.
	 */
	public boolean isBlockElement() {
		return blockElement;
	}

	public void setBlockElement(boolean blockElement) {
		this.blockElement = blockElement;
	}

	/**
	 * Indicates whether the XML element of this result has text contents to be
	 * added to the CAS document text.
	 * 
	 * @return True if this element might have text contents for the CAS
	 *         document text.
	 */
	public boolean isTextBodyElement() {
		return textBodyElement;
	}

	public void setTextBodyElement(boolean textBodyElement) {
		this.textBodyElement = textBodyElement;
	}

	@Override
	public String getResultText() {
		StringBuilder sb = new StringBuilder();
		for (ParsingResult result : subResults)
			sb.append(result.getResultText());
		return sb.toString();
	}

	/**
	 * Returns all text that is included directly in the element of this result,
	 * excluding text contained in child elements.
	 * 
	 * @return Directly contained text of this element result.
	 */
	public String getDirectResultText() {
		StringBuilder sb = new StringBuilder();
		for (ParsingResult result : subResults) {
			if (result.getResultType() == ResultType.TEXT)
				sb.append(result.getResultText());
		}
		return sb.toString();
	}

}
