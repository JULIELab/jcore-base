/**
 * 
 */
package banner.annotation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Annotation implements Comparable<Annotation>
{

	private int start;
	private int end;
	private String textId;
	private String semanticType;
	private String conceptId;

	public Annotation(int start, int end, String textId, String semanticType, String conceptId)
	{
		this.start = start;
		this.end = end;
		this.textId = textId;
		this.semanticType = semanticType;
		this.conceptId = conceptId;
	}

	public String getTextId()
	{
		return textId;
	}

	public String getSemanticType()
	{
		return semanticType;
	}

	public void setSemanticType(String semanticType)
	{
		this.semanticType = semanticType;
	}

	public String getConceptId()
	{
		return conceptId;
	}

	public void setConceptId(String conceptId)
	{
		this.conceptId = conceptId;
	}

	public int getStart()
	{
		return start;
	}

	public int getEnd()
	{
		return end;
	}

	@Override
	public int compareTo(Annotation annotation2)
	{
		int cmp = start - annotation2.start;
		if (cmp != 0)
			return cmp;
		return end - annotation2.end;
	}

	public static void loadAnnotations(String filename, Map<String, Text> texts, Map<String, Concept> concepts, Map<String, List<Annotation>> annotations) throws IOException
	{
		// Create an empty list of annotations for each text
		System.out.println("Loading annotations");
		for (String textId : texts.keySet())
			annotations.put(textId, new ArrayList<Annotation>());
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		while (line != null)
		{
			line = line.trim();
			if (line.length() > 0)
			{
				String[] split = line.split("\\t");
				if (split.length != 6)
				{
					// TODO Improve exception handling
					throw new RuntimeException("Annotation file is in wrong format");
				}
				String textId = split[0];
				int start = Integer.parseInt(split[1]);
				int end = Integer.parseInt(split[2]);
				String semanticType = split[3];
				String conceptId = split[4];
				if (conceptId.equals("null"))
					conceptId = null;
				String annotationText = split[5];
				// TODO Validate concept
				Text text = texts.get(textId);
				if (text == null)
				{
					// TODO Improve exception handling
					throw new RuntimeException("Texts does not contain identifier: " + textId);
				}
				if (!annotationText.equals(text.getText().substring(start, end)))
				{
					// TODO Improve exception handling
					throw new RuntimeException("Annotation text does not match: " + textId);
				}
				if (conceptId != null && !concepts.containsKey(conceptId))
				{
					// TODO Improve exception handling
					throw new RuntimeException("Concepts do not contain identifier " + conceptId);
				}
				Annotation annotation = new Annotation(start, end, textId, semanticType, conceptId);
				annotations.get(textId).add(annotation);
			}
			line = reader.readLine();
		}
	}

	public static void saveAnnotations(String filename, Map<String, Text> texts, Map<String, List<Annotation>> annotations)
	{
		System.out.println("Saving annotations");
		Set<String> textIds = new TreeSet<String>(annotations.keySet());
		try
		{
			PrintWriter writer = new PrintWriter(filename);
			for (String id : textIds)
			{
				Text text = texts.get(id);
				for (Annotation annotation : annotations.get(id))
				{
					StringBuilder line = new StringBuilder();
					line.append(annotation.getTextId());
					line.append("\t");
					line.append(annotation.getStart());
					line.append("\t");
					line.append(annotation.getEnd());
					line.append("\t");
					line.append(annotation.getSemanticType());
					line.append("\t");
					line.append(annotation.getConceptId());
					line.append("\t");
					line.append(text.getText().substring(annotation.getStart(), annotation.getEnd()));
					writer.println(line);
				}
			}
			writer.close();
		}
		catch (FileNotFoundException e)
		{
			// TODO Improve exception handling
			e.printStackTrace();
		}
	}
}