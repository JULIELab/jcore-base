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
package de.julielab.jcore.reader.pmc.parser;

public abstract class ParsingResult {

	private int begin;
	private int end;
	private ResultType resultType;

	public enum ResultType {
		ELEMENT, TEXT, NONE
	}

	/**
	 * The begin offset of in bytes this parsing result in the original XML
	 * file. This is not the begin offset in the CAS.
	 * 
	 * @return The XML begin offset associated with this result.
	 */
	public int getBegin() {
		return begin;
	}

	/**
	 * The end offset in bytes of this parsing result in the original XML file.
	 * This is not the end offset in the CAS.
	 * 
	 * @return The XML end offset associated with this result.
	 */
	public int getEnd() {
		return end;
	}

	public ParsingResult(int begin, int end, ResultType resultType) {
		this.begin = begin;
		this.end = end;
		this.resultType = resultType;
	}

	public ResultType getResultType() {
		return resultType;
	}

	public void setResultType(ResultType resultType) {
		this.resultType = resultType;
	}

	/**
	 * Returns a pretty-printed representation of this result and all its sub
	 * results, recursively.
	 * 
	 * @param indentLevel
	 *            The indentation level to start with.
	 * @return A textual representation of this parsing result.
	 */
	public abstract String toString(int indentLevel);

	/**
	 * Returns the text of text nodes of this parsing element and its sub
	 * elements as a single string.
	 * 
	 * @return The element text.
	 */
	public abstract String getResultText();
}
