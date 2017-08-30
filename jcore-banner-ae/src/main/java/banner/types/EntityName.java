package banner.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntityName
{

	private Entity entity;
	private String name;
	private List<String> elements;

	private EntityName()
	{
		// Empty
	}

	public EntityName(Entity entity, String name, List<String> elements)
	{
		if (entity == null)
			throw new IllegalArgumentException();
		this.entity = entity;
		if (name == null)
			throw new IllegalArgumentException();
		this.name = name;
		this.elements = Collections.unmodifiableList(new ArrayList<String>(elements));
	}

	public static EntityName createFromTokens(String name, List<Token> tokens)
	{
		EntityName entityName = new EntityName();
		entityName.entity = null;
		entityName.name = name;
		entityName.elements = new ArrayList<String>();
		for (Token token : tokens)
			entityName.elements.add(token.getText());
		entityName.elements = Collections.unmodifiableList(entityName.elements);
		return entityName;
	}

	public Entity getEntity()
	{
		return entity;
	}

	public String getName()
	{
		return name;
	}

	public List<String> getElements()
	{
		return elements;
	}

	@Override
	public String toString()
	{
		return name + "(" + entity + ")";
	}
}
