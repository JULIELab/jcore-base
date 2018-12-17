/**
 * 
 */
package banner.annotation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class Concept
{
	private String type;
	private String id;
	private String description;

	public Concept(String type, String id, String description)
	{
		if (type == null)
			throw new IllegalArgumentException("type cannot be null");
		this.type = type;
		if (id == null)
			throw new IllegalArgumentException("id cannot be null");
		this.id = id;
		if (description == null)
			throw new IllegalArgumentException("description cannot be null");
		this.description = description;
	}

	public String getType()
	{
		return type;
	}

	public String getId()
	{
		return id;
	}

	public String getDescription()
	{
		return description;
	}

	public static void loadConcepts(String filename, Map<String, Concept> concepts) throws IOException
	{
		System.out.println("Loading concepts");
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		while (line != null)
		{
			line = line.trim();
			if (line.length() > 0)
			{
				String[] split = line.split("\\t");
				if (split.length != 3)
				{
					// TODO Improve exception handling
					throw new RuntimeException("Concepts file is in wrong format");
				}
				String semanticType = split[0];
				String conceptId = split[1];
				String description = split[2];
				if (concepts.containsKey(conceptId))
				{
					// TODO Improve exception handling
					throw new RuntimeException("Concepts file contains duplicate concept IDs: " + conceptId);
				}
				concepts.put(conceptId, new Concept(semanticType, conceptId, description));
			}
			line = reader.readLine();
		}
	}
}