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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Template for a Feature which contains all necessary Informations to build a concrete Type.  
 * 
 * @author Weigel
 */
public class FeatureTemplate extends TypeTemplate {

	private boolean type;
	private Map<String, String> valueMap = null;
	private String tsName;

	public FeatureTemplate() {
		super();
		this.valueMap = new HashMap<String, String>();
	}
	
	/**
	 * Adds a ValueMapping to the Template
	 * 
	 * @param xmlElement
	 * @param value
	 */
	public void addVauleMapping(String xmlElement, String value) {
		if (valueMap == null) {
			valueMap = new HashMap<String, String>();
		}
		valueMap.put(xmlElement, value);
	}

	/**
	 * @param valueMapping
	 *            the valueMapping to set
	 */
	public void setValueMap(HashMap<String, String> valueMapping) {
		this.valueMap = valueMapping;
	}

	/**
	 * @return the valueMapping
	 */
	public Map<String, String> getValueMap() {
		return valueMap;
	}

	/**
	 * returns a mapped Value for a spezific value from the xml
	 * 
	 * @param xmlElement
	 * @return a mapped Valu
	 */
	public String getMappedValue(String xmlElement) {
		if (this.valueMap == null) {
			return xmlElement;
		}
		String mappedValue = this.valueMap.get(xmlElement);
		if (mappedValue == null) {
			mappedValue = this.valueMap.get("defaultValueMapping");
			if (mappedValue == null) {
				mappedValue = xmlElement;
			}
		}
		return mappedValue;
	}

	/**
	 * can mark the feature as a type.
	 * 
	 * @param type
	 */
	public void setType(boolean type) {
		this.type = type;
	}

	/**
	 * @return wheter this feature is a Type or not
	 */
	public boolean isType() {
		return this.type;
	}

	public String toString() {
		if (this.isType()) {
			return super.toString();
		}
		String out = "[FeatureTemplate] " + this.getFullClassName() + ":" + "at: " + Arrays.toString(this.xPaths.toArray());
		if (this.valueMap != null) {
			out += "\n  ValueMapping:";
			for (String key : this.valueMap.keySet()) {
				out += "\n" + key + " = " + this.valueMap.get(key);
			}
		}
		return out;
	}

	/**
	 * @return the tsName
	 */
	public String getTsName() {
		return tsName;
	}

	/**
	 * @param tsName
	 *            the tsName to set
	 */
	public void setTsName(String tsName) {
		this.tsName = tsName;
	}
	@Override
	public FeatureTemplate clone(){
//		private boolean type;
//		private Map<String, String> valueMap = null;
//		private String tsName;
		FeatureTemplate template = new FeatureTemplate();
		template.fullClassName = new String(this.fullClassName==null?"":this.fullClassName);
		template.features = new ArrayList<FeatureTemplate>();
		for (FeatureTemplate feature : this.features) {
			template.features.add(feature.clone());
		}
		template.parser = this.parser;
		template.xPaths = new ArrayList<String>();
		for(String path:this.xPaths){
			template.addXPath(new String(path));
		}
		template.externalParser = this.externalParser;
		
		template.additionalData =  new HashMap<Integer, String>();
		for(Integer id:this.additionalData.keySet()){
			template.additionalData.put(id, this.additionalData.get(id));
		}
		template.partOfDocuments = new ArrayList<Integer>();
		for (Integer id : this.partOfDocuments) {
			template.partOfDocuments.add(id);
		}
		
		template.type= this.type;
		template.tsName = new String(this.tsName==null?"":this.tsName);
		template.valueMap = new HashMap<String, String>();
		for(String key:this.valueMap.keySet()){
			template.valueMap.put(key, new String(this.valueMap.get(key)));
		}
		template.multipleInstances=this.multipleInstances;
		template.inlineAnnotation=this.inlineAnnotation;
		return template;
	}

}
