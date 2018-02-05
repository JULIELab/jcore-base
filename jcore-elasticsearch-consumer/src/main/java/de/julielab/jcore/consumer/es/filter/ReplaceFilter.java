package de.julielab.jcore.consumer.es.filter;

import java.util.List;
import java.util.Map;

public class ReplaceFilter extends AbstractFilter {
	private Map<String, String> replacements;

	public ReplaceFilter(Map<String, String> replacements) {
		this.replacements = replacements;
	}

	@Override
	public List<String> filter(String input) {
		newOutput();
		if (null != input) {
			String replacement = replacements.get(input);
			if (null != replacement)
				output.add(replacement);
			else
				output.add(input);
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new ReplaceFilter(replacements);
	}

}
