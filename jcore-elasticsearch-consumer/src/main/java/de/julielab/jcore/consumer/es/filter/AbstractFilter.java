package de.julielab.jcore.consumer.es.filter;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFilter implements Filter {
	protected List<String> output;

	public AbstractFilter() {
		newOutput();
	}
	
	protected void newOutput() {
		output = new ArrayList<>();
	}
	

	@Override
	public void reset() {
		output.clear();
	}
}
