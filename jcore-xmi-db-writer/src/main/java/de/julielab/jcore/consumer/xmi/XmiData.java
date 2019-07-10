package de.julielab.jcore.consumer.xmi;

/**
 * The minimum of data required to store some portion of XMI data for a document
 * - i.e. the document's ID and the respective data.
 * 
 * @author faessler
 * 
 */
public class XmiData {
	public XmiData(String columnName, DocumentId docId, Object storedData) {
		this.columnName = columnName;
		this.docId = docId;
		data = storedData;
	}

	public String columnName;
	public DocumentId docId;
	public Object data;

	public String getColumnName() {
		return columnName;
	}

	public DocumentId getDocId() {
		return docId;
	}

	public Object getData() {
		return data;
	}
}
