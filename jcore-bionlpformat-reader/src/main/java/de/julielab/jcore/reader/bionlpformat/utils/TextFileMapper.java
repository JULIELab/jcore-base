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

import org.apache.uima.jcas.JCas;

import de.julielab.jcore.types.AbstractText;
import de.julielab.jcore.types.Title;
import de.julielab.jcore.types.pubmed.Header;

public class TextFileMapper {

    public void mapAbstractFile(String id, String source, BufferedReader bufferedReader, JCas cas) throws IOException {
        String title = bufferedReader.readLine();
        String text = "";

        StringBuilder sb = new StringBuilder();
        Boolean addedText = false;
        while (text != null) {
            text = bufferedReader.readLine();
            if (text != null) {
                sb.append(text + " ");
                if (!addedText) {
                    addedText = true;
                }
            }
        }

        text = !addedText ? sb.toString() : sb.toString().substring(0, sb.length() - 1);

        title = title == null ? "" : title;
        text = text == null ? "" : text;
        cas.setDocumentText(title + "\n" + text);

        Title titleAnnotation = new Title(cas);
        titleAnnotation.setBegin(0);
        titleAnnotation.setEnd(title.length());
        titleAnnotation.addToIndexes();

        AbstractText abstractTextAnnotation = new AbstractText(cas);
        abstractTextAnnotation.setBegin(title.length() + 1);
        abstractTextAnnotation.setEnd(title.length() + text.length() + 1);
        abstractTextAnnotation.addToIndexes();

        Header header = new Header(cas);
        header.setBegin(0);
        header.setEnd(text.length() + title.length());
        header.setDocId(id);
        header.setSource(source);
        header.addToIndexes();
    }

}
