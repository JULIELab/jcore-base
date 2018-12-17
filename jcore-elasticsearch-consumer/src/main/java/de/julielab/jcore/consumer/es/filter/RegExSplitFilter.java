package de.julielab.jcore.consumer.es.filter;

import java.util.List;

/**
 * Splits filtered tokens on occurrences of the specified regular expression.
 * @author faessler
 *
 */
public class RegExSplitFilter extends AbstractFilter {

	private String splitRegEx;

	public RegExSplitFilter(String splitRegEx) {
		this.splitRegEx = splitRegEx;
	}
	
	@Override
	public List<String> filter(String input) {
		newOutput();
		if (input != null) {
			String[] split = input.split(splitRegEx);
			for (int i = 0; i < split.length; i++) {
				String string = split[i];
				output.add(string);
			}
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new RegExSplitFilter(splitRegEx);
	}

}
