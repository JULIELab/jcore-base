package de.julielab.jcore.consumer.es.preanalyzed;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;

/**
 * This class corresponds to 'any other token' than a preanalyzed token. This is directly the reason why this class also
 * implements the <tt>IFieldValue</tt> interface: In a normal ElasticSearch JSON document, the values of fields are
 * strings, numbers, dates (represented as strings), arrays of those or inner objects. This class is meant for all of
 * those except the last two, complex, types. To speak of tokens is actually failing the point because for raw
 * ElasticSearch JSON document field values (just like for Lucene field values) are supposed to be the original document
 * value, e.g. the title of the document, which are then broken down to tokens and eventually index terms by analyzers.
 * This exact mechnic is circumvented by the preanalyzed field values, but not by raw values represented by this class.
 * <p>
 * The reason for this class to implement <tt>IToken</tt> is because these raw field values are generated in an
 * algorithmically similar - almost equal - way as preanalyzed tokens are. So this is a technically motivated decision.
 * The difference is however, that preanalyzed tokens have to be grouped to field values and complemented by the
 * original value they have been derived from. The raw values however are interpreted as this original value itself
 * without a pre-analysis. This is why this class also implements the <tt>IFieldValue</tt> interface - because the raw
 * values just are the Lucene field values prior to Lucene-style analysis.
 * </p>
 * 
 * @author faessler
 * 
 */
public class RawToken implements IToken, IFieldValue {
	public Object token;

	public RawToken(Object tokenString) {
		this.token = tokenString;

	}

	@Override
	public FieldType getFieldType() {
		return FieldType.RAW;
	}

	@Override
	public TokenType getTokenType() {
		return TokenType.RAW;
	}

	@Override
	public Object getTokenValue() {
		return token;
	}

	@Override
	public String toString() {
		return token.toString();
	}

	public static class RawTokenGsonAdapter extends TypeAdapter<RawToken> {

		@Override
		public void write(JsonWriter out, RawToken token) throws IOException {
			Object tokenValue = token.token;
			if (tokenValue instanceof String)
				out.value((String) tokenValue);
			else if (tokenValue instanceof Number)
				out.value((Number) tokenValue);
			else if (tokenValue instanceof Boolean)
				out.value((boolean) tokenValue);
			else if (tokenValue == null)
				out.nullValue();
			else
				throw new IllegalArgumentException("The token value class " + tokenValue.getClass()
						+ " (for token with value \""
						+ tokenValue
						+ "\") is currently not supported");
		}

		@Override
		public RawToken read(JsonReader in) throws IOException {
			throw new NotImplementedException(
					"Reading the ElasticSearch JSON format is currently not in the scope of this library.");
		}

	}
}
