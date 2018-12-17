package de.julielab.jcore.consumer.es.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a pipeline of filters where the ith filter is applied to the
 * results of the (i-1)th filter.
 * 
 * @author faessler
 *
 */
public class FilterChain extends AbstractFilter {

	private List<Filter> chain;

	public FilterChain() {
		super();
		this.chain = new ArrayList<Filter>();
	}

	public FilterChain(Filter... filters) {
		this();
		for (int i = 0; i < filters.length; i++) {
			Filter filter = filters[i];
			add(filter);
		}
	}

	public void add(Filter f) {
		if (f == null)
			throw new IllegalArgumentException("null values are not allowed for elements of the filter chain");
		chain.add(f);
	}

	@Override
	public List<String> filter(String input) {
		// the output will be iteratively filtered by all the filters in the
		// chain
		newOutput();
		// add the input as a starting point; the first filter will operate
		// directly on the input value, subsequent filters on the output of the
		// previous filter, respectively
		output.add(input);
		for (Filter f : chain) {
			List<String> filterValues = new ArrayList<>();
			for (String value : output) {
				filterValues.addAll(f.filter(value));
			}
			output = filterValues;
		}
		return output;
	}

	@Override
	public void reset() {
		for (Filter f : chain)
			f.reset();
	}

	@Override
	public Filter copy() {
		FilterChain copy = new FilterChain();
		for (Filter f : chain)
			copy.add(f.copy());
		return copy;
	}
}
