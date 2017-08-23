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
package de.julielab.jcore.reader.xmlmapper.genericTypes;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Type form the (user-extended) UIMA TypeSystem that has features with concrete values (ConcreteFeatures).
 * 
 * @author Weigel
 */
public class ConcreteType {
	static Logger LOGGER = LoggerFactory.getLogger(ConcreteType.class);
	TypeTemplate typeTemplate;
	private List<ConcreteFeature> features;
	/**
	 * optional offset
	 */
	private int begin;
	private int end;
	
	/**
	 * Creates a new Instance of a ConcreteType on the base of a TypeTemplate
	 * 
	 * @param typeTemplate
	 */
	public ConcreteType(TypeTemplate typeTemplate) {
		this.setTypeTemplate(typeTemplate.clone());
		this.features = new ArrayList<ConcreteFeature>();
	}
	

	/**
	 * Adds a ConcreteFeature to the List of ConcreteFeatures
	 * 
	 * @param feature
	 */
	public void addFeature(ConcreteFeature feature) {
		if (this.features == null)
			this.features = new ArrayList<ConcreteFeature>();
		this.features.add(feature);
	}

	/**
	 * Returns the list of ConcreteFeatures
	 * 
	 * @return List<ConcretFeautures>
	 */
	public List<ConcreteFeature> getConcreteFeatures() {
		return this.features;
	}

	public String toString() {
		String out;
		out = "[ConcreteType] " + this.getFullClassName() + ")\n";
		if (this.features != null) {
			for (ConcreteFeature concreteFeature : this.features) {
				out += concreteFeature.toString() + "\n";
			}
		}
		return out;
	}

	/**
	 * Deprecated Constructor, don't use
	 */
	@Deprecated
	public ConcreteType() {
	}

	/**
	 * @return the fullClassName
	 */
	public String getFullClassName() {
		return typeTemplate.getFullClassName();
	}

	
	/**
	 * @return the typeTemplate
	 */
	public TypeTemplate getTypeTemplate() {
		return typeTemplate;
	}

	
	/**
	 * @param typeTemplate the typeTemplate to set
	 */
	public void setTypeTemplate(TypeTemplate typeTemplate) {
		this.typeTemplate = typeTemplate;
	}


	public void setBegin(int begin) {
		this.begin = begin;
	}


	public void setEnd(int end) {
		this.end = end;
	}


	public int getBegin() {
		return begin;
	}
	
	public int getEnd() {
		return end;
	}
}
