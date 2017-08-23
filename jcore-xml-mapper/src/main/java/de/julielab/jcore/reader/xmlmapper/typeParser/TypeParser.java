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

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;

import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.mapper.DocumentTextData;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;

/**
 * standard Interface to define an external Parser
 * 
 * @author weigel
 */
public interface TypeParser {

	/**
	 * @return an instance of the TypeBuilder class. Wheter the Type Need special handling or not.
	 *         if not just return a new instance of the StandardTypeBuilder
	 */
	public TypeBuilder getTypeBuilder();

	/**
	 * Parses a Type. Gather all necessary Infomations from the vdtnav, and fill the concrete Type.
	 * The corresponding TypeTemplate is part of the ConcreteType
	 * 
	 * @param concreteType
	 * @param xpath
	 * @param vn
	 * @param String identifier
	 * @param DocumentTextData docText
	 * @throws Exception 
	 * @throws CollectionException
	 */
	public void parseType(ConcreteType concreteType, VTDNav vn, JCas jcas, byte[] identifier, DocumentTextData docText) throws Exception;

}
