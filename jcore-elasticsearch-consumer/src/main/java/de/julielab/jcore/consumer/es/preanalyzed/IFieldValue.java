package de.julielab.jcore.consumer.es.preanalyzed;

public interface IFieldValue {
	public enum FieldType {PREANALYZED, RAW, OBJECT, ARRAY}
	
	FieldType getFieldType();
}
