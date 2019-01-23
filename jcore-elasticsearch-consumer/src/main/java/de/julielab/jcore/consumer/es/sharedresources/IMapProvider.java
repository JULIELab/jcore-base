package de.julielab.jcore.consumer.es.sharedresources;

import java.util.Map;

import org.apache.uima.resource.SharedResourceObject;

public interface IMapProvider<K,V> extends SharedResourceObject {
	Map<K, V> getMap();
}
