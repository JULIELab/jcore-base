/**
 * StringArrayParserAndBuilder.java
 * <p>
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the BSD-2-Clause License
 * <p>
 * Author: bernd
 * <p>
 * Current version: 1.0
 * Since version:   1.0
 * <p>
 * Creation date: 09.11.2008
 **/
package de.julielab.jcore.reader.xmlmapper.typeParser;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDNav;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteFeature;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.genericTypes.FeatureTemplate;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.StringArrayBuilder;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handels to parse a StringArray Type from a TypeTemplate to a ConcreteType
 *
 * @author weigel
 */

public class StringArrayParser implements TypeParser {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(FSArrayParser.class);
    private static int rawArrayWarningSent = 0;
    private static Pattern p = Pattern.compile("<[^>]+>");

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
                        if (rawArrayWarningSent < 10) {
                            LOGGER.warn("PLEASE NOTE! You use an array type. The class parsing this type (StringArrayParser) in the XMLMapper returns raw XML string and does not perform entity resolution (i.e. &amp; is not resolved to &). This method should be changed if resolution is necessary. This warning is repeated for 10 cases and then silenced to avoid excessive scrolling and log growth.");
                            ++rawArrayWarningSent;
                        }
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
