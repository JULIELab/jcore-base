/**
 * 
 */
package banner.annotation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConceptName
{
	private String conceptId;
	private String name;

	public ConceptName(String conceptId, String name)
	{
		if (conceptId == null)
			throw new IllegalArgumentException("conceptId cannot be null");
		this.conceptId = conceptId;
		if (name == null)
			throw new IllegalArgumentException("name cannot be null");
		this.name = name;
	}

	public String getConceptId()
	{
		return conceptId;
	}

	public String getName()
	{
		return name;
	}

	public static void loadConceptNames(String filename, Map<String, Concept> concepts, Map<String, List<ConceptName>> conceptNames) throws IOException
	{
		System.out.println("Loading concept names");
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		while (line != null)
		{
			line = line.trim();
			if (line.length() > 0)
			{
				String[] split = line.split("\\t");
				if (split.length != 2)
				{
					// TODO Improve exception handling
					throw new RuntimeException("Concept names file is in wrong format");
				}
				String conceptId = split[0];
				String name = split[1];
				if (!concepts.containsKey(conceptId))
				{
					// TODO Improve exception handling
					throw new RuntimeException("Concepts do not contain identifier " + conceptId);
				}
				List<ConceptName> names = conceptNames.get(conceptId);
				if (names == null)
				{
					names = new ArrayList<ConceptName>();
					conceptNames.put(conceptId, names);
				}
				names.add(new ConceptName(conceptId, name));
			}
			line = reader.readLine();
		}
	}

}