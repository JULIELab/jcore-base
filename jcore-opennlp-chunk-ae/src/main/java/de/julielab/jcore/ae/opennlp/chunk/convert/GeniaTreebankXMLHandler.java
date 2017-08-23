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
package de.julielab.jcore.ae.opennlp.chunk.convert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 * 
 * this class is faced with the problem to
 * extract the flat chunk categories
 * from the hierarchical constituent categories
 * given in the Genia Treebank
 * This is solved by giving each chunk the
 * lowest / closest category of the corresponding
 * constitute
 * When a token is read it belongs to consCatStack.top()

 * @author rubruck
 *
 */
public class GeniaTreebankXMLHandler extends DefaultHandler
{
	// The Reader can be in one of three states:
	private final int BEGIN = 0;
	private final int IN = 1;
	private final int OUT = 2;
	private int currentState = OUT;

	// true -> next content is token text
	private boolean inToken = false;
	private String currentPOS = "";

	// stores the constituents hierarchy at each step during the reading process
	private Stack<String> consCatStack = new Stack<String>();

	// result String
	private StringBuilder sb = new StringBuilder();

	// output File
	private File outputFile = null;

	/*
	 * constructor used for intialization
	 * receives output-file, to write the converted data to
	 */
	public GeniaTreebankXMLHandler(String out)
	{
		super();
		outputFile = new File(out);
	}

	/*
	 * Called at an opening xml tag
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException 
	{
		// cons (constituent)
		// attribute cat (category)
		// -> chunk tag
		if (qName.equals("cons"))
		{
			String cat = attributes.getValue("cat");
			if ((cat!= null)&&(!cat.equals("")))
			{
				currentState = BEGIN;
				consCatStack.push(cat);
			}
		}			

		// tok (token)
		// attribute cat (category)
		// -> POS tag
		if (qName.equals("tok"))
		{
			String cat = attributes.getValue("cat");
			if ((cat!= null)&&(!cat.equals("")))
			{
				inToken = true;
				currentPOS = cat;
				// special cases:
				// use . instead of PERIOD
				if (currentPOS.equals("PERIOD"))
					currentPOS = ".";
				else if (currentPOS.equals("COMMA"))
					currentPOS = ",";
			}
		}

		// begin of sentence
		if (qName.equals("sentence"))
		{
			consCatStack = new Stack<String>();
		}
	}

	/*
	 * called when xml end tag is reached
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		// cons (constituent)
		if (qName.equals("cons"))
		{
			consCatStack.pop();
			// if only the S = sentence constituent remains,
			// there is no constituent still open, that contains tokens 
			if ((consCatStack.isEmpty()) || (consCatStack.peek().equals("S")))
				currentState = OUT;
			// in this case, there is still a constituent open, that can start now
			// since a chunker creates a flat structure, it is not taken
			// into account, that a previous constituent might be continued
			// and just be separated by another constituent
			else
				currentState = BEGIN;
		}			

		// tok (token)
		if (qName.equals("tok"))
		{
			inToken = false;
			currentPOS = "";
		}

		// end of sentence
		if (qName.equals("sentence"))
		{
			currentState = OUT;
			// insert an empty line
			sb.append("\n");
		}

		// end of current input file
		if (qName.equals("Annotation"))
		{
			// write current content of sb to the output file
			BufferedWriter writer = null;
			try
			{
				writer = new BufferedWriter(new FileWriter(outputFile));
				writer.append(sb.toString());
				writer.close();
			}
			catch (IOException e)
			{
				System.err.println("Error while writing to output file " + outputFile.getAbsolutePath() + "." );
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	/*
	 * called when content of an xml tag is reached
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(char ch[], int start, int length) throws SAXException
	{
		/*
		 * create a format like e.g.:
		 * the DT B-NP
		 * current JJ I-NP
		 * account NN I-NP
		 * ...
		 * . . O
		 */
		
		// remove all whitespaces,
		// that are not used to divide the token from the pos cat and chunk cat
		if (inToken)
		{
			// token text
			sb.append(new String(ch, start, length).replaceAll("\\s+", ""));
			// tab
			sb.append(" ");
			// POS tag
			sb.append(currentPOS.replaceAll("\\s+", ""));
			// tab
			sb.append(" ");
			// B(egin) - I(n) - O(ut)
			if (currentState == OUT)
				sb.append("O\n");
			else
			{
				if (currentState == BEGIN)
				{
					sb.append("B-");
					currentState = IN;
				}
				else
					sb.append("I-");

				// chunk category
				sb.append(consCatStack.peek().replaceAll("\\s+", ""));
				sb.append("\n");
			}
		}
	}
}
