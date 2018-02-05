package de.julielab.jcore.consumer.es.sharedresources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;

public class ListProvider implements IListProvider {

	private List<String> list;
	private Set<String> set;

	@Override
	public void load(DataResource aData) throws ResourceInitializationException {
		try {
			list = new ArrayList<>();
			LineIterator lineIt = IOUtils.lineIterator(new InputStreamReader(aData.getInputStream(), "UTF-8"));
			while (lineIt.hasNext()) {
				String line = lineIt.nextLine();
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				list.add(line.intern());
			}
			lineIt.close();
			((ArrayList<String>) list).trimToSize();
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

	}

	/**
	 * Returns the loaded list of strings where all strings are internalized.
	 */
	@Override
	public List<String> getList() {
		return list;
	}

	/**
	 * Returns the loaded set of strings where all strings are internalized.
	 */
	@Override
	public Set<String> getAsSet() {
		if (null == set)
			set = new HashSet<>(list);
		return set;

	}

}
