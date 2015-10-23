/** 
 * ConcreteFeature.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: bernd
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 28.10.2008 
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
