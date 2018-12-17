/*
 * Copyright (c) 2005, Sorenson Molecular Genealogy Foundation,
 * 2480 South Main Street, Salt Lake City, Utah, 84115 U.S.A.
 * All rights reserved.
 *
 * \$RCSfile: Index.java,v $
 * Owner: bleaman
 * Created on: Jul 27, 2006
 *
 * \$Author: Bob $
 * \$Revision: 1.1.1.1 $
 * \$Date: 2008/01/28 18:48:59 $
 */

package banner.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Index<K, V>
{

    public static final int defaultInitialMapCapacity = 128;
    public static final float defaultMapLoadFactor = (float)0.75;
    public static final int initialSetCapacity = 16;
    public static final float initialSetLoadFactor = (float)0.75;

    private Map<K, Set<V>> index;
    private boolean packed;


    public Index()
    {
        this(defaultInitialMapCapacity, defaultMapLoadFactor);
    }


    public Index(int initialCapacity)
    {
        this(initialCapacity, defaultMapLoadFactor);
    }


    public Index(int initialCapacity, float loadFactor)
    {
        index = new HashMap<K, Set<V>>(initialCapacity, loadFactor);
        packed = false;
    }


    public void add(K[] keys, V value)
    {
        for (int i = 0; i < keys.length; i++)
            add(keys[i], value);
    }


    public void add(K key, V value)
    {
        if (packed)
            throw new UnsupportedOperationException("Cannot add to a packed Index");
        if (key == null)
            throw new NullPointerException("Cannot add a null key");
        if (value == null)
            throw new NullPointerException("Cannot add a null value");
        Set<V> lookupSet = index.get(key);
        if (lookupSet == null)
            lookupSet = new HashSet<V>(initialSetCapacity, initialSetLoadFactor);
        lookupSet.add(value);
        index.put(key, lookupSet);
    }


    public Set<V> lookup(K key)
    {
        if (!packed)
            pack();
        if (key == null)
            throw new NullPointerException("key is null");
        return index.get(key);
    }


    public int numKeys()
    {
        return index.size();
    }


    public Set<K> keySet()
    {
        if (!packed)
            pack();
        return index.keySet();
    }


    public void pack()
    {
        index = new HashMap<K, Set<V>>(index);
        Iterator<K> i = index.keySet().iterator();
        while (i.hasNext())
        {
            K key = i.next();
            index.put(key, new UnmodifiableArraySet<V>(index.get(key)));
        }
        packed = true;
    }


    public void printStats()
    {
        int size;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int sum = 0;
        Iterator<K> i = index.keySet().iterator();
        while (i.hasNext())
        {
            size = lookup(i.next()).size();
            sum += size;
            max = Math.max(max, size);
            min = Math.min(min, size);
        }
        System.out.println("\tNumber of keys: " + index.size());
        System.out.println("\tMaximum set size: " + max);
        System.out.println("\tAverage set size: " + ((double)sum / index.size()));
        System.out.println("\tMinimum set size: " + min);
    }

}