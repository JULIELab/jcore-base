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
package de.julielab.jcore.reader.msdoc2txt.main;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ReadSingleMSdoc {

	public static String INPUT_FILE = "";

	public static String CONTENT_HTML = "";
	public static String CONTENT_NORMAL = "";
	public static String CONTENT_TAB_MARKED = "";

	public static String LAB_PARAMS_HTML = "";
	public static String LAB_PARAMS_NORMAL = "";
	public static String LAB_PARAMS_TAB_MARKED = "";

	// WOLP... without lab params
	public static String CONTENT_WOLP_HTML = "";
	public static String CONTENT_WOLP_NORMAL = "";
	public static String CONTENT_WOLP_TAB_MARKED = "";

	public static boolean LAB_TABLE = false;

	public static void doc2Text() {

		CONTENT_HTML = "";
		CONTENT_NORMAL = "";
		CONTENT_TAB_MARKED = "";

		LAB_PARAMS_HTML = "";
		LAB_PARAMS_NORMAL = "";
		LAB_PARAMS_TAB_MARKED = "";

		// WOLP... without lab params
		CONTENT_WOLP_HTML = "";
		CONTENT_WOLP_NORMAL = "";
		CONTENT_WOLP_TAB_MARKED = "";

		try {
			InputStream fis = new FileInputStream(INPUT_FILE);
			POIFSFileSystem fs = new POIFSFileSystem(fis);

			@SuppressWarnings("resource")
			HWPFDocument doc = new HWPFDocument(fs);

			Range docRange = doc.getRange();

			boolean inTable = false;
			boolean inRow = false;

			boolean tbOpen = false;
			boolean tdOpen = false;

			for (int i = 0; i < docRange.numParagraphs(); i++) {
				Paragraph par = docRange.getParagraph(i);

				if (par.isInTable()) {
					if (!inTable) {
						CONTENT_HTML += "\n<table>\n";
						inTable = true;
					}
					if (!inRow) {
						if (tbOpen == true) {
							CONTENT_HTML += "</tr>\n";
						}

						CONTENT_HTML += "<tr>";
						inRow = true;
						tbOpen = true;
					}
					if (par.isTableRowEnd()) {
						inRow = false;
					} else {
						if ((!tdOpen) && (par.text().endsWith("\u0007"))) {
							CONTENT_HTML += "<td>";
							CONTENT_HTML += par.text().replaceAll("\\s", " ");
							CONTENT_HTML += "</td>";
						} else if ((!tdOpen) && (!(par.text().endsWith("\u0007")))) {
							CONTENT_HTML += "<td>";
							CONTENT_HTML += par.text().replaceAll("\\s", " ");
							tdOpen = true;
						}

						else if (tdOpen && (par.text().endsWith("\u0007"))) {
							CONTENT_HTML += par.text().replaceAll("\\s", " ");
							CONTENT_HTML += "</td>";
							tdOpen = false;
						} else if (tdOpen && (!(par.text().endsWith("\u0007")))) {
							CONTENT_HTML += par.text().replaceAll("\\s", " ");
						}
					}
				} else {
					if (inTable) {
						CONTENT_HTML += "</tr>\n</table>\n";
						inTable = false;
					}
					CONTENT_HTML += par.text() + "<br/>";
				}
			}
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}

		CONTENT_HTML = characterChecker(CONTENT_HTML);

		CONTENT_NORMAL = makeNormalText(CONTENT_HTML);
		CONTENT_TAB_MARKED = makeTabMarked(CONTENT_HTML);

		// CONTENT_TEXT_ONLY = characterChecker(CONTENT_TEXT_ONLY);

		String[] contentSplitMT = characterChecker(CONTENT_HTML).split("\n");
		// String[] contentSplitNMT =
		// characterChecker(CONTENT_TEXT_ONLY).split("\n");

		boolean foundLabParams = false;

		for (int i = 0; i < contentSplitMT.length; i++) {

			if (contentSplitMT[i].contains("<br/>Laborwerte:")) {
				for (int j = i; j < contentSplitMT.length; j++) {
					if (contentSplitMT[j].contains("Normwert")) {
						foundLabParams = true;
						break;
					}
				}
			}

			if (!foundLabParams) {
				CONTENT_WOLP_HTML = CONTENT_WOLP_HTML + contentSplitMT[i] + "\n";
			} else {
				LAB_PARAMS_HTML = LAB_PARAMS_HTML + contentSplitMT[i] + "\n";
			}
		}

		CONTENT_WOLP_NORMAL = makeNormalText(CONTENT_WOLP_HTML);
		CONTENT_WOLP_TAB_MARKED = makeTabMarked(CONTENT_WOLP_HTML);

		LAB_PARAMS_NORMAL = makeNormalText(LAB_PARAMS_HTML);
		LAB_PARAMS_TAB_MARKED = makeTabMarked(LAB_PARAMS_HTML);

		if ((LAB_PARAMS_HTML.contains("<table>")) && (LAB_PARAMS_HTML.contains("</table>"))
				&& (LAB_PARAMS_HTML.contains("<tr>")) && (LAB_PARAMS_HTML.contains("</tr>"))) {
			LAB_TABLE = true;
		} else {
			LAB_TABLE = false;
		}
	}

	/**
	 * The Reader doesn't use all characters.
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

	private static String makeNormalText(String input) {
		String text = input;
		text = text.replaceAll("<table>", "");
		text = text.replaceAll("</table>", "");
		text = text.replaceAll("<tr>", "");
		text = text.replaceAll("</tr>", "\n");
		text = text.replaceAll("<td>", "");
		text = text.replaceAll("</td>", "\t");
		text = text.replaceAll("<br/>", "");
		text = text.replaceAll("\n+", "\n");

		if (text.startsWith("\n")) {
			text = text.replaceFirst("\n", "");
		}

		return text;
	}

	private static String makeTabMarked(String input) {
		String text = input;
		text = text.replaceAll("<table>", "");
		text = text.replaceAll("</table>", "");
		text = text.replaceAll("<tr>", "");
		text = text.replaceAll("</tr>", "");
		text = text.replaceAll("<td>", "|    ");
		text = text.replaceAll("</td>", "    |");
		text = text.replaceAll("<br/>", "");
		text = text.replaceAll("\n+", "\n");

		if (text.startsWith("\n")) {
			text = text.replaceFirst("\n", "");
		}

		return text;
	}
}
