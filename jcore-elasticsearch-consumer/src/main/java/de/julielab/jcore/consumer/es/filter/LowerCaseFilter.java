package de.julielab.jcore.consumer.es.filter;

import java.util.List;

public class LowerCaseFilter extends AbstractFilter {

	@Override
	public List<String> filter(String input) {
		newOutput();
		if (input != null)
			output.add(input.toLowerCase());
		return output;
	}

	@Override
	public Filter copy() {
		return new LowerCaseFilter();
	}

}
