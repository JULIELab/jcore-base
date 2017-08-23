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
package de.julielab.jcore.consumer.bionlpformat.utils;

import java.io.IOException;
import java.io.Writer;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Title;

public class DocumentWriter {

	private Writer writer;

	public DocumentWriter(Writer writer) {
		this.writer = writer;
	}

	public void write(JCas cas) throws IOException {
//		Type titleType = cas.getTypeSystem().getType(Title.class.getCanonicalName());
//		Type abstractTextType = cas.getTypeSystem().getType(AbstractText.class.getCanonicalName());
//		
//		Title title = (Title) cas.getAnnotationIndex(titleType).iterator().next();
//		AbstractText abstractText = (AbstractText) cas.getAnnotationIndex(abstractTextType).iterator().next();
		String documentText = cas.getDocumentText();
//		
//		writer.write(documentText.substring(title.getBegin(), title.getEnd())+"\n");
//		writer.write(documentText.substring(abstractText.getBegin(), abstractText.getEnd())+"\n");
		writer.write(documentText + "\n");
	}

	public void close() throws IOException {
		writer.close();
	}

}
