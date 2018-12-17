package banner.types;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Entity
{
	private String id;
	private EntityType type;
	private Set<EntityName> names;

	public Entity(String id, EntityType type)
	{
		this.id = id;
		this.type = type;
		names = new HashSet<EntityName>();
	}

	public void addName(EntityName name)
	{
		if (!equals(name.getEntity()))
			throw new IllegalArgumentException();
		names.add(name);
	}

	public String getId()
	{
		return id;
	}

	public EntityType getEntityType()
	{
		return type;
	}

	public Set<EntityName> getNames()
	{
		return Collections.unmodifiableSet(names);
	}

	// hash code is just the hash code of the id
	@Override
	public int hashCode()
	{
		return this.id.hashCode();
	}

	// two entities are equal id they have the same id
	@Override
	public boolean equals(Object o)
	{
		Entity temp = (Entity) o;
		return this.id.equals(temp.getId());
	}

	@Override
	public String toString()
	{
		return type.toString() + ": " + id;
	}

}
