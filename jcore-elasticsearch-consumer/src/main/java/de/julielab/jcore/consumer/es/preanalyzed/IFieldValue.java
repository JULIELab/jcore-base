package de.julielab.jcore.consumer.es.preanalyzed;

public interface IFieldValue {
	enum FieldType {PREANALYZED, RAW, OBJECT, ARRAY}
	
	FieldType getFieldType();
}
