/**
 * 
 */
package banner.annotation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Text
{
	private String id;
	private String text;
	private boolean complete;

	public Text(String id, String text, boolean complete)
	{
		if (id == null)
			throw new IllegalArgumentException("id cannot be null");
		this.id = id;
		if (text == null)
			throw new IllegalArgumentException("text cannot be null");
		this.text = text;
		this.complete = complete;
	}

	public String getId()
	{
		return id;
	}

	public String getText()
	{
		return text;
	}

	public boolean isComplete()
	{
		return complete;
	}

	public void setComplete(boolean complete)
	{
		this.complete = complete;
	}

	public static void loadTexts(String textsFilename, String completeFilename, List<String> textIds, Map<String, Text> texts) throws IOException
	{
		Set<String> completeTextIds = new HashSet<String>();
		BufferedReader reader = new BufferedReader(new FileReader(completeFilename));
		String line = reader.readLine();
		while (line != null)
		{
			line = line.trim();
			if (line.length() > 0)
				completeTextIds.add(line);
			line = reader.readLine();
		}

		System.out.println("Loading texts");
		reader = new BufferedReader(new FileReader(textsFilename));
		line = reader.readLine();
		while (line != null)
		{
			line = line.trim();
			if (line.length() > 0)
			{
				String[] split = line.split("\\t");
				if (split.length != 2)
				{
					System.out.println(line);
					// TODO Improve exception handling
					throw new RuntimeException("Text file is in wrong format");
				}
				String textId = split[0];
				String textStr = split[1];
				Text text = new Text(textId, textStr, completeTextIds.contains(textId));
				if (texts.containsKey(text.getId()))
				{
					// TODO Improve exception handling
					throw new RuntimeException("Duplicate identifier in text file: " + text.getId());
				}
				if (!text.isComplete())
					textIds.add(text.getId());
				texts.put(text.getId(), text);
			}
			line = reader.readLine();
		}

		for (String textId : completeTextIds)
		{
			if (!texts.containsKey(textId))
			{
				// TODO Improve exception handling
				throw new RuntimeException("List of completed textIDs contains unknown ID: " + textId);
			}
		}
	}

	public static void saveTextCompletion(String filename, Map<String, Text> texts)
	{
		try
		{
			PrintWriter writer = new PrintWriter(filename);
			System.out.println("Saving text completion");
			for (Text text : texts.values())
			{
				if (text.isComplete())
					writer.println(text.getId());
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