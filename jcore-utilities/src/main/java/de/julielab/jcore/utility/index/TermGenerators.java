package de.julielab.jcore.utility.index;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.uima.cas.CASException;

/**
 * This class offers a range of predefined term generators (to be used as a
 * constructor argument to {@link JCoReMapAnnotationIndex} that might be useful
 * in a range of applications.
 * 
 * @author faessler
 *
 */
public class TermGenerators {
	/**
	 * Creates strict n-grams of the covered text of an annotation. Returned
	 * terms are always of length n. Annotations shorter than n will not return
	 * any terms.
	 * 
	 * @param n
	 *            The n-gram size.
	 * @return The n-gram index terms.
	 */
	public static JCoReMapAnnotationIndex.IndexTermGenerator<String> nGramTermGenerator(int n) {
		return a -> {
			String text = a.getCoveredText();
			return IntStream.range(0, text.length()).mapToObj(i -> {
				if (i + n <= text.length())
					return text.substring(i, i + n);
				return null;
			}).filter(s -> s != null);
		};
	}

	/**
	 * Generates all prefixes between length of 1 and length of max(n,
	 * annotation.getCoveredText().length()) for an annotation a.
	 * 
	 * @param n
	 *            The maximum prefix length.
	 * @return An index generated generating edge n-grams to a maxmimum length
	 *         of n.
	 */
	public static JCoReMapAnnotationIndex.IndexTermGenerator<String> edgeNGramTermGenerator(int n) {
		return a -> {
			String text = a.getCoveredText();
			return IntStream.range(0, n).mapToObj(i -> {
				if (i < text.length())
					return text.substring(0, i + 1);
				return null;
			}).filter(s -> s != null);
		};
	}

	/**
	 * Generates as a search term the prefix of the covered text of an
	 * annotation up to length <tt>length</tt>. If the annotation is shorter
	 * than <tt>length</tt> the whole annotation text is returned.
	 * 
	 * @param maxLength
	 *            The maximum prefix length.
	 * @return The annotation text prefix of maximum length <tt>length</tt>
	 */
	public static JCoReMapAnnotationIndex.IndexTermGenerator<String> prefixTermGenerator(int maxLength) {
		return a -> {
			try {
				String documentText = a.getCAS().getJCas().getDocumentText();
				return Stream.of(documentText.substring(a.getBegin(),
						a.getBegin() + Math.min(maxLength, a.getEnd() - a.getBegin())));
			} catch (CASException e) {
				e.printStackTrace();
			}
			return Stream.empty();
		};
	}

	/**
	 * Generates as a search term the suffix of the covered text of an
	 * annotation up to length <tt>length</tt>. If the annotation is shorter
	 * than <tt>length</tt> the whole annotation text is returned.
	 * 
	 * @param maxLength
	 *            The maximum suffix length.
	 * @return The annotation text suffix of maximum length <tt>length</tt>
	 */
	public static JCoReMapAnnotationIndex.IndexTermGenerator<String> suffixTermGenerator(int maxLength) {
		return a -> {
			try {
				String documentText = a.getCAS().getJCas().getDocumentText();
				return Stream.of(documentText.substring(Math.max(a.getEnd() - maxLength, a.getBegin()), a.getEnd()));
			} catch (CASException e) {
				e.printStackTrace();
			}
			return Stream.empty();
		};
	}

	/**
	 * Generates as a search term the prefix of the covered text of an
	 * annotation of length <tt>length</tt>. If the annotation is shorter than
	 * <tt>length</tt> no terms are generated.
	 * 
	 * @param length
	 *            The prefix length.
	 * @return The annotation text prefix of length <tt>length</tt>
	 */
	public static JCoReMapAnnotationIndex.IndexTermGenerator<String> exactPrefixTermGenerator(int length) {
		return a -> {
			try {
				String documentText = a.getCAS().getJCas().getDocumentText();
				if (a.getEnd() - a.getBegin() >= length)
					return Stream.of(documentText.substring(a.getBegin(), a.getBegin() + length));
			} catch (CASException e) {
				e.printStackTrace();
			}
			return Stream.empty();
		};
	}

	/**
	 * Generates as a search term the suffix of the covered text of an
	 * annotation of length <tt>length</tt>. If the annotation is shorter than
	 * <tt>length</tt> no terms are generated.
	 * 
	 * @param length
	 *            The suffix length.
	 * @return The annotation text suffix of length <tt>length</tt>
	 */
	public static JCoReMapAnnotationIndex.IndexTermGenerator<String> exactSuffixTermGenerator(int length) {
		return a -> {
			try {
				String documentText = a.getCAS().getJCas().getDocumentText();
				if (a.getEnd() - a.getBegin() >= length)
					return Stream.of(documentText.substring(Math.max(a.getEnd() - length, a.getBegin()), a.getEnd()));
			} catch (CASException e) {
				e.printStackTrace();
			}
			return Stream.empty();
		};
	}
}