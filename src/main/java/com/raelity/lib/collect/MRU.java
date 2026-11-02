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

import java.util.Collection;

/**
 * Most recently used collection, the only methods that modify this
 * collection is {@code addItem}, {@code removeItem},
 * or {@code addAll}; other attempts to modify throw.
 * Implementations are expected to be
 * unmodifieable except as noted above.
 * Only one of a given element is allowed, like a set.
 * The iteration order is most recent first.
 * <p>
 * Implementations in {@link AbstractMRU}
 * There is synchronizedMRU.
 * <pre>
 * performance for available implementations
 * 
 *               LinkedList                 LinkedHashSet
 * 
 * removeItem    loopfind                   treefind-remove
 *
 * 
 * addItem      loopfind                    treefind-remove
 *              list insert                 treefind-insert
 *
 * forEach      direct                      create array list
 *
 * trim         removeLast()                it.remove()
 *
 * iterator     direct                      create array list
 * </pre>
 */
// TODO: extend both List and Set?
public interface MRU<E> extends Collection<E>
{

/**
 * Implementation based on LinkedHashSet. Arg limit is used
 * for trim(); may be null which means no limit.
 */
public static <E>MRU<E> getSetMRU(int limit)
{
    return new AbstractMRU.LinkedHashSetMRU<>(limit);
}

/**
 * Implementation based on LinkedList. Arg limit is used
 * for trim(); may be null which means no limit.
 */
public static <E>MRU<E> getListMRU(int limit)
{
    return new AbstractMRU.LinkedListMRU<>(limit);
}

/** Returns true if the collection changed.
 * Typically only returns false if the current mru item is the argument.
 */
boolean addItem(E key);

/** contract like remove(E). */
boolean removeItem(E key);

// /** Remove LRU items until within limit. return true if modified */
// boolean trim();

/** Set the limit to the number of elements, 0 implies no limit.
 * @return true if the size changed
 */
boolean setCapacity(int size);
/** Get the limit to the number of elements */
int getCapacity();

/** The MRU item. null if empty. */
E peekMRU();

@Override void clear();
@Override boolean addAll(Collection< ? extends E> c);

/**
 * The limitations/restrictions specified in 
 * {@code Collections.synchronizedCollection}
 * apply; note: applies to any Iterable returned.
 */
public static <E> MRU<E> synchronizedMRU(MRU<E> mru)
{
    return new AbstractMRU.SynchronizedMRU<>(mru);
}

} // END INTERFACE MRU ////////////////////////////////////////


// vi: sw=4 ts=8
