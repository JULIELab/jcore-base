/** 
 * XMLMapper.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Author: muehlhausen
 * 
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 09.12.2008 
 **/

package de.julielab.jcore.reader.xmlmapper.mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.EOFException;
import com.ximpleware.EncodingException;
import com.ximpleware.EntityException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

import de.julielab.jcore.reader.xmlmapper.genericTypes.ConcreteType;
import de.julielab.jcore.reader.xmlmapper.genericTypes.TypeFactory;
import de.julielab.jcore.reader.xmlmapper.genericTypes.TypeTemplate;
import de.julielab.jcore.reader.xmlmapper.typeBuilder.TypeBuilder;
import de.julielab.jcore.reader.xmlmapper.typeParser.NoDocumentTextCoveredException;
import de.julielab.xml.JulieXMLTools;

/**
 * Generic XML to UIMA TypeSystem mapper. It is intended to be used by an UIMA
 * {@link CollectionReader}.
 * 
 * @author muehlhausen, weigel
 */
public class XMLMapper {

	private static final Logger LOG = LoggerFactory.getLogger(XMLMapper.class);

	/**
	 * List of generic Types, parsed from Mapping File
	 */
	private List<TypeTemplate> genericTemplates;

	private DocumentTextHandler documentTextHandler;

	/**
	 * Creates an new instacne of the XMLMapper
	 * 
	 * @param mappingFileData
	 * @throws FileNotFoundException
	 */
	public XMLMapper(byte[] mappingFileData) {
		readMappingFile(mappingFileData);
	}

	public XMLMapper(InputStream mappingFileDateStream) throws IOException {
		readMappingFile(JulieXMLTools.readStream(mappingFileDateStream, 1000));
	}

	private void readMappingFile(byte[] mappingFileData) {
		assert mappingFileData != null;
		TypeFactory tf = new TypeFactory(mappingFileData);
		try {
			this.genericTemplates = tf.createTemplates();
			this.documentTextHandler = tf.getDocumentTextParser();
		} catch (CollectionException e) {
			e.printStackTrace();
		}
	}

	public void parse(byte[] data, byte[] identifier, JCas jcas) {
		try {
			VTDGen vg = new VTDGen();
			// needed for extraction of mixed-content-XML
			// when there is a whitespace only between two
			// tags, e.g. ...</s> <s id=".">...
			vg.enableIgnoredWhiteSpace(true);
			vg.setDoc(data);
			vg.parse(true);
			VTDNav vn = vg.getNav();

			buildTypes(identifier, jcas, vn);
		} catch (EncodingException e) {
			e.printStackTrace();
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (EntityException e) {
			LOG.error(String.format("Document %s could not be parsed due to an EntityError. Document text is:\n%s", new String(identifier), new String(data)),
					e);
		} catch (CollectionException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			LOG.error(String.format("Document %s could not be parsed due to a general parsing error. Document text is:\n%s", new String(identifier),
					new String(data)), e);
		}
	}

	/**
	 * parses the File to the jcas
	 * 
	 * @param file
	 * @param jcas
	 * @throws CollectionException
	 */
	public void parse(File file, JCas jcas) throws Throwable {
		InputStream is = new FileInputStream(file);
		byte[] b = JulieXMLTools.readStream(is, 1000);
		parse(b, file.getAbsolutePath().getBytes(), jcas);
	}

	private void buildTypes(byte[] identifier, JCas jcas, VTDNav vn) throws CollectionException {
		try {
			DocumentTextData docText = this.documentTextHandler.parseAndAddToCas(vn, jcas, identifier);
			for (TypeTemplate typeTemplate : this.genericTemplates) {
				ConcreteType concreteType = new ConcreteType(typeTemplate);
				try {
					// This parser is the StandardTypeParser unless an external
					// parser has been specified in the mapping file.
					concreteType.getTypeTemplate().getParser().parseType(concreteType, vn, jcas, identifier, docText);
				} catch (NoDocumentTextCoveredException e) {
					// this is not actually an error but just tells us that this
					// concrete type - i.e. the annotation we are currently
					// building - would refer to a non-existing text span and
					// thus should be left out.
					continue;
				}
				TypeBuilder builder = typeTemplate.getParser().getTypeBuilder();
				if (builder == null) {
					throw new RuntimeException(
							"Your TypeParser is not associated with a TypeBuilder. To fix this, return a TypeBuilder in the implementation of the method getTypeBuilder of your TypeParser.");
				}
				builder.buildType(concreteType, jcas);
			}
		} catch (Exception e) {
			LOG.error("", e);
		}
	}
}
