package banner.tagging.dictionary;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;

import banner.types.EntityType;

public class UMLSMetathesaurusDictionaryTagger extends DictionaryTagger
{

	public UMLSMetathesaurusDictionaryTagger()
	{
		super();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void load(HierarchicalConfiguration config) throws IOException
	{
		HierarchicalConfiguration localConfig = config.configurationAt(this.getClass().getName());
		String semanticTypesFilename = localConfig.getString("semanticTypesFile");

		Map<String, EntityType> typeMap = null;
		int maxIndex = localConfig.getMaxIndex("types");
		if (maxIndex >= 0)
			typeMap = new HashMap<String, EntityType>();
		for (int i = 0; i <= maxIndex; i++)
		{
			Set<String> typeNames = new HashSet<String>(localConfig.getList("types(" + i + ").name"));
			String mappedTypeName = localConfig.getString("types(" + i + ").mapTo");
			EntityType mappedType = null;
			if (mappedTypeName != null)
			{
				mappedType = EntityType.getType(mappedTypeName);
				for (String typeName : typeNames)
					typeMap.put(typeName, mappedType);
			}
			else
			{
				for (String typeName : typeNames)
					typeMap.put(typeName, EntityType.getType(typeName));
			}
		}
		// for (String typeName : typeMap.keySet())
		// System.out.println("Type name \"" + typeName + "\" becomes \"" + typeMap.get(typeName).getText() + "\"");

		Set<String> allowedLang = null;
		if (localConfig.containsKey("allowedLang"))
			allowedLang = new HashSet<String>(localConfig.getList("allowedLang"));

		Set<String> allowedPref = null;
		if (localConfig.containsKey("allowedPref"))
			allowedPref = new HashSet<String>(localConfig.getList("allowedPref"));

		Set<String> allowedSupp = null;
		if (localConfig.containsKey("allowedSupp"))
			allowedSupp = new HashSet<String>(localConfig.getList("allowedSupp"));

		Map<String, Set<EntityType>> cuiToTypeMap = loadTypes(semanticTypesFilename, typeMap);
		String conceptNamesFilename = localConfig.getString("conceptNamesFile");
		loadConcepts(conceptNamesFilename, cuiToTypeMap, allowedLang, allowedPref, allowedSupp);
	}

	/**
	 * Reads in the CUI / semantic type file
	 * 
	 * @param semanticTypesFilename
	 * @return Mapping from CUI to the semantic type
	 */
	private Map<String, Set<EntityType>> loadTypes(String semanticTypesFilename, Map<String, EntityType> typeMap) throws IOException
	{
		// TODO Determine if the UMLS type structure has a (useful) hierarchy
		Map<String, Set<EntityType>> cuiToTypeMap = new HashMap<String, Set<EntityType>>();
		BufferedReader reader = new BufferedReader(new FileReader(semanticTypesFilename));
		String line = reader.readLine();
		int lineNum = 0;
		LineFieldParser parser = new LineFieldParser();
		while (line != null)
		{
			parser.init(line);
			String CUI = parser.getField(0);
			String semanticType = parser.getField(3);
			if (typeMap == null || typeMap.containsKey(semanticType))
			{
				Set<EntityType> types = cuiToTypeMap.get(CUI);
				if (types == null)
				{
					types = new HashSet<EntityType>(1);
					cuiToTypeMap.put(CUI, types);
				}
				EntityType type = null;
				if (typeMap == null)
					type = EntityType.getType(semanticType);
				else
					type = typeMap.get(semanticType);
				types.add(type);
			}
			line = reader.readLine();
			lineNum++;
			if (lineNum % 100000 == 0)
			{
				System.out.println("loadTypes() Line: " + lineNum + " Entries: " + cuiToTypeMap.size() + " types: " + EntityType.getTypes().size());
			}
		}
		return cuiToTypeMap;
	}

	private void loadConcepts(String conceptNamesFilename, Map<String, Set<EntityType>> cuiToTypeMap, Set<String> allowedLang, Set<String> allowedPref, Set<String> allowedSupp)
			throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(conceptNamesFilename));
		String line = reader.readLine();
		int lineNum = 0;
		LineFieldParser parser = new LineFieldParser();
		while (line != null)
		{
			parser.init(line);

			Set<EntityType> types = cuiToTypeMap.get(parser.getField(0)); // CUI
			boolean add = types != null;
			add &= (allowedLang == null) || (allowedLang.contains(parser.getField(1))); // Language
			add &= (allowedPref == null) || (allowedPref.contains(parser.getField(6))); // Preferred
			String name = parser.getField(14);
			add &= (allowedSupp == null) || (allowedSupp.contains(parser.getField(16))); // Suppressed

			if (add)
			{
				// List<String> tokens = process(name);
				// List<String> tokens2 = new ArrayList<String>();
				// for (String token : tokens)
				// {
				// if (token.matches(("^[A-Za-z0-9]*$")))
				// tokens2.add(token);
				// }
				// add(tokens2, types);
				add(name, types);
			}
			line = reader.readLine();
			lineNum++;
			if (lineNum % 100000 == 0)
			{
				System.out.println("loadConcepts() Line: " + lineNum + " Entries: " + size() + " types: " + EntityType.getTypes().size());
			}
		}
	}

	private static class LineFieldParser
	{
		// Using this class is much faster than using a call to string.split()
		private String line;
		private int currentField;
		private int beginIndex;
		private int endIndex;

		public LineFieldParser()
		{
			// Empty
		}

		public void init(String line)
		{
			this.line = line;
			currentField = 0;
			beginIndex = 0;
			endIndex = line.indexOf("|", beginIndex);
		}

		public String getField(int field)
		{
			if (field < currentField)
				throw new IllegalStateException("Cannot request a field lower than current field");
			while (currentField < field)
				advance();
			return line.substring(beginIndex, endIndex);
		}

		private void advance()
		{
			beginIndex = endIndex + 1;
			endIndex = line.indexOf("|", beginIndex);
			if (endIndex == -1)
				endIndex = line.length();
			currentField++;
		}
	}
}
