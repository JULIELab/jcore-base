/**
 * MSdoc2txtReader.java
 * 
 * @author Christina Lohr
 *
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 *
 * Current version: 1.0
 * Since version:   1.0
 *
 * Creation date: 31.03.2017
 * 
 * Here, there are 3 ways to read *.doc-files of MS Word and transform it to plain text.
 * 
 * readDocFileTableAndTransform2TextMarkedTables() with getOutputTextMarkedTables()
 *  * reads a file, can recognize tables and write a table structure by | and 4 space characters
 *  * for using text with the brat rapid annotation tool
 *  
 * readDocFileTableAndTransform2Text()
 *	* reads a file, can recognize tables and remove the table structure 
 *
 * readDocFileAndTransform2HTMLtable()
 *  * reads a file, can recognize tables and write a table structure by the HTML-code <table>...</table>
 *  
 *  We removed non-readable characters, see characterChecker().
 */

package de.julielab.jcore.reader.msdoc2txt.main;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ReadMSdocWithTable {
	static String output = "";
	static String output_withoutlabparams = "";
	static String output_labparams = "";

	public static String getOutputTextMarkedTables() {
		return output;
	}

	public static String getOutput_withoutlabparams() {
		return output_withoutlabparams;
	}

	public static String getOutput_labparams() {
		return output_labparams;
	}

	public static void readDocFileTableAndTransform2TextMarkedTables(String filename) {
		// see
		// http://stackoverflow.com/questions/17062841/read-table-from-docx-file-using-apache-poi

		output = "";
		output_withoutlabparams = "";
		output_labparams = "";

		String content = "";

		try {
			InputStream fis = new FileInputStream(filename);
			POIFSFileSystem fs = new POIFSFileSystem(fis);

			@SuppressWarnings("resource")
			HWPFDocument doc = new HWPFDocument(fs);

			Range range = doc.getRange();
			boolean intable = false;
			boolean inrow = false;

			boolean td_open = false;

			for (int i = 0; i < range.numParagraphs(); i++) {
				Paragraph par = range.getParagraph(i);

				if (par.isInTable()) {
					if (!intable) {
						intable = true;
					}
					if (!inrow) {
						inrow = true;
					}
					if (par.isTableRowEnd()) {
						content = content + "\n";
						inrow = false;
					} else {
						if (td_open == false) {
							if (par.text().endsWith("\u0007")) {
								content = content + "|    " + par.text() + "    |";
							} else {
								td_open = true;
								content = content + "|    " + par.text() + " ";
							}
						} else {
							if (par.text().endsWith("\u0007")) {
								td_open = false;
								content = content + par.text() + "    |";

							} else {
								content = content + par.text() + " ";
							}
						}
					}
				} else {
					if (intable) {
						intable = false;
					}
					content = content + par.text();
				}

			}
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}

		String[] contentSplit = characterChecker(content).split("\n");

		boolean foundLabParams = false;

		for (int i = 0; i < contentSplit.length; i++) {
			if (i + 2 < contentSplit.length) {
				if ((contentSplit[i].contains("Laborwerte")) && (contentSplit[i + 2].contains("Normwert"))) {
					foundLabParams = true;
				}
			}

			if (!foundLabParams) {
				output_withoutlabparams = output_withoutlabparams + contentSplit[i] + "\n";
			} else {
				output_labparams = output_labparams + contentSplit[i] + "\n";
			}
		}
	}

	public static String readDocFileAndTransform2HTMLtable(String filename) {
		// see
		// http://stackoverflow.com/questions/17062841/read-table-from-docx-file-using-apache-poi

		String table = "";

		try {
			InputStream fis = new FileInputStream(filename);
			POIFSFileSystem fs = new POIFSFileSystem(fis);

			@SuppressWarnings("resource")
			HWPFDocument doc = new HWPFDocument(fs);

			Range range = doc.getRange();
			boolean intable = false;
			boolean inrow = false;

			boolean td_open = false;

			for (int i = 0; i < range.numParagraphs(); i++) {
				Paragraph par = range.getParagraph(i);

				if (par.isInTable()) {
					if (!intable) {
						table = table + "<table border='1'>\n";
						intable = true;
					}
					if (!inrow) {
						table = table + "<tr>\n";
						inrow = true;
					}
					if (par.isTableRowEnd()) {
						table = table + "</tr>\n";
						inrow = false;
					} else {
						if (td_open == false) {
							if (par.text().endsWith("\u0007")) {
								table = table + "<td>" + par.text() + "</td>\n";
								// originale Zeile ohne Abfrage aus
								// Steuerzeichen
							} else {
								td_open = true;
								table = table + "<td>" + par.text() + "<br>";
							}
						} else {
							if (par.text().endsWith("\u0007")) {
								td_open = false;
								table = table + par.text() + "</td>\n";

							} else {
								table = table + par.text() + "<br>";
							}
						}
					}
				} else {
					if (intable) {
						table = table + "</table>";
						intable = false;
					}
					table = table + par.text() + "<br/>";
				}

			}
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}

		table = table.replaceAll("\u0007", "");

		return table;
	}
	
	public static String readDocFileTableAndTransform2Text(String filename) {
		// see
		// http://stackoverflow.com/questions/17062841/read-table-from-docx-file-using-apache-poi

		String content = "";

		try {
			InputStream fis = new FileInputStream(filename);
			POIFSFileSystem fs = new POIFSFileSystem(fis);

			@SuppressWarnings("resource")
			HWPFDocument doc = new HWPFDocument(fs);

			Range range = doc.getRange();
			boolean intable = false;
			boolean inrow = false;

			boolean td_open = false;

			for (int i = 0; i < range.numParagraphs(); i++) {
				Paragraph par = range.getParagraph(i);

				if (par.isInTable()) {
					if (!intable) {
						content = content + "\n";
						intable = true;
					}
					if (!inrow) {
						inrow = true;
					}
					if (par.isTableRowEnd()) {
						inrow = false;
					} else {
						if (td_open == false) {
							if (par.text().endsWith("\u0007")) {
								content = content + par.text() + "\n";
							} else {
								td_open = true;
								content = content + par.text();
							}
						} else {
							if (par.text().endsWith("\u0007")) {
								td_open = false;
								content = content + par.text() + "\n";

							} else {
								content = content + par.text();
							}
						}
					}
				} else {
					if (inrow) {
						inrow = false;
					}
					if (intable) {
						intable = false;
					}
					content = content + par.text();
				}

			}
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}

		return characterChecker(content);
	}
	
	/**
	 * MSdocReade don't use all characters.
	 * 
	 * Private Use Area from U+E000 until U+F8FF
	 * https://unicode-table.com/en/blocks/private-use-area/
	 * 
	 * Basic Latin from 0020 until 007F,
	 * 007F is DEL-character, not used,
	 * used characters into 0020-007E  
	 * https://unicode-table.com/en/blocks/basic-latin/
	 * 
	 * Latin-1 Supplement from 0080 until 00FF 
	 * used characters into 00A1-00FF
	 * https://unicode-table.com/en/blocks/latin-1-supplement/
	 * 
	 */
	
	private static String characterChecker(String content)
	{
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
	
	private static int ReadIntegerValueOf1Character(char c)
	{
		String y = c + Character.digit(c, 10) + " ";
		
		y = y.substring(0, y.length() - 1);
		
		return Integer.parseInt(y);
	}
}