package banner.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class UnmodifiableArraySet<E> implements Set<E>
{

    private E[] values;


    @SuppressWarnings("unchecked")
    public UnmodifiableArraySet(Set<? extends E> c)
    {
        values = (E[])new Object[c.size()];
        Iterator<? extends E> i = c.iterator();
        int current = 0;
        while (i.hasNext())
        {
            values[current++] = i.next();
        }
    }


    public Iterator<E> iterator()
    {
        return new UnmodifiableArraySetIterator();
    }


    public int size()
    {
        return values.length;
    }


    public boolean isEmpty()
    {
        return values.length == 0;
    }


    public boolean contains(Object o)
    {
        for (int i = 0; i < values.length; i++)
            if (o.equals(values[i]))
                return true;
        return false;
    }


    public boolean containsAll(Collection<?> c)
    {
        Iterator<?> e = c.iterator();
        while (e.hasNext())
            if (!contains(e.next()))
                return false;
        return true;
    }


    public boolean add(E arg0)
    {
        throw new UnsupportedOperationException();
    }


    public boolean addAll(Collection<? extends E> arg0)
    {
        throw new UnsupportedOperationException();
    }


    public void clear()
    {
        throw new UnsupportedOperationException();
    }


    public boolean remove(Object arg0)
    {
        throw new UnsupportedOperationException();
    }


    public boolean removeAll(Collection<?> arg0)
    {
        throw new UnsupportedOperationException();
    }


    public boolean retainAll(Collection<?> arg0)
    {
        throw new UnsupportedOperationException();
    }


    public Object[] toArray()
    {
        throw new UnsupportedOperationException();
    }


    public <T> T[] toArray(T[] arg0)
    {
        throw new UnsupportedOperationException();
    }

    private class UnmodifiableArraySetIterator implements Iterator<E>
    {
        private int index;


        public UnmodifiableArraySetIterator()
        {
            index = 0;
        }


        public boolean hasNext()
        {
            return index < values.length;
        }


        public E next()
        {
            return values[index++];
        }


        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }
}
