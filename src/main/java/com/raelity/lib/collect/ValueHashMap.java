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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.raelity.lib.collect.ValueMap.checkMap;

/**
 *
 * @author err
 */
@SuppressWarnings("serial")
public class ValueHashMap<K, V>
extends HashMap<K, V>
implements ValueMap<K, V>
{
    private final Function<V,K> keyFunction;

    public ValueHashMap(Function<V, K> keyFunction, int initialCapacity)
    {
        super(initialCapacity);
        this.keyFunction = keyFunction;
    }

    public ValueHashMap(Function<V, K> keyFunction)
    {
        this.keyFunction = keyFunction;
    }

    public ValueHashMap(Function<V, K> keyFunction,
                        Map<? extends K, ? extends V> map)
    {
        super(checkMap(keyFunction, map));
        this.keyFunction = keyFunction;
    }

    @Override
    public V put(V value)
    {
        return super.put(keyFunction.apply(value), value);
    }

    @Override
    public Function<V, K> getKeyFunction()
    {
        return keyFunction;
    }


    @Override
    public V put(K key, V value)
    {
        if(!key.equals(keyFunction.apply(value)))
            throw kvex(key, value);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        super.putAll(checkMap(keyFunction, m));
    }

    @Override
    public V replace(K key, V value)
    {
        if(!key.equals(keyFunction.apply(value)))
            throw kvex(key, value);
        return super.replace(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue)
    {
        if(!key.equals(keyFunction.apply(newValue)))
            throw kvex(key, newValue);
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public Object clone()
    {
        return super.clone();
    }

    private static <K,V>IllegalArgumentException kvex(K k, V v)
    {
        return new IllegalArgumentException(
                String.format("K/V miss-match: %s/%s", k, v));
    }

    ///////////////////////////////////////////////////////////////////////
    //
    // LATER...
    //

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
    {
        if(true) throw new UnsupportedOperationException("yet");
        super.replaceAll(function);
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.merge(key, value, remappingFunction);
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.compute(key, remappingFunction);
    }

    @Override
    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V putIfAbsent(K key, V value)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.putIfAbsent(key, value);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue)
    {
        if(true) throw new UnsupportedOperationException("yet");
        return super.getOrDefault(key, defaultValue);
    }
}
