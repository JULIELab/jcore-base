package de.julielab.jcore.consumer.es.filter;

import java.util.List;

/**
 * Replaces portions of terms according to the given regular expression and replacement string.
 * @author faessler
 *
 */
public class RegExReplaceFilter extends AbstractFilter {

	private String regex;
	private String replacement;
	private boolean replaceAll;

	public RegExReplaceFilter(String regex, String replacement, boolean replaceAll) {
		this.regex = regex;
		this.replacement = replacement;
		this.replaceAll = replaceAll;
	}
	
	@Override
	public List<String> filter(String input) {
		newOutput();
		if (input != null) {
			output.add(replaceAll ? input.replaceAll(regex, replacement) : input.replaceFirst(regex, replacement));
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new RegExReplaceFilter(regex, replacement, replaceAll);
	}

}
