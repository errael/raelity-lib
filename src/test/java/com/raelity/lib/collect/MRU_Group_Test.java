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
 * Tests for MRU of both list and set.
 */
public class MRU_Group_Test
{
  
  /** x */
  @Nested
  public class MRU_List_Test extends MRUTest
  {
    /** x */
    public MRU_List_Test() { }

    @Override
    MRU<String> setupTestMRU() {
      return MRU.getListMRU(limit);
    }
  }
  
  /** x */
  @Nested
  public class MRU_Set_Test extends MRUTest
  {
    /** x */
    public MRU_Set_Test() { }

    @Override
    MRU<String> setupTestMRU() {
      return MRU.getSetMRU(limit);
    }
  }
  
}
// vi: set sw=2 ts=8: