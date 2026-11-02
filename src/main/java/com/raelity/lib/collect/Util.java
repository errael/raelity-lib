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
import java.util.EnumSet;

/**
 *
 * @author err
 */
public class Util
{

/** return v1 {@literal &} v2 */
public static <T extends Enum<T>> EnumSet<T> intersection(EnumSet<T> v1, EnumSet<T> v2)
{
    EnumSet<T> v0 = EnumSet.copyOf(v1);
    v0.retainAll(v2);
    return v0;
}

public static <T extends Enum<T>> boolean intersects(EnumSet<T> v1, EnumSet<T> v2)
{
    return !intersection(v1, v2).isEmpty();
}

/**
 * Clear dst, copy contents from src to dst.
 */
static public <T> void assignContents(Collection<? super T> dst,
                                      Collection<? extends T> src)
{
    dst.clear();
    dst.addAll(src);
}

private Util() { }
}
