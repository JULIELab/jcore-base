package de.julielab.jcore.consumer.es.filter;

import java.util.List;

import com.ibm.icu.text.Transliterator;

public class LatinTransliterationFilter extends AbstractFilter {

	private Transliterator transliterator;
	private boolean outputOriginalInput;

	/**
	 * 
	 * @param outputOriginalInput If set to <tt>true</tt>, the original, untransliterated input is also returned by the filter. This is useful for indexing both forms so either can be searched.
	 */
	public LatinTransliterationFilter(boolean outputOriginalInput) {
		this.outputOriginalInput = outputOriginalInput;
		transliterator = Transliterator.getInstance("Any-Latin");
	}
	
	@Override
	public List<String> filter(String input) {
		newOutput();
		if (null != input) {
			String transform = transliterator.transform(input);
			output.add(transform);
			if (outputOriginalInput && !input.equals(transform))
				output.add(input);
				
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new LatinTransliterationFilter(outputOriginalInput);
	}

}
