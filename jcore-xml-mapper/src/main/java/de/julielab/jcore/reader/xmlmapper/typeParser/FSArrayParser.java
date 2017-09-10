/** 
 * FSArrayParser.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: bernd
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 02.11.2008 
 **/
package de.julielab.jcore.reader.xmlmapper.typeParser;

import org.apache.uima.jcas.JCas;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteFeature;
import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.genericTypes.FeatureTemplate;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.FSArrayBuilder;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;

/**
 * Handels to parse a FSArray Type from a TypeTemplate to a ConcreteType
 * 
 * @author weigel
 */

public class FSArrayParser extends StandardTypeParser {

	@Override
	public void parseType(ConcreteType concreteType, VTDNav nav, JCas jcas, byte[] identifier, DocumentTextData docText) throws Exception {
		VTDNav vn = nav.cloneNav();
		for (FeatureTemplate featureTemplate : concreteType.getTypeTemplate().getFeatures()) {
			for (String xPath : featureTemplate.getXPaths()) {
				AutoPilot ap = new AutoPilot(vn);
				ap.selectXPath(xPath);
				while (ap.evalXPath() != -1) {
					ConcreteFeature concreteFeature = new ConcreteFeature(featureTemplate);
					parseSingleType(concreteFeature, vn, jcas, identifier, docText);
					concreteType.addFeature(concreteFeature);
				}
			}
		}
	}

		/**
	 * @return the typeBuilder
	 */
	public TypeBuilder getTypeBuilder() {
		return new FSArrayBuilder();
	}
}
