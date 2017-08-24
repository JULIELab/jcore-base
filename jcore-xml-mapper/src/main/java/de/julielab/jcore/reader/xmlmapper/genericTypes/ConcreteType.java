/** 
 * ConcreteType.java
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
 * Creation date: 28.10.2008 
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
