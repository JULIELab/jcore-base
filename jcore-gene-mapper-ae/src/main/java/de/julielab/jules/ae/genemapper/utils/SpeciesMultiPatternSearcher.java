package de.julielab.jules.ae.genemapper.utils;

import com.fulmicoton.multiregexp.MultiPattern;
import com.fulmicoton.multiregexp.MultiPatternSearcher;

public class SpeciesMultiPatternSearcher{

	public static MultiPatternSearcher searcher = MultiPattern.of(
            "yeast ",
            "mammalian ",
            "Drosophila (melanogaster )?",
            "mouse ",
            "murine ", 
            "human ",
            "Saccharomyces (cerevisiae )?",
            "E. coli ",
            "Escherichia coli",
            "Arabidopsis (thaliana )?",
            "Tetrahymena ",
            "eukariotic ",
            "rabbit ",
            "goat ",
            "rat ",
            "Caenorhabditis (elegans )?"
    ).searcher();
}
