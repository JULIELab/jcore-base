package de.julielab.jcore.reader.dta.mapping;

import com.google.common.collect.ImmutableMap;

import de.julielab.jcore.types.extensions.dta.DTABelletristik;
import de.julielab.jcore.types.extensions.dta.DTAFachtext;
import de.julielab.jcore.types.extensions.dta.DTAGebrauchsliteratur;
import de.julielab.jcore.types.extensions.dta.DTAOther;
import de.julielab.jcore.types.extensions.dta.DocumentClassification;

public class DTAMapper extends AbstractMapper {
  
    DTAMapper() {
          super("dtamain", "dtasub", ImmutableMap
            .<String, Class<? extends DocumentClassification>> builder()
            .put("Belletristik", DTABelletristik.class)
            .put("Fachtext", DTAFachtext.class)
            .put("Gebrauchsliteratur", DTAGebrauchsliteratur.class).build(), DTAOther.class);
    }
}
