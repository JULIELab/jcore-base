package de.julielab.jcore.consumer.es.sharedresources;

import java.util.List;
import java.util.Set;

import org.apache.uima.resource.SharedResourceObject;

public interface IListProvider extends SharedResourceObject {
	List<String> getList();

	Set<String> getAsSet();
}
