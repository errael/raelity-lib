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

import java.util.Map;
import java.util.function.Function;

/**
 * Map that takes it key from the value, see getKeyFunction.
 * The KeyFunction is typically set in a constructor.
 * If an operation is attempted and key != func(val),
 * an IllegalArgumentException is thrown.
 * 
 * @author err
 */
public interface ValueMap<K, V> extends Map<K, V>
{

/**
 * This is equivalent to put(getKeyFunction().apply(value), value).
 * @param value
 * @return 
 */
V put(V value);

/**
 * Function used by map to generate keys.
 * @return mapping function
 */
Function<V,K> getKeyFunction();

/**
 * Verify that the argument map has valid K/V entries according
 * to argument keyFunction.
 * @return the map
 * @throws IllegalArgumentException
 */
public static <K,V>Map<K,V> checkMap(Function<V, K> keyFunction,
                                     Map<? extends K, ? extends V> map)
{
    @SuppressWarnings("unchecked")
            Map<K, V> tmap = (Map<K, V>)map;
    for(Entry<K, V> entry : tmap.entrySet())
        if(!entry.getKey().equals(keyFunction.apply(entry.getValue())))
            throw new IllegalArgumentException(
                    String.format("K/V miss-match: %s/%s",
                                      entry.getKey(), entry.getValue()));
    return tmap;
}
}
