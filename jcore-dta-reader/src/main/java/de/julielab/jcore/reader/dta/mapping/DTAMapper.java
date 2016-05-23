package de.julielab.jcore.reader.dta.mapping

public class DTAMapper extends Mapper {
  
    DTAMapper() {
          super("dtamain", "dtasub", ImmutableMap
            .<String, Class<? extends DocumentClassification>> builder()
            .put("Belletristik", DTABelletristik.class)
            .put("Fachtext", DTAFachtext.class)
            .put("Gebrauchsliteratur", DTAGebrauchsliteratur.class).build());
    }
}
