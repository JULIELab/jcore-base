package de.julielab.jcore.consumer.entityevaluator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.cas.TOP;

import de.julielab.jcore.utility.JCoReFeaturePath;

public class Column {
	/**
	 * Pattern to check if a string matches the column definition syntax at all.
	 * This expression matches line like
	 * 
	 * <pre>
	 * entityid:Chemical,Gene=/registryNumber;Disease=/specificType
	 * entityid:Chemical,Gene=/registryNumber;
	 * entityid:Disease=/specificType
	 * entityid:/id
	 * entityid:/:getCoveredText()
	 * </pre>
	 * 
	 * but not
	 * 
	 * <pre>
	 * entityid:Gene=
	 * entityid:Chemical,Gene=/registryNumber;Disease=
	 * entityid:Chemical:number
	 * </pre>
	 */
	private static final Pattern fullColumnDefinitionFormat = Pattern.compile(".+:(([^=]+=.[^=;]*;?)+|\\/[^;,=]+)");
	/**
	 * Matches:<br>
	 * <b>Chemical=/registryNumber</b>;<b>Disease=/specificType</b><br>
	 * <b>Chemical,Disease=/registryNumber</b> entityid:Gene=/species
	 * <b>Gene,Organism=/specificType</b>
	 * <b>Gene,Chemical=/specificType</b>;<b>Organism=/id</b>
	 * <b>/value</b>;<b>Gene=specificType</b>
	 * <b>/value</b>
	 * <b>/:getCoveredText()</b>
	 */
	private static final Pattern typeDefinitionFormat = Pattern.compile("([^:;]+=[^;]+|\\/:?[^;]+)");
	/**
	 * Groups type definitions in their elements:<br>
	 * <b>Chemical</b>,<b>Gene</b>=<b>/registryNumber</b>
	 *
	 */
	private static final Pattern typeDefinitionElementsPattern = Pattern.compile("([^,=]+)");
	protected String name;
	protected Map<Type, JCoReFeaturePath> featurePathMap;
	protected JCoReFeaturePath globalFeaturePath;
	/**
	 * @see #typeDefinitionElementsPattern
	 */
	private Matcher melements;
	/**
	 * @see #fullColumnDefinitionFormat
	 */
	private Matcher mfull;
	/**
	 * @see #typeDefinitionFormat
	 */
	private Matcher mtypes;

	@Override
	public String toString() {
		return "Column [name=" + name + ", featurePathMap=" + featurePathMap + "]";
	}

	public Column(Column other) {
		this();
		this.name = other.name;
		this.featurePathMap = other.featurePathMap;
	}

	public Column(String columnDefinition, String typePrefix, TypeSystem ts) throws CASException {
		this();
		parseAndAddDefinition(columnDefinition, typePrefix, ts);
	}

	public Column() {
		mfull = fullColumnDefinitionFormat.matcher("");
		mtypes = typeDefinitionFormat.matcher("");
		melements = typeDefinitionElementsPattern.matcher("");
		featurePathMap = new LinkedHashMap<>();
	}

	public String getName() {
		return name;
	}

	public void parseAndAddDefinition(String columnDefinition, String typePrefix, TypeSystem ts) throws CASException {
		if (!mfull.reset(columnDefinition).matches()) {
			throw new IllegalArgumentException(
					"The line does not obey the column definition syntax: " + columnDefinition);
		}
		name = columnDefinition.split(":", 2)[0];
		mtypes.reset(columnDefinition);
		// find the type=/path expressions
		while (mtypes.find()) {
			String group = mtypes.group();
			melements.reset(group);
			List<String> elements = new ArrayList<>();
			while (melements.find())
				elements.add(melements.group());
			if (elements.size() > 1) {
				for (int i = 0; i < elements.size(); ++i) {
					String element = elements.get(i);
					if (i < elements.size() - 1) {
						String typeName = element.trim();
						Type type = EntityEvaluatorConsumer.findType(typeName, typePrefix, ts);
						JCoReFeaturePath fp = new JCoReFeaturePath();
						fp.initialize(elements.get(elements.size() - 1).trim());
						featurePathMap.put(type, fp);
					}
				}
			} else {
				globalFeaturePath = new JCoReFeaturePath();
				globalFeaturePath.initialize(elements.get(0));
			}
		}
	}

	public Set<Type> getTypes() {
		return featurePathMap.keySet();
	}

	public Type getSingleType() {
		if (featurePathMap.size() > 1)
			throw new IllegalStateException("The column " + name + " has more than one type");
		return featurePathMap.keySet().stream().findFirst().get();
	}

	public String getValue(TOP a) {
		String value = null;
		JCoReFeaturePath fp = getMostSpecificApplicableFeaturePath(a.getType(), a.getCAS().getTypeSystem());
		if (fp != null) {
			value = fp.getValueAsString(a);
		} else if (globalFeaturePath != null) {
			value = globalFeaturePath.getValueAsString(a);
		}
		return value;
	}

	private JCoReFeaturePath getMostSpecificApplicableFeaturePath(Type type, TypeSystem ts) {
		Type ret = type;
		while(featurePathMap.get(ret)==null && ret != null) {
			ret = ts.getParent(ret);
		}
		if (featurePathMap.containsKey(ret)) {
			featurePathMap.put(type, featurePathMap.get(ret));
		}
		return featurePathMap.get(type);
	}
	
	public void reset() {}
}
