/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.reader.dta.mapping;

import com.google.common.collect.ImmutableMap;
import de.julielab.jcore.types.extensions.dta.*;

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
