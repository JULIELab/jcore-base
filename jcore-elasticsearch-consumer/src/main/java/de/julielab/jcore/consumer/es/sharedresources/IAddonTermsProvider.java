package de.julielab.jcore.consumer.es.sharedresources;

import java.util.Map;

import org.apache.uima.resource.SharedResourceObject;

public interface IAddonTermsProvider extends SharedResourceObject {
	Map<String, String[]> getAddonTerms();
}
