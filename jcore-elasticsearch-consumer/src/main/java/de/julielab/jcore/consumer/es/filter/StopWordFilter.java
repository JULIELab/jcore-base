package de.julielab.jcore.consumer.es.filter;

import java.util.List;
import java.util.Set;

public class StopWordFilter extends AbstractFilter {

	private Set<String> stopwords;
	private boolean lowerCaseInput;

	public StopWordFilter(Set<String> stopwords, boolean compareWithLowerCase) {
		this.stopwords = stopwords;
		this.lowerCaseInput = compareWithLowerCase;
	}

	@Override
	public List<String> filter(String input) {
		newOutput();
		if (null != input) {
			String forComparison = lowerCaseInput ? input.toLowerCase() : input;
			if (!stopwords.contains(forComparison))
				output.add(input);
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new StopWordFilter(stopwords, lowerCaseInput);
	}

}
