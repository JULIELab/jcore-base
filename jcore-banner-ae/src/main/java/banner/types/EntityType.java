/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Instances of this class represent the type of a {@link Mention}. Instances of this class are kept in a static cache
 * to ensure only one instance of any type being in use. No processing is done on the text, however, so "Gene" is a
 * different type than "GENE"
 * 
 * @author Bob
 */
public class EntityType
{

	private static final Map<String, EntityType> types = new HashMap<String, EntityType>();

	private String text;
	private EntityType parent;

	private EntityType(String text, EntityType parent)
	{
		this.text = text;
		this.parent = parent;
	}

	public static EntityType getType(String text)
	{
		return getType(text, null);
	}

	public static EntityType getType(String text, EntityType parent)
	{
		if (text == null)
			throw new IllegalArgumentException();
		EntityType type = types.get(text);
		if (type == null)
		{
			type = new EntityType(text, parent);
			types.put(text, type);
		}
		return type;
	}

	public static Map<EntityType, EntityType> getViewMap(EntityType type)
	{
		// Get set of self & ancestors
		Set<EntityType> ancestors = new HashSet<EntityType>();
		EntityType current = type;
		while (current != null)
		{
			ancestors.add(current);
			current = current.parent;
		}
		// Iterate through all types
		// Note elements in a different tree get mapped to null
		Map<EntityType, EntityType> viewMap = new HashMap<EntityType, EntityType>();
		for (EntityType temp : new HashSet<EntityType>(types.values()))
		{
			current = temp;
			while (current != null && !ancestors.contains(current))
				current = current.parent;
			viewMap.put(temp, current);
		}
		return Collections.unmodifiableMap(viewMap);
	}

	public static Set<EntityType> getTypes()
	{
		return Collections.unmodifiableSet(new HashSet<EntityType>(types.values()));
	}

	public String getText()
	{
		return text;
	}

	public EntityType getParent()
	{
		return parent;
	}

	public List<EntityType> getAncestors()
	{
		List<EntityType> ancestors = new ArrayList<EntityType>();
		EntityType type = this;
		while (type != null)
		{
			ancestors.add(type);
			type = type.parent;
		}
		return Collections.unmodifiableList(ancestors);
	}

	@Override
	public String toString()
	{
		if (parent == null)
			return text;
		return parent.toString() + "->" + text;
	}

	@Override
	public int hashCode()
	{
		return text.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final EntityType other = (EntityType) obj;
		if (!text.equals(other.text))
			return false;
		return true;
	}
}
