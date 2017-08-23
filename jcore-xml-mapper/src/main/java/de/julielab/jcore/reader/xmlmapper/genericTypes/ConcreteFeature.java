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

/**
 * Represents a Feature of an Type from the (user-defined) UIMA TypeSystem with a concrete value.
 * 
 * @author Weigel
 */
public class ConcreteFeature extends ConcreteType {

	private String value;

	/**
	 * Sets the value of a ConcreteFeature
	 * 
	 * @param trim
	 */
	public void setValue(String trim) {
		this.value = trim;
	}

	/**
	 * Returns the Value of a ConcreteFeature
	 * 
	 * @return value
	 */
	public String getValue() {
		return value;
	}

	public String toString() {
		if (this.isType()) {
			return super.toString();
		}
		String out = "[ConcreteFeature]" + this.getTsName() + " (" + this.getFullClassName() + ") = " + this.value;
		return out;
	}

	public String getFullClassName() {
		return this.getTypeTemplate().getFullClassName();
	}

	/**
	 * Creates an new Instance of a ConcretFeatured based on a Feature Template
	 * 
	 * @param featureTemplate
	 */
	public ConcreteFeature(FeatureTemplate featureTemplate) {
		super(featureTemplate);
	}

	/**
	 * Creates an new Instance of a ConcretFeatured based on a Type Template
	 * 
	 * @param typeTemplate
	 */
	public ConcreteFeature(TypeTemplate typeTemplate) {
		super(typeTemplate);
	}

	/**
	 * @return the tsName
	 */
	public String getTsName() {
		return ((FeatureTemplate) this.getTypeTemplate()).getTsName();
	}

	/**
	 * @return the type
	 */
	public boolean isType() {
		return ((FeatureTemplate) this.getTypeTemplate()).isType();
	}
}
