package de.julielab.jcore.consumer.es.filter;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.AttributeFactory;

public class LuceneStandardTokenizerFilter extends AbstractFilter {

	@Override
	public List<String> filter(String input) {
		newOutput();
		try (Tokenizer t = new StandardTokenizer(AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY)) {
			t.setReader(new StringReader(input));
			t.reset();
			CharTermAttribute att = t.addAttribute(CharTermAttribute.class);
			while (t.incrementToken()) {
				output.add(att.toString());
			}
			return output;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Filter copy() {
		return new LuceneStandardTokenizerFilter();
	}

}
