package de.julielab.jcore.consumer.es.filter;

import org.tartarus.snowball.SnowballProgram;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class SnowballFilter extends AbstractFilter {

	private SnowballProgram stemmer;

	public SnowballFilter() {
		this("org.tartarus.snowball.ext.EnglishStemmer");
	}

	public SnowballFilter(String snowballProgram) {
		super();
		Class<? extends SnowballProgram> stemClass;
		try {
			stemClass = Class.forName(snowballProgram).asSubclass(SnowballProgram.class);
			stemmer = stemClass.getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
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
