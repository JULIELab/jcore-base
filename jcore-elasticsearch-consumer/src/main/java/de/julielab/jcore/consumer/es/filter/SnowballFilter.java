package de.julielab.jcore.consumer.es.filter;

import org.tartarus.snowball.SnowballProgram;

import java.util.List;

public class SnowballFilter extends AbstractFilter {

	private SnowballProgram stemmer;

	public SnowballFilter() {
		super();
		Class<? extends SnowballProgram> stemClass;
		try {
			stemClass = Class.forName("org.tartarus.snowball.ext.EnglishStemmer").asSubclass(SnowballProgram.class);
			stemmer = stemClass.newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> filter(String input) {
		newOutput();
		if (null != input) {
			stemmer.setCurrent(input);
			stemmer.stem();
			output.add(stemmer.getCurrent());
		}
		return output;
	}

	@Override
	public Filter copy() {
		return new SnowballFilter();
	}

}
