package de.julielab.jcore.consumer.es.sharedresources;

import org.apache.uima.resource.SharedResourceObject;

import java.util.Map;

public interface IAddonTermsProvider extends SharedResourceObject {
	Map<String, String[]> getAddonTerms();
}
