package de.julielab.jcore.consumer.es.filter;

import java.util.List;

/**
 * Adds a suffix to filtered terms.
 * @author faessler
 *
 */
public class SuffixAdditionFilter extends AbstractFilter {

	private String addition;

	public SuffixAdditionFilter(String addition) {
		this.addition = addition;
	}

	@Override
	public List<String> filter(String input) {
		newOutput();
		if (null != input) {
			output.add(input + addition);
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new SuffixAdditionFilter(addition);
	}

}
