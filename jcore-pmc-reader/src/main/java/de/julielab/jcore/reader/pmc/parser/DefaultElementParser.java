package de.julielab.jcore.reader.pmc.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import com.ximpleware.NavException;
import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.pmc.ElementProperties;
import de.julielab.jcore.reader.pmc.PMCReader;
import de.julielab.jcore.reader.pmc.parser.ParsingResult.ResultType;

/**
 * A generic element parser that is applicable to any element of the document
 * body. Parses the text contents from the element and calls specialized parsers
 * for child elements.
 * 
 * @author faessler
 *
 */
public class DefaultElementParser extends NxmlElementParser {

	public DefaultElementParser(NxmlDocumentParser nxmlDocumentParser) {
		super(nxmlDocumentParser);
	}

	@Override
	protected void beforeParseElement() throws ElementParsingException {
		// since this parser does not know the element is is used upon, set
		// it first for the parsing result creation
		try {
			elementName = vn.toString(vn.getCurrentIndex());
		} catch (NavException e) {
			throw new ElementParsingException(e);
		}
	}

	@Override
	public void parseElement(ElementParsingResult result) throws ElementParsingException {
		try {
			// checkCursorPosition();
			int elementDepth = vn.getCurrentDepth();
			boolean omitElement = determineOmitElement();
			if (omitElement) {
				int firstIndexAfterElement = skipElement();
				result.setLastTokenIndex(firstIndexAfterElement);
				result.setResultType(ResultType.NONE);
				return;
			}
			result.setAnnotation(getParsingResultAnnotation());
			editResult(result);

			// idea: get the text contents between child elements. For the child
			// elements we let the appropriate parser do the work
			// we determine whether we are still in the original element by
			// checking if the current depth gets higher (i.e. small depth) than
			// the element is
			// go one token further to leave the starting tag token
			int i = vn.getCurrentIndex() + 1;
			boolean inElementContent = false;
			for (; i < vn.getTokenCount() && tokenIndexBelongsToElement(i, elementDepth); ++i) {
				int tokenType = vn.getTokenType(i);
				// look for the first token that does not belong to the starting
				// tag
				if (tokenType != VTDNav.TOKEN_ATTR_NAME && tokenType != VTDNav.TOKEN_ATTR_VAL
						&& tokenType != VTDNav.TOKEN_ATTR_NS && tokenType != VTDNav.TOKEN_PI_NAME
						&& tokenType != VTDNav.TOKEN_PI_VAL)
					inElementContent = true;
				// as long as we are not yet within the element contents, just
				// keep on looking
				if (!inElementContent)
					continue;
				switch (tokenType) {
				case VTDNav.TOKEN_STARTING_TAG:
					// set the cursor to the position of the starting tag and
					// then call the parser for this tag
					if (vn.getCurrentIndex() != i)
						vn.recoverNode(i);
					String tagName = vn.toString(vn.getCurrentIndex());
					ElementParsingResult subResult = nxmlDocumentParser.getParser(tagName).parse();
					result.addSubResult(subResult);
					// The contract is: each element parser returns the index of
					// the first VTD token after its processed element. Since we
					// are here in a for-loop that will increment the index, we
					// subtract one so that in the next iteration we actually
					// process the first after-last-element-token
					i = subResult.getLastTokenIndex() - 1;
					break;
				case VTDNav.TOKEN_CHARACTER_DATA:
				case VTDNav.TOKEN_CDATA_VAL:
					result.addSubResult(new TextParsingResult(vn.toString(i), vn.getTokenOffset(i),
							vn.getTokenOffset(i) + vn.getTokenLength(i)));
					break;
				}
			}
			// result.setLastTokenIndex(i);
			// return result;
		} catch (NavException e) {
			throw new ElementParsingException(e);
		}
	}

	private boolean determineOmitElement() throws NavException {
		// default: the element is configured to be omitted or not
		boolean omitElement = (boolean) nxmlDocumentParser.getTagProperties(elementName)
				.getOrDefault(ElementProperties.OMIT_ELEMENT, false);

		// however, the element properties file might specify specific
		// attribute-value combinations for the element which are might define
		// different properties
		if (nxmlDocumentParser.getTagProperties(elementName).get(ElementProperties.ATTRIBUTES) != null) {
			// the list of defined attribute properties in the element
			// properties file
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> attributeList = (List<Map<String, Object>>) nxmlDocumentParser
					.getTagProperties(elementName).get(ElementProperties.ATTRIBUTES);

			// the actual properties of the current element
			// we just go through the attributes and build a name-value map
			Map<String, String> attributesOfElement = getElementAttributes();

			// now check if an attribute-value pair defined in the element
			// properties file matches this element;
			// if so, check if it defines the omission of the element
			for (Map<String, Object> attribute : attributeList) {
				String attributeValue = attributesOfElement.get(attribute.get(ElementProperties.NAME));
				if (attributeValue != null && attributeValue.equals(attribute.get(ElementProperties.VALUE))
						&& attribute.containsKey(ElementProperties.OMIT_ELEMENT)) {
					omitElement = (boolean) attribute.get(ElementProperties.OMIT_ELEMENT);
				}
			}
		}
		return omitElement;
	}

	// for access to the local ParsingResult
	protected void editResult(ElementParsingResult result) {
		// does nothing by default, just here for override
	}

	/**
	 * <p>
	 * This method returns the UIMA annotation that will be used to annotate the
	 * contents of this element. The default is
	 * {@link ElementProperties#TYPE_NONE} indicating no annotation. For each
	 * element that should receive an annotation, an appropriate entry should go
	 * into the <tt>elementproperties.xml</tt> file. This file configures
	 * element properties for the default parser. Alternatively, a parser
	 * extending the default parser may overwrite this method and return an
	 * annotation of the desired type.
	 * </p>
	 * <p>
	 * This method is called while the VTDNav cursor is still positioned on the
	 * starting tag token. Thus, attributes of the element may also be parsed
	 * within this method into fields of the extending parser, if required. To
	 * avoid side effects, the use of {@link VTDNav#push()} and
	 * {@link VTDNav#pop()} is advisable in case the VTDNav cursor is moved.
	 * Also, when parsing attribute values into parser fields, keep in mind that
	 * most parsers might be called recursively so a parser cannot have a real
	 * state and fields will be overwritten with the next call to their
	 * {@link #parse()} method. For this reason, this class discloses its
	 * ParsingResult through the field {@link #result} to subclasses. You should
	 * immediately write all required information into {@link #result} by
	 * overwriting {@link #editResult(ElementParsingResult)} and not rely on any
	 * state of the parser.
	 * </p>
	 * 
	 * @return The UIMA element annotation.
	 * @throws ElementParsingException
	 */
	protected Annotation getParsingResultAnnotation() throws ElementParsingException {
		try {
			String annotationClassName = (String) nxmlDocumentParser.getTagProperties(elementName)
					.getOrDefault(ElementProperties.TYPE, ElementProperties.TYPE_NONE);

			annotationClassName = determineAnnotationClassName(annotationClassName);
			if (annotationClassName.trim().equals(ElementProperties.TYPE_NONE))
				return null;

			Constructor<?> constructor = Class.forName(annotationClassName).getConstructor(JCas.class);
			Annotation annotation = (Annotation) constructor.newInstance(nxmlDocumentParser.cas);
			if (annotation instanceof de.julielab.jcore.types.Annotation)
				((de.julielab.jcore.types.Annotation)annotation).setComponentId(PMCReader.class.getName());
			return annotation;
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException | NavException e) {
			throw new ElementParsingException(e);
		}
	}

	/**
	 * Checks if the current element has one or more types given via paths. If
	 * so, checks for the most specific path matching the current element path
	 * and selects the respective annotation type. The default type name given
	 * directly to the annotation type will be overwritten.
	 * 
	 * @param defaultClassName
	 *            The simple type name given directly to the element name.
	 * @return The final class name.
	 * @throws NavException
	 */
	@SuppressWarnings("unchecked")
	private String determineAnnotationClassName(String defaultClassName) throws NavException {
		List<Object> paths = (List<Object>) nxmlDocumentParser.getTagProperties(elementName)
				.getOrDefault(ElementProperties.PATHS, Collections.emptyList());
		String currentElementPath = null;
		if (!paths.isEmpty()) {
			currentElementPath = getElementPath();
		}
		String longestPathFragment = "";
		int longestPathFragmentLength = 0;
		for (Object o : paths) {
			Map<String, String> pathMap = (Map<String, String>) o;
			String pathFragment = pathMap.get(ElementProperties.PATH);
			if (currentElementPath.endsWith(pathFragment)) {
				int pathFragmentLength = pathFragment.split("/").length;
				if (pathFragmentLength > longestPathFragmentLength) {
					longestPathFragment = pathFragment;
					longestPathFragmentLength = pathFragmentLength;
					defaultClassName = pathMap.get(ElementProperties.TYPE);
				} else if (pathFragment.length() == longestPathFragmentLength)
					throw new IllegalArgumentException("The given type paths for element " + elementName
							+ " are ambiguous. The given paths " + pathFragment + " as well as " + longestPathFragment
							+ " are applicable but both are of same length and thus none is more specific than the other. At least one of the path must be made more specific.");
			}
		}
		return defaultClassName;
	}
}
