package de.julielab.jcore.consumer.es.preanalyzed;

import de.julielab.jcore.consumer.es.ArrayFieldValue;

import java.util.HashMap;
import java.util.List;

public class Document extends HashMap<String, IFieldValue> implements
		IFieldValue {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6036140833025381237L;
	private String id;
	private String parentId;
	private String index;

	public Document() {}
	
	public Document(String id) {
		this.id = id;
	}

	@Override
	public FieldType getFieldType() {
		return FieldType.OBJECT;
	}

	/**
	 * Calls the underlying <tt>put()</tt> method to add the field data to the
	 * document map. Does not create a field for <tt>null</tt> values or empty
	 * array values.
	 * 
	 * @param fieldname
	 * @param value
	 */
	public void addField(String fieldname, IFieldValue value) {
		if (null == value)
			return;
		if (value instanceof ArrayFieldValue) {
			ArrayFieldValue array = (ArrayFieldValue) value;
			if (array.isEmpty())
				return;
		}
		put(fieldname, value);
	}

	/**
	 * Calls the underlying <tt>put()</tt> method to add the field data to the
	 * document map. Does not create a field for <tt>null</tt> values or emtpy
	 * array values.
	 * <p>
	 * If <tt>value</tt> is not an <tt>IFieldValue</tt>, the new field will be
	 * created using a single {@link RawToken} for <tt>value</tt>. Delegates to
	 * {@link #addField(String, IFieldValue)} if <tt>value</tt> is an
	 * <tt>IFieldValue</tt>.
	 * </p>
	 * 
	 * @param fieldname
	 * @param value
	 */
	public void addField(String fieldname, Object value) {
		if (null == value)
			return;
		if (IFieldValue.class.isAssignableFrom(value.getClass())) {
			addField(fieldname, (IFieldValue) value);
			return;
		} else if (value.getClass().isArray()) {
			addField(fieldname, new ArrayFieldValue((Object[]) value));
			return;
        } else if (value instanceof List) {
            addField(fieldname, new ArrayFieldValue(((List<?>) value).toArray()));
            return;
        }
		put(fieldname, new RawToken(value));
	}

	/**
	 * The index ID of this document. May be null. Is required for root-level documents for the indexing.
	 * @return
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String type) {
		this.index = type;
	}
}
