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
