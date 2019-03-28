package de.julielab.jcore.consumer.es.preanalyzed;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;
import java.util.List;

public class PreanalyzedFieldValue implements IFieldValue {
	@SerializedName("v")
	public final String version = "1";
	/**
	 * The stored value of the field, if the field is configured to be stored in
	 * the ElasticSearch mapping. Can be set independently from the actual
	 * tokens, if convenient. This way, the tokens may be generated from one
	 * value but the stored value is another that might be more appropriate or
	 * useful for the application.
	 */
	@SerializedName("str")
	public String fieldString;
	@SerializedName("tokens")
	public List<PreanalyzedToken> tokens;

	public PreanalyzedFieldValue(String fieldString, List<PreanalyzedToken> tokens) {
		super();
		this.fieldString = fieldString;
		this.tokens = tokens;
	}

	public PreanalyzedFieldValue() {
		super();
	}

	@Override
	public FieldType getFieldType() {
		return FieldType.PREANALYZED;
	}

	public static class PreanalyzedFieldValueGsonAdapter extends TypeAdapter<PreanalyzedFieldValue> {
		private Gson gson = new Gson();

		@Override
		public void write(JsonWriter out, PreanalyzedFieldValue value) throws IOException {
			String json = gson.toJson(value);
			out.value(json);
		}

		@Override
		public PreanalyzedFieldValue read(JsonReader in) throws IOException {
			throw new NotImplementedException(
					"Reading the ElasticSearch JSON format is currently not in the scope of this library.");
		}

	}
}
