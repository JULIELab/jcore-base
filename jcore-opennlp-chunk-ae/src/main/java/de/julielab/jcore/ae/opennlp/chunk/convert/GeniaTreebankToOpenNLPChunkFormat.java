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
package de.julielab.jcore.ae.opennlp.chunk.convert;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * * Copyright (c) 2015, JULIE Lab. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the BSD-2-Clause
 * License
 * 
 * This java class is used to convert GENIA Treebank version 1.0 xml files into
 * one file in the openNLP 1.6 Chunker Training format
 * 
 * Comment from faessler, 09/2017: This conversion doesn't take into account
 * that phrases may be embedded and some other issues. For this reason, the
 * resulting chunking is rather weird when training a chunker on the resulting
 * data. Consider using {@link ToIOBConverter} instead.
 * 
 * @author rubruck
 *
 */
public class GeniaTreebankToOpenNLPChunkFormat {
	/**
	 * args[0] = input directory args[1] = outputFile
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// check correct usage
		if (args.length != 2) {
			System.err.println("Usage: GeniaTreebankToOpenNLPChunkFormat <inputDir> <outputFile>");
			System.exit(-1);
		}

		// check if args[0] exists and is a directory
		File inputDir = new File(args[0]);
		if (!inputDir.exists()) {
			System.err.println(inputDir.getAbsolutePath() + " does not exist!");
			System.exit(-1);
		}
		if (!inputDir.isDirectory()) {
			System.err.println(inputDir.getAbsolutePath() + " is not a directory!");
			System.exit(-1);
		}

		// instantiate SAXParser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			System.err.println("Error while instantiating the SAX Parser.");
			e.printStackTrace();
			System.exit(-1);
		} catch (SAXException e) {
			System.err.println("Error while instantiating the SAX Parser.");
			e.printStackTrace();
			System.exit(-1);
		}

		// instantiate custom XML Tag Handler
		File out = new File(args[1]);
		if ((out.exists()) && (out.isDirectory())) {
			System.err.println(out.getAbsolutePath() + " already exists and is a directory.");
			System.exit(-1);
		}

		if (out.exists()) {
			Scanner reader = new Scanner(System.in); // Reading from System.in
			System.out.println(out.getAbsolutePath() + " (output file) already exists. Overwrite? y / n");
			String answer = reader.next();
			if (!(answer.equalsIgnoreCase("Y") || answer.equalsIgnoreCase("YES"))) {
				System.out.println("File will not be overwritten. Converter terminates now.");
				System.exit(0);
			}
			// otherwise delete the old file
			if (!out.delete()) {
				System.err.println("Old output file could not be deleted.");
				System.exit(-1);
			}
		}

		try {
			out.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("Error while creating a new output file.");
			System.exit(-1);
		}

		DefaultHandler handler = new GeniaTreebankXMLHandler(args[1]);

		// read all xml files in inputDir
		for (File currentFile : inputDir.listFiles()) {
			// check if file is an xml
			int indexOfDot = currentFile.getName().lastIndexOf(".");
			if (indexOfDot > -1) {
				String type = currentFile.getName().substring(indexOfDot + 1);
				if (type.equals("xml")) {
					System.out.println("Converting " + currentFile.getName() + " ...");

					// parse current file
					try {
						parser.parse(currentFile, handler);
					} catch (SAXException e) {
						System.err.println("Error while parsing input xml file.");
						e.printStackTrace();
						System.exit(-1);
					} catch (IOException e) {
						System.err.println("Error while writing to output file.");
						e.printStackTrace();
						System.exit(-1);
					}
				}
			}
		}
	}

}
