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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteFeature;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.genericTypes.FeatureTemplate;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.StringArrayBuilder;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;

/**
 * Handels to parse a StringArray Type from a TypeTemplate to a ConcreteType
 * 
 * @author weigel
 */

public class StringArrayParser implements TypeParser {
	static Pattern p = Pattern.compile("<[^>]+>");
	private static final Logger LOGGER = LoggerFactory
			.getLogger(FSArrayParser.class);

	public void parseType(ConcreteType concreteType, VTDNav nav, JCas jcas,
			byte[] identifier, DocumentTextData docText) throws Exception {
		VTDNav vn = nav.cloneNav();
		for (String xPath : concreteType.getTypeTemplate().getXPaths()) {
			AutoPilot ap = new AutoPilot(vn);
			Matcher m = p.matcher("");
			ap.selectXPath(xPath);
			while (ap.evalXPath() != -1) {
				if (vn.toElement(VTDNav.FIRST_CHILD, "*")) {
					do {
						LOGGER.warn("PLEASE NOTE! You use an array type. The class parsing this type (StringArrayParser) in the XMLMapper returns raw XML string and does not perform entity resolution (i.e. &amp; is not resolved to &). This method should be changed if resolution is necessary.");
						int val = vn.getText();
						if (val != -1) {
							long fragment = vn.getElementFragment();
							int length = (int) (fragment >> 32);
							int offset = (int) fragment;

							String str = vn.toRawString(offset, length);
							m.reset(str);
							String featureString = m.replaceAll("");
							if (!featureString.equals("")) {
								ConcreteFeature concreteFeature = new ConcreteFeature(
										new FeatureTemplate());
								concreteFeature.setValue(featureString);
								concreteType.addFeature(concreteFeature);
							} else {
								LOGGER.warn("empty String in StringArray found at"
										+ xPath);
							}
						}
					} while (vn.toElement(VTDNav.NEXT_SIBLING));
				}
			}
		}
	}

	public TypeBuilder getTypeBuilder() {
		return new StringArrayBuilder();
	}
}
