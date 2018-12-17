package de.julielab.jcore.consumer.es.sharedresources;

import java.util.Map;

import org.apache.uima.resource.SharedResourceObject;

public interface IMapProvider extends SharedResourceObject {
	Map<String, String> getMap();
}
