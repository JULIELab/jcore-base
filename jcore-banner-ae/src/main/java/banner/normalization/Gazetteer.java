package banner.normalization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import banner.tokenization.Tokenizer;
import banner.types.Entity;
import banner.types.EntityName;
import banner.types.EntityType;

/**
 * A Gazetteer allows read in a list of {@link Entity}s and their associated {@link EntityName}s from a file. These can
 * also be re-written to a file to simplify loading at a later time.
 */
public class Gazetteer
{

	private Map<String, Entity> id2EntityMap;
	private Set<Entity> entities;

	public Gazetteer()
	{
		id2EntityMap = new HashMap<String, Entity>();
		entities = new HashSet<Entity>();
	}

	public void load(Tokenizer tokenizer, EntityType type, String filename) throws IOException
	{
		// System.out.println("Dictionary Read Start");

		FileInputStream fstream = new FileInputStream(filename);
		Entity ent;

		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		// String entry = firstLine + "\n";

		while ((strLine = br.readLine()) != null)
		{
			String[] entries = strLine.split("\t");
			ent = new Entity(entries[0], type);

			for (int i = 1; i < entries.length; i++)
			{
				EntityName name = new EntityName(ent, entries[i], tokenizer.getTokens(entries[i]));
				ent.addName(name);
			}
			addEntity(ent);
		}

		in.close();
		// System.out.println(dictionary);
		// System.out.println("Dictionary Read End");
	}

	public void load(Tokenizer tokenizer, String namesFilename) throws IOException
	{
		this.load(tokenizer, namesFilename, null);
	}

	public interface EntityFilter
	{
		public boolean include(Entity e);
	}

	public void load(Tokenizer tokenizer, String namesFilename, EntityFilter entityFilter) throws IOException
	{

		// System.out.println("Dictionary Read Start");
		BufferedReader nameIn = new BufferedReader(new FileReader(namesFilename));

		String strLine;
		Map<String, Entity> localId2EntityMap = new HashMap<String, Entity>();
		while ((strLine = nameIn.readLine()) != null)
		{

			// System.out.println("Name file read started");

			String[] entries = strLine.split("\t");

			String type = entries[0];
			String id = entries[1];

			Entity ent = new Entity(id, EntityType.getType(type));

			for (int i = 1; i < entries.length; i++)
			{
				EntityName name = new EntityName(ent, entries[i], tokenizer.getTokens(entries[i]));
				ent.addName(name);
			}

			// System.out.println(type + "\t" + id);
			localId2EntityMap.put(ent.getId(), ent);
		}

		// System.out.println("Name File Read = Done");

		nameIn.close();

		// Add entities if not filtered
		for (String id : localId2EntityMap.keySet())
		{
			Entity entity = localId2EntityMap.get(id);
			if (entityFilter == null || entityFilter.include(entity))
				addEntity(entity);
		}

		// System.out.println(dictionary);
		// System.out.println("Dictionary Read End");
	}

	protected void addEntity(Entity entity)
	{
		id2EntityMap.put(entity.getId(), entity);
		entities.add(entity);
	}

	public Entity getEntity(String id)
	{
		return id2EntityMap.get(id);
	}

	public Set<Entity> getEntities()
	{
		return Collections.unmodifiableSet(entities);
	}

	/*********
	 * A gazetteer is written as two parts, one contains the Type, ID and EntityNames and the second contains the entity
	 * relationships
	 * 
	 * @param type
	 * @param namesFilename
	 * @param relationshipFilename
	 * @throws IOException
	 */

	public void write(String namesFilename) throws IOException
	{
		// System.out.println("Dictionary Write Start");
		BufferedWriter nameOut = new BufferedWriter(new FileWriter(namesFilename));

		for (Entity currEnt : entities)
		{
			nameOut.write(currEnt.getEntityType().getText() + "\t");
			nameOut.write(currEnt.getId() + "\t");
			for (EntityName name : currEnt.getNames())
				nameOut.write(name.getName() + "\t");
			nameOut.write("\n");
		}

		nameOut.close();
	}
}
