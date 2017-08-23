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
package de.julielab.jcore.reader.xmlmapper.typeParser;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteFeature;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.genericTypes.FeatureTemplate;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.mapper.MapperUtils;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.StandardTypeBuilder;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;
import de.julielab.xml.JulieXMLTools;

/**
 * Parses all standard Types, which does not need a special handling in parsing
 * 
 * @author weigel
 */
public class StandardTypeParser implements TypeParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(StandardTypeParser.class);

	public void parseType(ConcreteType concreteType, VTDNav nav, JCas jcas, byte[] identifier, DocumentTextData docText) throws Exception {
		VTDNav vn = nav.cloneNav();
		// System.out.println(this.getClass().getSimpleName() + ".parseType:" +
		// concreteType.getTypeTemplate().getFullClassName());

		boolean inline = concreteType.getTypeTemplate().isInlineAnnotation();
		if (concreteType.getTypeTemplate().isMultipleInstances() || inline) {
			if (concreteType.getTypeTemplate().getXPaths().size() > 0) {
				int inlinePosition = 0;
				String text = docText.getText();
				for (String xPath : concreteType.getTypeTemplate().getXPaths()) {
					// xpath auswerten
					AutoPilot ap = new AutoPilot(vn);
					ap.selectXPath(xPath);
					while (ap.evalXPath() != -1) {
						int begin = 0;
						int end = 0;
						if (inline) {
							String inlineText = JulieXMLTools.getElementText(vn);
							// System.out.println(inlineText);
							begin = text.indexOf(inlineText, inlinePosition);
							end = begin + inlineText.length();
							if (begin == -1)
								throw new IllegalStateException("Inline annotation text \"" + inlineText
										+ "\" was not found in the document text after position " + inlinePosition + ".\nDocument identifier is: "
										+ new String(identifier) + ".\nDocument text is: \"" + text + "\"");
						}
						if (begin >= 0) {
							ConcreteFeature realType = new ConcreteFeature(concreteType.getTypeTemplate());
							realType.getTypeTemplate().setMultipleInstances(false);
							realType.getTypeTemplate().setInlineAnnotation(false);
							realType.setBegin(begin);
							realType.setEnd(end);
							parseSingleType(realType, vn, jcas, identifier, docText);
							concreteType.addFeature(realType);

							inlinePosition = end;
						}
					}
				}
			} else {
				LOGGER.warn("type is marked as Multiple Instance withoutxPath. parsing only one Instance insted");
				concreteType.getTypeTemplate().setMultipleInstances(false);
				parseSingleType(concreteType, vn, jcas, identifier, docText);
			}
		} else {
			parseSingleType(concreteType, vn, jcas, identifier, docText);
		}
	}

	protected void parseSingleType(ConcreteType concreteType, VTDNav nav, JCas jcas, byte[] identifier, DocumentTextData docText) throws Exception,
			XPathParseException, XPathEvalException, NavException {

		VTDNav vn = nav.cloneNav();
		for (FeatureTemplate featureTemplate : concreteType.getTypeTemplate().getFeatures()) {
			ConcreteFeature concreteFeature = new ConcreteFeature(featureTemplate);
			if (featureTemplate.isType()) {
				featureTemplate.getParser().parseType(concreteFeature, vn, jcas, identifier, docText);
				if (concreteFeature.getConcreteFeatures() != null) {
					concreteType.addFeature(concreteFeature);
				}
			} else {
				// For non-type features without an external parser
				// specification in the mapping file, the parse has been set to
				// null. Thus: Simple features like strings have no parser (is
				// null).
				if (featureTemplate.getParser() != null) {
					featureTemplate.getParser().parseType(concreteFeature, vn, jcas, identifier, docText);
					concreteType.addFeature(concreteFeature);
				} else {
					if (featureTemplate.getValueMap() != null && featureTemplate.getValueMap().size() == 1) {
						directValueFromDefaultMapping(concreteType, featureTemplate, concreteFeature);
					} else {
						parseStandardFeature(concreteType, vn, featureTemplate, concreteFeature);
					}
				}
			}
		}
		int[] ids = concreteType.getTypeTemplate().getOffsetPartIDs();
		if (ids != null) {
			int begin = docText.get(ids[0]).getBegin();
			int end = docText.get(ids[1]).getEnd();
			// the document text this annotation is referring to does not exist.
			// Skip this annotation
			if (end - begin <= 0)
				throw new NoDocumentTextCoveredException();
			concreteType.setBegin(begin);
			concreteType.setEnd(end);
		}
	}

	public static void parseStandardFeature(ConcreteType concreteType, VTDNav nav, FeatureTemplate featureTemplate, ConcreteFeature concreteFeature)
			throws XPathParseException, XPathEvalException, NavException {
		VTDNav vn = nav.cloneNav();
		for (String xpath : featureTemplate.getXPaths()) {
			AutoPilot ap = new AutoPilot(vn);
			ap.selectXPath(xpath);
			int i = ap.evalXPath();
			while (i != -1) {
				String value;
				if (vn.getTokenType(i) == VTDNav.TOKEN_ATTR_NAME) {
					value = vn.toString(i + 1);
				} else {
					value = MapperUtils.getElementText(vn);
				}
				String mappedValue = featureTemplate.getMappedValue(value);
				concreteFeature.setValue(mappedValue);
				concreteType.addFeature(concreteFeature);
				concreteFeature = new ConcreteFeature(featureTemplate);
				i = ap.evalXPath();
			}
		}
	}

	// TODO rename (use a verbal phrase not a noun phrase)
	private void directValueFromDefaultMapping(ConcreteType concreteType, FeatureTemplate featureTemplate, ConcreteFeature concreteFeature) {
		concreteFeature.setValue(featureTemplate.getMappedValue(""));
		concreteType.addFeature(concreteFeature);
	}

	public boolean equals(Object obj) {
		return this.getClass().getCanonicalName().equals(obj.getClass().getCanonicalName());
	}

	public TypeBuilder getTypeBuilder() {
		return new StandardTypeBuilder();
	}
}
