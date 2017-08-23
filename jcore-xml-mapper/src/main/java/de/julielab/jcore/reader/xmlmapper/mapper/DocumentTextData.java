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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DocumentTextData{
	private String text;
	private HashMap<Integer, PartOfDocument> parts;
	Logger LOGGER = LoggerFactory.getLogger(DocumentTextData.class);
	
	public DocumentTextData() {
		parts = new HashMap<Integer, PartOfDocument>(); 
	}
	
	public void put(int id,PartOfDocument part){
		parts.put(id,part);
	}
	
	public String getText(){
		return text;
	}

	public PartOfDocument get(int id) {
		return parts.get(id);
	}

	public void setText(String text) {
		this.text=text;
	}

	public HashMap<Integer, PartOfDocument> getParts() {
		return parts;
	}

	public int size() {
		return this.parts.size();
	}
	
}
