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
