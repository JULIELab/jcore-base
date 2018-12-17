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

import de.julielab.jcore.types.extensions.dta.DWDS2Belletristik;
import de.julielab.jcore.types.extensions.dta.DWDS2Gebrauchsliteratur;
import de.julielab.jcore.types.extensions.dta.DWDS2Roman;
import de.julielab.jcore.types.extensions.dta.DWDS2Traktat;
import de.julielab.jcore.types.extensions.dta.DWDS2Wissenschaft;
import de.julielab.jcore.types.extensions.dta.DWDS2Zeitung;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public class DWDS2Mapper extends AbstractMapper {

	DWDS2Mapper() {
		super("dwds2main", "dwds2sub", ImmutableMap
				.<String, Class<? extends DocumentClassification>> builder()
				.put("Wissenschaft", DWDS2Wissenschaft.class)
				.put("Gebrauchsliteratur", DWDS2Gebrauchsliteratur.class)
				.put("Belletristik", DWDS2Belletristik.class)
				.put("Zeitung", DWDS2Zeitung.class)
				.put("Traktat", DWDS2Traktat.class)
				.put("Roman", DWDS2Roman.class).build());
	}
}
