package de.julielab.jcore.reader.dta.mapping;

import com.google.common.collect.ImmutableMap;

import de.julielab.jcore.types.extensions.dta.DWDS1Belletristik;
import de.julielab.jcore.types.extensions.dta.DWDS1Gebrauchsliteratur;
import de.julielab.jcore.types.extensions.dta.DWDS1Wissenschaft;
import de.julielab.jcore.types.extensions.dta.DWDS1Zeitung;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;

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
