package de.julielab.jcore.consumer.es.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UniqueFilter extends AbstractFilter {

	private Set<String> unificationSet;

	public UniqueFilter() {
		this.unificationSet = new HashSet<>();
	}

	@Override
	public List<String> filter(String input) {
		newOutput();
		if (unificationSet.contains(input))
			return output;
		unificationSet.add(input);
		output.add(input);
		return output;
	}

	@Override
	public void reset() {
		super.reset();
		unificationSet.clear();
	}

	@Override
	public Filter copy() {
		return new UniqueFilter();
	}

}
