/**
 * MSdoc2txtReader.java
 * 
 * @author Christina Lohr
 *
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Current version: 1.1
 * Since version:   1.1
 *
 * Creation date: 11.04.2017
 * 
 * There are different ways to read *.doc-files of MS Word and transform it to plain text.
 * 
 *  * ReadSingleMSdoc.getContentTextOnly(): only the text
 *  
 *  * ReadSingleMSdoc.getContentTextWithMarkedTables(): text and tables marked
 *  * by | and 4 space characters for using text with the brat rapid annotation tool
 *  
 *  * ReadSingleMSdoc.getContentLabParams(): text and tables marked by | and 4 space characters
 *  	(only parameters from the laboratory in the document, if you read a clinical document)
 *  
 *  * ReadSingleMSdoc.getContentHTML(): text and tables marked by HTML-code <table> ... </table>
 *  
 * We removed non-readable characters, see characterChecker().
 * 
 * see http://stackoverflow.com/questions/17062841/read-table-from-docx-file-using-apache-poi
 */

package de.julielab.jcore.reader.msdoc2txt.main;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ReadSingleMSdoc {

	static String filename;
	static Range docRange;

	static String contentTextOnly;
	static String contentTextWithMarkedTables;
	static String contentLabParams;
	static String contentHTML;

	// static String outputDocument = "";

	public static void setFilename(String file) {
		filename = file;
	}

	public static void setDocRange() // open file
	{
		docRange = null;

		try {

			InputStream fis = new FileInputStream(filename);
			POIFSFileSystem fs = new POIFSFileSystem(fis);

			@SuppressWarnings("resource")
			HWPFDocument doc = new HWPFDocument(fs);

			docRange = doc.getRange();

		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}

	public static Range getDocRange() {
		return docRange;
	}

	public static String getContentTextOnly() {
		return contentTextOnly;
	}

	public static String getContentTextWithMarkedTables() {
		return contentTextWithMarkedTables;
	}

	public static String getContentLabParams() {
		return contentLabParams;
	}

	public static String getContentHTML() {
		return contentHTML;
	}

	public static void doc2Text() {

		contentTextOnly = "";
		contentTextWithMarkedTables = "";
		contentHTML = "";

		boolean intable = false;
		boolean inrow = false;
		boolean td_open = false;

		for (int i = 0; i < docRange.numParagraphs(); i++) {
			Paragraph par = docRange.getParagraph(i);

			if (par.isInTable()) {
				if (!intable) {
					contentTextOnly = contentTextOnly + "\n";
					contentTextWithMarkedTables = contentTextWithMarkedTables + "\n";
					contentHTML = contentHTML + "<table>\n";

					intable = true;
				}
				if (!inrow) {
					contentHTML = contentHTML + "<tr>\n";
					inrow = true;
				}
				if (par.isTableRowEnd()) {
					contentHTML = contentHTML + "</tr>\n";
					inrow = false;
				} else {
					if (td_open == false) {
						if (par.text().endsWith("\u0007")) {
							contentTextOnly = contentTextOnly + par.text() + "\n";
							contentTextWithMarkedTables = contentTextWithMarkedTables + "|    " + par.text() + "    |";
							contentHTML = contentHTML + "<td>" + par.text() + "</td>\n";
						} else {
							td_open = true;
							contentTextOnly = contentTextOnly + par.text();
							contentTextWithMarkedTables = contentTextWithMarkedTables + "|    " + par.text() + " ";
							contentHTML = contentHTML + "<td>" + par.text() + "<br>";
						}
					} else {
						if (par.text().endsWith("\u0007")) {
							td_open = false;
							contentTextOnly = contentTextOnly + par.text() + "\n";
							contentTextWithMarkedTables = contentTextWithMarkedTables + par.text() + "    |";
							contentHTML = contentHTML + par.text() + "</td>\n";

						} else {
							contentTextOnly = contentTextOnly + par.text();
							contentTextWithMarkedTables = contentTextWithMarkedTables + par.text() + " ";
							contentHTML = contentHTML + par.text() + "<br>";
						}
					}
				}
			} else {
				if (inrow) {
					contentHTML = contentHTML + "</table>";
					inrow = false;
				}
				if (intable) {
					contentHTML = contentHTML + par.text() + "<br/>";
					intable = false;
				}
				contentTextOnly = contentTextOnly + par.text();
				contentTextWithMarkedTables = contentTextWithMarkedTables + par.text();
				contentHTML = contentHTML + par.text() + "<br/>";
			}
		}

		contentTextOnly = characterChecker(contentTextOnly);

		String[] contentSplit = characterChecker(contentTextWithMarkedTables).split("\n");

		boolean foundLabParams = false;

		for (int i = 0; i < contentSplit.length; i++) {
			if (i + 2 < contentSplit.length) {
				if ((contentSplit[i].contains("Laborwerte")) && (contentSplit[i + 2].contains("Normwert"))) {
					foundLabParams = true;
				}
			}

			if (!foundLabParams) {
				contentTextWithMarkedTables = contentTextWithMarkedTables + contentSplit[i] + "\n";
			} else {
				contentLabParams = contentLabParams + contentSplit[i] + "\n";
			}
		}

		contentTextOnly = characterChecker(contentTextOnly);
		contentTextWithMarkedTables = characterChecker(contentTextWithMarkedTables);
		contentHTML = characterChecker(contentHTML);

	}

	/**
	 * MSdocReade don't use all characters.
	 * 
	 * Private Use Area from U+E000 until U+F8FF
	 * https://unicode-table.com/en/blocks/private-use-area/
	 * 
	 * Basic Latin from 0020 until 007F, 007F is DEL-character, not used, used
	 * characters into 0020-007E
	 * https://unicode-table.com/en/blocks/basic-latin/
	 * 
	 * Latin-1 Supplement from 0080 until 00FF used characters into 00A1-00FF
	 * https://unicode-table.com/en/blocks/latin-1-supplement/
	 * 
	 */

	private static String characterChecker(String content) {
		String output = "";

		for (int i = 0; i < content.length(); i++) {
			int charvalue = ReadIntegerValueOf1Character(content.charAt(i));

			if ((!((57343 <= charvalue) && (charvalue <= 63742)))
					&& (((3 <= charvalue) && (charvalue <= 125)) || ((160 <= charvalue) && (charvalue <= 254)))) {
				output = output + content.charAt(i);
			}
		}

		output = output.replaceAll("", "");

		output = output.replaceAll("\u0007", "");
		output = output.replaceAll("\u0008", "");
		output = output.replaceAll("\u0009", "    "); // \t
		output = output.replaceAll("\u000B", "");
		output = output.replaceAll("\u000C", "");
		output = output.replaceAll("\\r", "\n");

		return output;
	}

	private static int ReadIntegerValueOf1Character(char c) {
		String y = c + Character.digit(c, 10) + " ";

		y = y.substring(0, y.length() - 1);

		return Integer.parseInt(y);
	}

}