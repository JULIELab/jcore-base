/**
 * Utils.java
 *
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: faessler
 *
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 22.03.2011
 **/

package de.julielab.jcore.reader.xmlmapper.mapper;

import com.ximpleware.NavException;
import com.ximpleware.VTDNav;

import de.julielab.xml.JulieXMLTools;

/**
 * Some utility methods like string transformations.
 * 
 * @author faessler
 */
public class MapperUtils {


	/**
	 * Returns XML fragment corresponding to the XML element <code>vn</code>
	 * points to.<br>
	 * This fragment is represented by the exact bytes of the underlying XML
	 * text document corresponding the XML element where <code>vn</code> is
	 * positioned. <br>
	 * XML Entities are resolved.
	 * 
	 * @param vn
	 *            A {@link VTDNav} object positioned at the desired XML element.
	 * @return The entity-resolved XML String corresponding to the XML element
	 *         <code>vn</code> points to.
	 * @throws NavException
	 */
	public static String getElementFragmentString(VTDNav vn)
			throws NavException {
		long fragment = vn.getElementFragment();
		int length = (int) (fragment >> 32);
		int offset = (int) fragment;
		return vn.toString(offset, length);
	}
	
	public static String getElementText(VTDNav vn) throws NavException {
		return JulieXMLTools.getElementText(vn);
	}

}
