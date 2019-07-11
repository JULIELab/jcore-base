package de.julielab.jcore.consumer.xmi;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An extension of {@link XmiData} for full documents or the XMI base document.
 * It holds the current xmi ID that may be given to the next annotation and the
 * map of the document's sofa IDs to the sofa names (so new annotations can
 * reference this original sofa, even if the sofa in their in-memory CAS has a
 * different xmi ID).
 * 
 * @author faessler
 * 
 */
public class DocumentXmiData extends XmiData {
	public String serializedSofaXmiIdMap;

	public DocumentXmiData(String columnName, DocumentId docId, Object storedData, Map<Integer, String> currentSofaXmiIdMap) {
		super(columnName, docId, storedData);
		setSofaXmiIdMap(currentSofaXmiIdMap);
	}

	/**
	 * The map is expected in the form of mappings sofaXmiId=sofaID. Multiple
	 * mappings are separated using a pipe symbol.
	 * 
	 * @param map
	 */
	public void setSofaXmiIdMap(Map<Integer, String> map) {
		if (map == null)
			return;
		List<String> mappings = new ArrayList<>();
		for (Entry<Integer, String> e : map.entrySet())
			mappings.add(String.valueOf(e.getKey()) + "=" + e.getValue());
		serializedSofaXmiIdMap = StringUtils.join(mappings, "|");
	}
}
