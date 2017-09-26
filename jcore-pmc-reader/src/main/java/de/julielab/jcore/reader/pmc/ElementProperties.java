/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.pmc;

public class ElementProperties {
	/**
	 * Block elements are enclosed by line breaks in the CAS document text.
	 */
	public static final String BLOCK_ELEMENT = "block-element";
	/**
	 * Indicates that the respective element should be omitted. That means that
	 * neither for the element nor for any of its descendants parsing will
	 * happen.
	 */
	public static final String OMIT_ELEMENT = "omit-element";
	/**
	 * The UIMA annotation type that should be used to annotate the described
	 * element.
	 */
	public static final String TYPE = "type";
	/**
	 * Indicates that no annotation should be created for this element.
	 */
	public static final String TYPE_NONE = "none";
	/**
	 * The paths property contains a list of path object. Each path object has a
	 * property named 'path' and {@link #TYPE} property. This is used to specify
	 * annotations given paths not just element names. Path matches overwrite
	 * the simple type assignment.
	 */
	public static final String PATHS = "paths";
	/**
	 * The path property of a path / type map given in {@link #PATHS}. The
	 * specified path may be absolute or relative. It will always be chosen the
	 * longest matching path.
	 */
	public static final String PATH = "path";
	/**
	 * Property that defines a list of attribute name-value combinations for
	 * which element properties may be applied. Then, the root element
	 * properties will be overwritten by attribute properties if the attribute
	 * name-value pair matches a particular element.
	 */
	public static final String ATTRIBUTES = "attributes";
	/**
	 * Used for attribute names in conjunction with {@link #ATTRIBUTES}.
	 */
	public static final String NAME = "name";
	/**
	 * Used for attribute values in conjunction with {@link #ATTRIBUTES}.
	 */
	public static final String VALUE = "value";
	/**
	 * Property that is key to a map of (feature name, default feature value)
	 * pairs. Only primitive feature values (string, int, double, ...) are
	 * supported, i.e. types cannot be specified this way. This value of this
	 * property is used by the default element parser to assign default values
	 * to elements that are not handled otherwise.
	 */
	public static final String DEFAULT_FEATURE_VALUES = "default-feature-values";
}
