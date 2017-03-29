package de.julielab.jcore.reader.pmc.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import com.ximpleware.NavException;
import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.pmc.ElementProperties;
import de.julielab.jcore.types.Zone;

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
	public ElementParsingResult parse() throws ElementParsingException, DocumentParsingException {
		try {
			checkCursorPosition();
			// since this parser does not know the element is is used upon, set
			// it first for the parsing result creation
			elementName = vn.toString(vn.getCurrentIndex());
			int elementDepth = vn.getCurrentDepth();
			ElementParsingResult result = createParsingResult();
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
					// The subresult parser moves the cursor towards the end of
					// its element. We should continue where the last parser
					// stopped. We decrement i because the for-loop will
					// immediately increment for the next iteration. But for the
					// next iteration we just want to be exactly where the last
					// parser stopped.
					i = subResult.getLastTokenIndex() - 1;
					break;
				case VTDNav.TOKEN_CHARACTER_DATA:
				case VTDNav.TOKEN_CDATA_VAL:
					result.addSubResult(new TextParsingResult(vn.toString(i), vn.getTokenOffset(i),
							vn.getTokenOffset(i) + vn.getTokenLength(i)));
					break;
				}
			}
			result.setLastTokenIndex(i);
			return result;
		} catch (NavException e) {
			throw new ElementParsingException(e);
		}
	}

	// for access to the local ParsingResult
	protected void editResult(ElementParsingResult result) {
		// does nothing by default, just here for override
	}

	/**
	 * <p>
	 * This method returns the UIMA annotation that will be used to annotate the
	 * contents of this element. This is a {@link Zone} as a default. Should be
	 * overwritten by implementing subclasses to use a more specific annotation.
	 * </p>
	 * <p>
	 * This method is called while the VTDNav cursor is still positioned on the
	 * starting tag token. Thus, attributes of the element may also be parsed
	 * within this method into fields of the extending parser, if required. To
	 * avoid side effects, the use of {@link VTDNav#push()} and
	 * {@link VTDNav#pop()} is advisable. Also, when parsing attribute values
	 * into parser fields, keep in mind that most parsers might be called
	 * recursively so a parser cannot have a real state and fields will be
	 * overwritten with the next call to their {@link #parse()} method. For this
	 * reason, this class discloses its ParsingResult through the field
	 * {@link #result} to subclasses. You should immediately write all required
	 * information into {@link #result} by overwriting
	 * {@link #editResult(ElementParsingResult)} and not rely on any state of
	 * the parser.
	 * </p>
	 * 
	 * @return The UIMA element annotation.
	 */
	protected Annotation getParsingResultAnnotation() {
		String annotationClassName = (String) nxmlDocumentParser.getTagProperties(elementName)
				.getOrDefault(ElementProperties.TYPE, Zone.class.getCanonicalName());
		if (annotationClassName.trim().equals(ElementProperties.TYPE_NONE))
			return null;
		try {
			Constructor<?> constructor = Class.forName(annotationClassName).getConstructor(JCas.class);
			return (Annotation) constructor.newInstance(nxmlDocumentParser.cas);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return new Zone(nxmlDocumentParser.cas);
	}

	/**
	 * <p>
	 * Helper method to determine whether the current token index still lies
	 * within the element this parser has been called for. Since VTD-XML does
	 * unfortunately not use its VTDNav.TOKEN_ENDING_TAG token type, we have to
	 * infer from token type and token depth whether we have left the element or
	 * not.
	 * </p>
	 * <p>
	 * When an XML element ends, the next token may be another starting tag with
	 * the same depth as the original element (a sibling in the XML tree) or
	 * something else, e.g. text data, with a lower depth belonging to the
	 * parent element. Those two cases are checked in this method.
	 * </p>
	 * 
	 * @param index
	 *            The token index to check.
	 * @param elementDepth
	 *            The depth of the original element this parser is handling.
	 * @return True, if the token with the given index belongs to the element
	 *         for this parser.
	 */
	private boolean tokenIndexBelongsToElement(int index, int elementDepth) {
		return !((vn.getTokenType(index) == VTDNav.TOKEN_STARTING_TAG && vn.getTokenDepth(index) <= elementDepth)
				|| vn.getTokenDepth(index) < elementDepth);
	}

}
