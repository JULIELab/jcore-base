package de.julielab.jcore.consumer.xmi;

/**
 * The minimum of data required to store some portion of XMI data for a document
 * - i.e. the document's ID and the respective data.
 * 
 * @author faessler
 * 
 */
public class XmiData {
	public XmiData(String docId, Object storedData) {
		this.docId = docId;
		data = storedData;
	}
	public String docId;
	public Object data;
}
