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

public class DWDS1Mapper extends AbstractMapper {

	DWDS1Mapper() {
		super("dwds1main", "dwds1sub", ImmutableMap
				.<String, Class<? extends DocumentClassification>> builder()
				.put("Wissenschaft", DWDS1Wissenschaft.class)
				.put("Gebrauchsliteratur", DWDS1Gebrauchsliteratur.class)
				.put("Belletristik", DWDS1Belletristik.class)
				.put("Zeitung", DWDS1Zeitung.class).build());
	}
}
