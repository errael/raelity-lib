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

import org.junit.jupiter.api.Nested;

/**
 *
 * @author err
 */
public class MRU_Group_Test
{

    @Nested
    public class MRU_List_Test extends MRUTest
    {
    public MRU_List_Test()
    {
        super("list");
    }
    }

    @Nested
    public class MRU_Set_Test extends MRUTest
    {
    public MRU_Set_Test()
    {
        super("set");
    }
    }
    
}
