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
package de.julielab.jcore.reader.bionlpformat.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.Annotation;
import de.julielab.jcore.types.Token;
import de.julielab.jcore.types.ace.Sentence;

public class AnnotationFileMapper_Seg {

	public void mapEventFile(BufferedReader bufferedReader, JCas jcas)
		throws IOException {
		Map<String, Annotation> mappedAnnotations = new HashMap<String, Annotation>();
		mapFile(bufferedReader, jcas);
	}
		
	private void mapFile(BufferedReader bufferedReader, JCas jcas)
			throws IOException {
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			// Entities
			if (line.startsWith("T")) {
				mapEntity(line, jcas);
			}
		}
	}

	private void mapEntity(String entry, JCas jcas) {
		String[] headAndTail = entry.split("\t");
		String id = headAndTail[0];
		String tail = headAndTail[1];
		String[] tokens = tail.split(" ");
		Annotation annotation = null;
		if (tokens[0].equals("Sentence")) {
			Sentence sent = new Sentence(jcas);
			annotation = sent;
		} else if (tokens[0].equals("Token")) {
			Token tok = new Token(jcas);
			annotation = tok;
		}
		
		if (!annotation.equals(null)) {
			annotation.setId(id);
			annotation.setBegin(new Integer(tokens[1]));
			annotation.setEnd(new Integer(tokens[tokens.length-1]));
			annotation.addToIndexes();
		}
	}
}
