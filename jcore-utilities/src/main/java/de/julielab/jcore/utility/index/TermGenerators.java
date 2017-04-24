package de.julielab.jcore.utility.index;

import java.util.stream.IntStream;
import java.util.stream.Stream;

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
	 * terms are always of length n. Annotations shorter than n will not
	 * return any terms.
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
	 * Generates as a search term the prefix of the covered text of an
	 * annotation of length <tt>length</tt>. If the annotation is shorter
	 * than <tt>length</tt> no terms are generated.
	 * 
	 * @param length
	 *            The prefix length.
	 * @return The annotation text prefix of length <tt>length</tt>
	 */
	public static JCoReMapAnnotationIndex.IndexTermGenerator<String> prefixTermGenerator(int length) {
		return a -> {
			String text = a.getCoveredText();
			if (text.length() >= length)
				return Stream.of(text.substring(0, length));
			return Stream.empty();
		};
	}

	/**
	 * Generates as a search term the suffix of the covered text of an
	 * annotation of length <tt>length</tt>. If the annotation is shorter
	 * than <tt>length</tt> no terms are generated.
	 * 
	 * @param length
	 *            The suffix length.
	 * @return The annotation text suffix of length <tt>length</tt>
	 */
	public static JCoReMapAnnotationIndex.IndexTermGenerator<String> suffixTermGenerator(int length) {
		return a -> {
			String text = a.getCoveredText();
			if (text.length() >= length)
				return Stream.of(text.substring(text.length() - length, text.length()));
			return Stream.empty();
		};
	}
}