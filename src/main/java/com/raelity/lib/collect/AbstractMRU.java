/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package com.raelity.lib.collect;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractMRU<E> extends AbstractCollection<E> implements MRU<E>
{
protected final Collection<E> delegate;
protected int capacity = Integer.MAX_VALUE;

public AbstractMRU(Collection<E> c, int maxSize)
{
    this.delegate = c;
    this.capacity = maxSize;
}

protected abstract void insert(E key);

@Override
public boolean addItem(E key)
{
    // no change if current MRU is what is added
    if(Objects.equals(key, peekMRU()))
        return false;
    // remove key
    delegate.remove(key);
    // and insert as MRU
    insert(key);
    trim();
    return true; // since insert always does if not eary return
}

@Override
public boolean removeItem(E key)
{
    return delegate.remove(key);
}

@Override
public int size()
{
    return delegate.size();
}

@Override
public boolean addAll(Collection<? extends E> c)
{
    boolean modified = false;
    // add stuff in reverse order, since mru is first in arg collection
    List<E> l;
    if(c instanceof List) {
        @SuppressWarnings("unchecked")
        List<E> tmp = (List<E>)c;
        l = tmp;
    } else {
        l = new ArrayList<>(c);
    }
    for(ListIterator<E> it = l.listIterator(l.size()); it.hasPrevious();) {
        if(addItem(it.previous()))
            modified = true;
    }
    if(trim())
        modified = true;
    return modified;
}

@Override
public void clear()
{
    delegate.clear();
}

protected abstract void doTrim(int iLimit);

@Override
public boolean setCapacity(int maxSize)
{
    if(maxSize < 0)
        throw new IllegalArgumentException("Size must be >= 0");
    this.capacity = maxSize;
    return trim();
}

@Override
public int getCapacity()
{
    return capacity;
}

private boolean trim()
{
    int size = size();
    doTrim(capacity);
    return size != size();
}

/** peekMRU() is used to determine if addItem would change the list. */
@Override
public abstract E peekMRU();

//////////////////////////////////////////////////////////////////////
//
// Implementations
//


// CLASS LinkedListMRU ///////////////////////////////////////////////;
/** Simple implementation, if mostly accessing mru,
 * then this is probably the best.
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})
public static class LinkedListMRU<E> extends AbstractMRU<E>
        implements MRU<E>
{

public LinkedListMRU(int limit)
{
    super(new LinkedList<E>(), limit);
}

private LinkedList<E> getDelegate()
{
    return (LinkedList<E>)delegate;
}

@Override
public E peekMRU()
{
    return getDelegate().peekFirst();
}


@Override
protected void insert(E key)
{
    getDelegate().addFirst(key);
}

@Override
public Iterator<E> iterator()
{
    Iterator<E> it = getDelegate().iterator();
    return new Iterator<E>()
    {
        @Override
        public boolean hasNext()
        {
            return it.hasNext();
        }
        
        @Override
        public E next()
        {
            return it.next();
        }
    };
}

@Override
protected void doTrim(int iLimit)
{
    while(delegate.size() > iLimit)
        getDelegate().removeLast();
}
} // END CLASS LinkedListMRU //////////////////////////////////////////////


// CLASS LinkedHashSetMRU ////////////////////////////////////////////////
/**
 * This is best if random lookup is most important
 * or large datasets.
 * But probably most lookup/remove is recent.
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})
public static class LinkedHashSetMRU<E> extends AbstractMRU<E>
        implements MRU<E>
{
/** cache the MRU item, since it's expensive to get at. */
private E itemMRU;

public LinkedHashSetMRU(int limit)
{
    super(new LinkedHashSet<>(0), limit);
}

private LinkedHashSet<E> getDelegate()
{
    return (LinkedHashSet<E>)delegate;
}

@Override
public boolean removeItem(E key)
{
    if(Objects.equals(key, itemMRU))
        itemMRU = null;
    return super.removeItem(key);
}

@Override
public E peekMRU()
{
    return itemMRU;
}

@Override
protected void insert(E key)
{
    itemMRU = key;
    getDelegate().add(key);
}

@Override
public Iterator<E> iterator()
{
    ArrayList<E> l = new ArrayList<>(delegate);
    ListIterator<E> it = l.listIterator(l.size());
    return new Iterator<E>()
    {
        @Override
        public boolean hasNext()
        {
            return it.hasPrevious();
        }
        
        @Override
        public E next()
        {
            return it.previous();
        }
    };
}

@Override
protected void doTrim(int iLimit)
{
    Iterator<E> it = delegate.iterator();
    while(delegate.size() > iLimit) {
        //System.err.println("CLOSED: trim: curlen " + size());
        it.next();
        it.remove();
    }
}

} // END CLASS LinkedHashSetMRU ////////////////////////////////////////

    // CLASS SynchronizedMRU ////////////////////////////////////////////////

    @SuppressWarnings("serial")
    static class SynchronizedMRU<E1> extends SyncCol<E1>
            implements MRU<E1>
    {
    final private MRU<E1> mru;
    
    public SynchronizedMRU(MRU<E1> mru)
    {
        super(mru);
        this.mru = mru;
    }
    
    @Override
    public boolean addItem(E1 key) {
        synchronized (mutex) {return mru.addItem(key);}
    }
    
    @Override
    public boolean removeItem(E1 key) {
        synchronized (mutex) {return mru.removeItem(key);}
    }
    
    @Override
    public boolean setCapacity(int size)
    {
        synchronized (mutex) {return mru.setCapacity(size);}
    }
    
    @Override
    public int getCapacity()
    {
        synchronized (mutex) {return mru.getCapacity();}
    }
    
    @Override
    public E1 peekMRU()
    {
        synchronized(mutex) { return mru.peekMRU(); }
    }
    
    }
    // END CLASS SynchronizedMRU ///////////////////////////////////////////


    // CLASS SyncCol ////////////////////////////////////////

    /**
     * Synchronized Collection that can be inherited.
     * delegate everything but equals, hashcode
     */
    @SuppressWarnings("serial")
    static class SyncCol<E1>
            implements Collection<E1>, Serializable
    {
    final protected Collection<E1> mutex;

    public SyncCol(Collection<E1> c)
    {
        this.mutex = Collections.synchronizedCollection(c);
    }

    @Override
    public int size()
    {
        return mutex.size();
    }

    @Override
    public boolean isEmpty()
    {
        return mutex.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return mutex.contains(o);
    }

    @Override
    public Iterator<E1> iterator()
    {
        return mutex.iterator(); // Must be manually synched by user!
    }

    @Override
    public Object[] toArray()
    {
        return mutex.toArray();
    }

    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(T[] a)
    {
        return mutex.toArray(a);
    }

    // for JDK-11 and later
    //@Override
    //public <T> T[] toArray(IntFunction<T[]> generator)
    //{
    //    return mutex.toArray(generator);
    //}

    // for JDK-8
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(IntFunction<T[]> generator)
    {
        return mutex.toArray(generator.apply(0));
    }

    @Override
    public boolean add(E1 e)
    {
        return mutex.add(e);
    }

    @Override
    public boolean remove(Object o)
    {
        return mutex.remove(o);
    }

    @Override
    public boolean containsAll(
            Collection<?> c)
    {
        return this.mutex.containsAll(c);
    }

    @Override
    public boolean addAll(
            Collection<? extends E1> c)
    {
        return this.mutex.addAll(c);
    }

    @Override
    public boolean removeAll(
            Collection<?> c)
    {
        return this.mutex.removeAll(c);
    }

    @Override
    public boolean removeIf(
            Predicate<? super E1> filter)
    {
        return mutex.removeIf(filter);
    }

    @Override
    public boolean retainAll(
            Collection<?> c)
    {
        return this.mutex.retainAll(c);
    }

    @Override
    public void clear()
    {
        mutex.clear();
    }

    @Override
    public Spliterator<E1> spliterator()
    {
        return mutex.spliterator(); // Must be manually synched by user!
    }

    @Override
    public Stream<E1> stream()
    {
        return mutex.stream(); // Must be manually synched by user!
    }

    @Override
    public Stream<E1> parallelStream()
    {
        return mutex.parallelStream(); // Must be manually synched by user!
    }

    @Override
    public void forEach(
            Consumer<? super E1> action)
    {
        mutex.forEach(action);
    }

    @Override
    public String toString()
    {
        return mutex.toString();
    }

    }
    // END CLASS SyncCol ////////////////////////////////////

} // END CLASS AbstractMRU ////////////////////////////////////////
