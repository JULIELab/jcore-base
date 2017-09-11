/** 
 * XMIToIOBApplication.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: faessler
 * 
 * Current version: 1.1
 * Since version:   1.0
 *
 * Creation date: 06.09.2007 
 * 
 * An UIMA-Pipeline capable of reading all XMI files from a directory and converting the annotations
 * into IOB format.
 * 
 * The iob files are stored in the input directory, named <oldfilename>.iob 
 **/

/**
 * 
 */
package de.julielab.jcore.consumer.cas2iob.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author faessler
 * 
 */
public class XMIToIOBApplication {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(XMIToIOBApplication.class);

	public static void main(String args[]) throws Exception {

		if (args.length != 3) {
			System.out
					.println("Usage: XMIToIOBApplication <XMI directory> <TS descriptor> <consumer descriptor>");
			System.exit(0);
		}

		File xmiDir = new File(args[0]);
		String tsDescriptorName = args[1];
		String consumerDescriptorName = args[2];

		if (!xmiDir.exists() || !xmiDir.isDirectory()) {
			LOGGER
					.error("specified XMI dir does not exist or is not a directory: " + xmiDir);
			System.exit(-1);
		}

		LOGGER.info("Reading XMI file...");

		ResourceSpecifier spec;
		CasConsumer toIOBConsumer;

		// getting xmis in directory
		File[] xmis = xmiDir.listFiles(new ExtensionFileFilter("xmi"));
		if (xmis.length < 1) {
			LOGGER.error("specified directory contains no xmi files");
		}

		// create new iob folder
		Path iobPath = Paths.get(xmiDir.getAbsolutePath(),"iob/");
		File iobDir = new File(iobPath.toString());
		if (!iobDir.exists()) {
			iobDir.mkdir();
		}
		
		// read type system descriptor
		XMLParser xmlParser = UIMAFramework.getXMLParser();
		TypeSystemDescription tsDesc = xmlParser
				.parseTypeSystemDescription(new XMLInputSource(tsDescriptorName));

		// creating ToIOBConsumer
		spec = UIMAFramework.getXMLParser().parseResourceSpecifier(
				new XMLInputSource(consumerDescriptorName));
		toIOBConsumer = UIMAFramework.produceCasConsumer(spec);

		// now process all xmis
		for (int i = 0; i < xmis.length; i++) {
			File myXMI = xmis[i];
			System.out.println("Processing file " + myXMI.getAbsolutePath() + ".");

			// reading XMI file and creating corresponding CAS object
			FileInputStream inStream = new FileInputStream(myXMI);
			
			CAS myCAS = CasCreationUtils.createCas(tsDesc, null, null);
//			System.err.println(myXMI.getAbsolutePath() + ".iob");
			XmiCasDeserializer.deserialize(inStream, myCAS);

			// reconfigure Cos2IOBConsumer
			String outFile = Paths.get(iobPath.toString(), myXMI.getName()).toString() + ".iob";
//			String outFile = myXMI.getAbsolutePath() + ".iob";
//			String outFile = "/tmp/myiob/" + myXMI.getName() + ".iob";
			toIOBConsumer.setConfigParameterValue("outFileName", outFile);
			toIOBConsumer.reconfigure();
			LOGGER.debug("will write file to: " + outFile);

			// process this xmi
			toIOBConsumer.processCas(myCAS);

		}
	}
}

class ExtensionFileFilter implements FilenameFilter {
	String ext = ""; // the extension to use, e.g. "xmi"

	public ExtensionFileFilter(String ext) {
		this.ext = ext;
	}

	public boolean accept(File f, String s) {
		return s.toLowerCase().endsWith("." + ext);
	}
}
