package de.julielab.jcore.consumer.es.sharedresources;

import org.apache.uima.resource.SharedResourceObject;

import java.util.List;
import java.util.Set;

public interface IListProvider extends SharedResourceObject {
	List<String> getList();

	Set<String> getAsSet();
}
