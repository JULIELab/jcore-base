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

import static org.fest.reflect.core.Reflection.constructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.collection.CollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.reader.xmlmapper.typeParser.FSArrayParser;
import de.julielab.jcore.reader.xmlmapper.typeParser.StandardTypeParser;
import de.julielab.jcore.reader.xmlmapper.typeParser.StringArrayParser;
import de.julielab.jcore.reader.xmlmapper.typeParser.TypeParser;

/**
 * Represents a Template for a type which Contains a List of Feature Templates
 * and all necessary Informations to build a concrete Type.  
 * 
 * @author Weigel
 */
public class TypeTemplate {

	private static final Logger LOGGER = LoggerFactory.getLogger(TypeTemplate.class);
	protected String fullClassName;
	protected List<FeatureTemplate> features;
	protected TypeParser parser;
	protected List<String> xPaths;
	protected boolean externalParser = false;
	protected Map<Integer,String> additionalData;
	protected List<Integer> partOfDocuments; 
	protected boolean multipleInstances;
	protected boolean inlineAnnotation;


	/**
	 * adds a FeatureTemplate to the List 
	 * 
	 * @param feature
	 */
	public void addFeature(FeatureTemplate feature) {
		this.features.add(feature);
	}

	/**
	 * @return the List of all FeatureTemplates
	 */
	public List<FeatureTemplate> getFeatures() {
		return this.features;
	}

	/**
	 * @param features
	 *            the features to set
	 */
	public void setFeatures(List<FeatureTemplate> features) {
		this.features = features;
	}

	/**
	 * Creates a new instance of a TypeTemplate
	 */
	public TypeTemplate() {
		this.features = new ArrayList<FeatureTemplate>();
		this.parser = new StandardTypeParser();
		this.xPaths= new ArrayList<String>();
		this.partOfDocuments = new ArrayList<Integer>();
		this.additionalData = new HashMap<Integer, String>();
	}

	public String toString() {
		String out;
		out = "[TypeTemplate] " + this.fullClassName + "\n";
		for (TypeTemplate typeTemplate : this.features) {
			out += typeTemplate.toString() + "\n";
		}
		return out;
	}

	/**
	 * @return the fullClassName
	 */
	public String getFullClassName() {
		return fullClassName;
	}

	/**
	 * @param fullClassName
	 *            the fullClassName to set
	 */
	public void setFullClassName(String fullClassName) {
		if (fullClassName.equals("org.apache.uima.jcas.cas.FSArray") && !externalParser) {
			this.parser = new FSArrayParser();
		} else if (fullClassName.equals("org.apache.uima.jcas.cas.StringArray") && !externalParser) {
			this.parser = new StringArrayParser();
		}
		this.fullClassName = fullClassName;
	}

	/**
	 * Overrides the parser and sets an external parser
	 * 
	 * @param trim a String containing the full qualified Classname of the parser
	 * @throws CollectionException
	 */
	public void setParser(String trim) throws CollectionException {
		if (trim != null) {
			externalParser = true;
			Class<?> externalParserClass;
			try {
				externalParserClass = Class.forName(trim);
			} catch (ClassNotFoundException e) {
				LOGGER.error("ExternalParser " + trim + " for type or feature " + fullClassName + " returns a ClassNotFoundException", e);
				throw new CollectionException(e);
			}
			this.parser = (TypeParser) constructor().in(externalParserClass).newInstance();
		}else{
			this.parser = null;
		}
	}

	/**
	 * @return the externalParser
	 */
	public TypeParser getParser() {
		return parser;
	}

	/**
	 * @return the valueXPath
	 */
	public List<String> getXPaths() {
		return xPaths;
	}

	public void addAdditionalData(String value, int index) {
		if(this.additionalData==null){
			this.additionalData=new TreeMap<Integer,String>();
		}
		this.additionalData.put(index,value);
	}
	
	public String[] getAdditionalData(){
		if(this.additionalData!=null)
			return this.additionalData.values().toArray(new String[this.additionalData.size()]);
		return new String[0];
	}

	public void addPartOfDocumentText(int partOfDocumentId) {
		this.partOfDocuments.add(partOfDocumentId);
		Collections.sort(this.partOfDocuments);
	}

	/**
	 * returns the first and the last of partOfDocument ids that should be covered by this annotation; that is, the begin of this annotation should be at the begin of the first returned part and the end should be the end of the second returned part.
	 * 
	 * @return int[] ( int[0]->begin index, int[1] -> endIndex )
	 */
	public int[] getOffsetPartIDs() {
		if(this.partOfDocuments!=null && this.partOfDocuments.size()>0){
			return new int[]{this.partOfDocuments.get(0),this.partOfDocuments.get(this.partOfDocuments.size()-1)};
		}else{
			return null;
		}
	}
	

	public void addXPath(String xpath) {
		this.xPaths.add(xpath);
	}
	
	protected TypeTemplate clone() {
		TypeTemplate typeTemplate = new TypeTemplate();
		typeTemplate.fullClassName = new String(this.fullClassName);
		typeTemplate.features = new ArrayList<FeatureTemplate>();
		for (FeatureTemplate feature : this.features) {
			typeTemplate.features.add(feature.clone());
		}
		typeTemplate.parser = this.parser;
		typeTemplate.xPaths = new ArrayList<String>();
		for(String path:this.xPaths){
			typeTemplate.addXPath(new String(path));
		}
		typeTemplate.externalParser = this.externalParser;
		
		typeTemplate.additionalData =  new HashMap<Integer, String>();
		for(Integer id:this.additionalData.keySet()){
			typeTemplate.additionalData.put(id, this.additionalData.get(id));
		}
		typeTemplate.partOfDocuments = new ArrayList<Integer>();
		for (Integer id : this.partOfDocuments) {
			typeTemplate.partOfDocuments.add(id);
		}
		typeTemplate.multipleInstances=this.multipleInstances;
		typeTemplate.inlineAnnotation=this.inlineAnnotation;
		return typeTemplate;
	}

	public boolean isMultipleInstances() {
		return multipleInstances;
	}

	public boolean isInlineAnnotation() {
		return inlineAnnotation;
	}

	public void setMultipleInstances(boolean b) {
		this.multipleInstances = b;
	}

	public void setInlineAnnotation(boolean inlineAnnotation) {
		this.inlineAnnotation = inlineAnnotation;
	}
}
