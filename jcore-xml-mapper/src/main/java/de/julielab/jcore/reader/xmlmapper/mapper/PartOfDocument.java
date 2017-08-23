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
package de.julielab.jcore.reader.xmlmapper.mapper;

public class PartOfDocument {
	private String xPath;
	private int[] begin;
	private int[] end;
	private String[] text;
	private int id;
	// If set, this parser is used to get the text of the text part pointed to
	// by xPath. The parser might also set additional annotations internally.
	private DocumentTextPartParser parser;

	public DocumentTextPartParser getParser() {
		return parser;
	}

	public PartOfDocument(int id) {
		this.id = id;
	}

	public String getXPath() {
		return xPath;
	}

	public void setxPath(String xPath) {
		this.xPath = xPath;
	}

	/**
	 * Returns the very beginning of the document part, i.e. the first begin of
	 * the first substructure (if any).
	 * 
	 * @return
	 */
	public int getBegin() {
		if (begin.length > 0)
			return begin[0];
		return 0;
	}

	public int[] getBeginOffsets() {
		return begin;
	}

	public void setBeginOffsets(int[] begin) {
		this.begin = begin;
	}

	/**
	 * Returns the very end of the document part, i.e. the last end of the last
	 * substructure (if any).
	 * 
	 * @return
	 */
	public int getEnd() {
		if (end.length > 0)
			return end[end.length - 1];
		return 0;
	}

	public int[] getEndOffsets() {
		return end;
	}

	public void setEndOffsets(int[] end) {
		this.end = end;
	}

	public String[] getText() {
		return text;
	}

	public void setText(String[] text) {
		this.text = text;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setParser(DocumentTextPartParser parser) {
		this.parser = parser;
	}


}
