package de.julielab.jcore.consumer.es;

import org.apache.uima.UimaContext;

public abstract class FilterBoard {
	protected UimaContext context;

	public void setUimaContext(UimaContext context) {
		this.context = context;
	}
	
	public abstract void setupFilters();
}
