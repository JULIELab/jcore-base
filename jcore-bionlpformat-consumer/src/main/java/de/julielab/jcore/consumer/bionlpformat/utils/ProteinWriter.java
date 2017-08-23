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
import java.util.HashSet;
import java.util.Set;

import de.julielab.jcore.types.Gene;

public class ProteinWriter {

    private Writer writer;
    private String documentText;
    private Set<String> writtenIds;

    public Writer getFileWriter() {
        return writer;
    }

    public void setFileWriter(Writer writer) {
        this.writer = writer;
    }

    public ProteinWriter(Writer writer, String documentText) {
        super();
        this.writer = writer;
        this.documentText = documentText;
        this.writtenIds = new HashSet<String>();
    }

    public void writeProtein(Gene protein) throws IOException {
        String id = protein.getId();
        if (!writtenIds.contains(id)) {
            writtenIds.add(id);
        }

        String line = protein.getId() + "\tProtein " + protein.getBegin() + " " + protein.getEnd() + "\t"
                + documentText.substring(protein.getBegin(), protein.getEnd()) + "\n";

        writer.write(line);
    }

    public void close() throws IOException {
        writer.close();
    }

    public boolean isWritten(Gene protein) {
        return writtenIds.contains(protein.getId());
    }

}
