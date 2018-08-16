package de.julielab.jcore.consumer.es.sharedresources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import de.julielab.jcore.utility.JCoReTools;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;

public class MapProvider implements IMapProvider {

	private HashMap<String, String> map;

	@Override
	public void load(DataResource aData) throws ResourceInitializationException {
		BufferedReader br = null;
		try {
			InputStreamReader is;
			try {
				is = new InputStreamReader(JCoReTools.resolveExternalResourceGzipInputStream(aData));
			} catch (Exception e) {
				throw new IOException("Resource " + aData.getUri() + " not found");
			}
			br = new BufferedReader(is);
			map = new HashMap<>();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] split = line.split("\t");
				if (split.length != 2)
					throw new IllegalArgumentException("Format error in map file: Expected format is 'originalValue<tab>mappedValue' but the input line '" + line
							+ "' has " + split.length + " columns.");
				map.put(split[0].trim().intern(), split[1].trim().intern());
			}
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		} finally {
			try {
				if (null != br)
					br.close();
			} catch (IOException e) {
				throw new ResourceInitializationException(e);
			}
		}

	}

	/**
	 * Returns the loaded map. All strings - keys and values - are internalized.
	 */
	@Override
	public Map<String, String> getMap() {
		return map;
	}

}
