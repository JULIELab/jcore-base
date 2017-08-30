/*
 * Copyright (c) 2005, Sorenson Molecular Genealogy Foundation,
 * 2480 South Main Street, Salt Lake City, Utah, 84115 U.S.A.
 * All rights reserved.
 *
 * \$RCSfile: RankedList.java,v $
 * Owner: bleaman
 * Created on: Jul 27, 2006
 *
 * \$Author: Bob $
 * \$Revision: 1.1.1.1 $
 * \$Date: 2008/01/28 18:48:59 $
 */

package banner.util;

public class RankedList<E>
{

	private double[] values;
	private E[] objects;
	private int sizeVal = 0;

	@SuppressWarnings("unchecked")
	public RankedList(int maxSize)
	{
		values = new double[maxSize];
		// The following is safe because all types have Object as a superclass
		objects = (E[]) new Object[maxSize];
	}

	public void add(double value, E obj)
	{
		if (Double.isNaN(value))
			return;
		int index = sizeVal;
		while ((index > 0) && (value > values[index - 1]))
		{
			if (index < objects.length)
			{
				values[index] = values[index - 1];
				objects[index] = objects[index - 1];
			}
			index--;
		}
		if (index < objects.length)
		{
			values[index] = value;
			objects[index] = obj;
			if (sizeVal < objects.length)
				sizeVal++;
		}
	}

	public int find(E obj)
	{
		for (int i = 0; i < sizeVal; i++)
			if (objects[i].equals(obj))
				return i;
		return -1;
	}

	public void clear()
	{
		sizeVal = 0;
	}

	public E getObject(int rank)
	{
		if (rank >= sizeVal)
			throw new IndexOutOfBoundsException();
		return objects[rank];
	}

	public double getValue(int rank)
	{
		if (rank >= sizeVal)
			throw new IndexOutOfBoundsException();
		return values[rank];
	}

	public int maxSize()
	{
		return objects.length;
	}

	public int size()
	{
		return sizeVal;
	}

}