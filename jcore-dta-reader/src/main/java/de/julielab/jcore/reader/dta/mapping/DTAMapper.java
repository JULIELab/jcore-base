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
package de.julielab.jcore.reader.dta.mapping;

import com.google.common.collect.ImmutableMap;

import de.julielab.jcore.types.extensions.dta.DTABelletristik;
import de.julielab.jcore.types.extensions.dta.DTAFachtext;
import de.julielab.jcore.types.extensions.dta.DTAGebrauchsliteratur;
import de.julielab.jcore.types.extensions.dta.DTAOther;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public class DTAMapper extends AbstractMapper {

	DTAMapper() {
		super(
				"dtamain",
				"dtasub",
				ImmutableMap
						.<String, Class<? extends DocumentClassification>> builder()
						.put("Belletristik", DTABelletristik.class)
						.put("Fachtext", DTAFachtext.class)
						.put("Gebrauchsliteratur", DTAGebrauchsliteratur.class)
						.build(), DTAOther.class);
	}
}
