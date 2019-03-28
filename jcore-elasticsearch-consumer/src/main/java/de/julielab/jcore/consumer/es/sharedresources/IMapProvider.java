package de.julielab.jcore.consumer.es.sharedresources;

import org.apache.uima.resource.SharedResourceObject;

import java.util.Map;

public interface IMapProvider<K,V> extends SharedResourceObject {
	Map<K, V> getMap();
}
