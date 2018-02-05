package de.julielab.jcore.consumer.es.filter;

import java.util.List;

public interface Filter {
	List<String> filter(String input);
	void reset();
	Filter copy();
}
