package de.julielab.jcore.consumer.entityevaluator;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.cas.TOP;

import de.julielab.jcore.utility.JCoReFeaturePath;

public class FeatureValueFilter {
	private static final Pattern fullFilterDefinitionFormat = Pattern.compile("(.+:)?\\/(([^=]+=.[^=;]*;?)+)");

	private Matcher mfull;

	protected PathValuePair globalFeaturePath;
	protected Set<Type> types;
	protected PathValuePair pathValuePair;

	public FeatureValueFilter(String columnDefinition, String typePrefix, TypeSystem ts)  {
		this();
		parseAndAddDefinition(columnDefinition, typePrefix, ts);
	}

	public FeatureValueFilter() {
		mfull = fullFilterDefinitionFormat.matcher("");
	}

	public void parseAndAddDefinition(String filterDefinition, String typePrefix, TypeSystem ts) {
		if (!mfull.reset(filterDefinition).matches()) {
			throw new IllegalArgumentException(
					"The line does not obey the column definition syntax: " + filterDefinition);
		}
		if (filterDefinition.contains(":")) {
			String[] colonSplit = filterDefinition.split(":");
			types = Stream.of(colonSplit[0].split("\\s*,\\s*"))
					.map(typeName -> EntityEvaluatorConsumer.findType(typeName, typePrefix, ts))
					.collect(Collectors.toSet());
			pathValuePair = new PathValuePair(colonSplit[1].split("="));
		} else {
			types = Collections.emptySet();
			pathValuePair = new PathValuePair(filterDefinition.split("="));
		}
	}

	public boolean contradictsFeatureFilter(TOP a) {
		Type type = a.getType();
		if (!types.contains(type) && !types.isEmpty())
			return false;
		String fpValue = pathValuePair.fp.getValueAsString(a);
		return !fpValue.equals(pathValuePair.targetValue);
	}
	
	/**
	 * A pair of a feature path and the value it should have.
	 * 
	 * @author faessler
	 *
	 */
	public static class PathValuePair {
		public PathValuePair(String[] split) {
			try {
				fp = new JCoReFeaturePath();
				fp.initialize(split[0]);
				targetValue = split[1];
			} catch (CASException e) {
				throw new RuntimeException(e);
			}
		}

		public JCoReFeaturePath fp;
		public String targetValue;
	}
}
