package de.julielab.jcore.consumer.es.filter;

import java.util.List;
import java.util.Set;

public class ValidWordFilter extends AbstractFilter {

	private Set<String> validwords;
	private boolean lowerCaseInput;

	public ValidWordFilter(Set<String> validwords, boolean compareWithLowerCase) {
		this.validwords = validwords;
		this.lowerCaseInput = compareWithLowerCase;
	}

	@Override
	public List<String> filter(String input) {
		newOutput();
		if (null != input) {
			String forComparison = lowerCaseInput ? input.toLowerCase() : input;
			if (validwords.contains(forComparison))
				output.add(input);
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new ValidWordFilter(validwords, lowerCaseInput);
	}

}
