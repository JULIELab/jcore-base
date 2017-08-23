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
