/**
 * In the this package, a range of {@link de.julielab.jcore.utility.index.JCoReAnnotationIndex} implementations
 * can be found.
 * These classes leverage the {@link java.util.Map} and {@link java.util.TreeSet} classes to structure
 * a given set of annotations
 * by some comparator, e.g. start offset comparator. After a first run where a concrete index
 * implementation is filled with the annotations from a CAS, the CAS annotation indices can be left
 * alone and the index can be consulted efficiently for specific annotations. This is used, for example,
 * using the overlap comparator defined in {@link de.julielab.jcore.utility.index.Comparators}.
 * With this comparator, one can find annotations overlapping a given annotation.
 *
 * When using a map annotation index, the map keys are so-called IndexTerms.
 * Consult the {@link de.julielab.jcore.utility.index.TermGenerators} class for predefined index
 * term generators which are used for indexing and for searching. Index terms can be long numbers
 * encoding start and end offsets of annotations, for example, allowing for a very efficient
 * retrieval of overlapping annotations.
 */
package de.julielab.jcore.utility.index;