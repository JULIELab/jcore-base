package de.julielab.jcore.reader.dta.mapping

public class DWDS1Mapper extends Mapper {
  
    DWDS1Mapper() {
          super("dwds1main", "dwds1sub", ImmutableMap
            .<String, Class<? extends DocumentClassification>> builder()
                .put("Wissenschaft", DWDS1Wissenschaft.class)
            .put("Gebrauchsliteratur", DWDS1Gebrauchsliteratur.class)
            .put("Belletristik", DWDS1Belletristik.class)
            .put("Zeitung", DWDS1Zeitung.class).build());
    }
}
