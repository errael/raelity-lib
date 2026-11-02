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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author err
 */
@Disabled("")
public class MRUTest
{

MRU<String> mru;
private static final int DEFAULT_LIMIT = 5;
int limit = DEFAULT_LIMIT;
String testType;

public MRUTest(String testType)
{
    this.testType = testType;
    setupTestMRU();
}

/** Called at start of each test method with it's parameter.
 * In junit4, this was a constructor, but parameterized tests
 * can not be on the class level.
 */
private void setupTestMRU()
{
    mru = switch(testType) {
    case "list" -> MRU.getListMRU(limit);
    case "set" -> MRU.getSetMRU(limit);
    //case default, null -> throw new RuntimeException("WHAT?");
    default -> throw new RuntimeException("WHAT?");
    };
}

@BeforeAll
public static void setUpClass()
{
}

@AfterAll
public static void tearDownClass()
{
}

@BeforeEach
public void setUp()
{
    initMRU();
}

@AfterEach
public void tearDown()
{
}

void initMRU()
{
    limit = DEFAULT_LIMIT;
    mru.setCapacity(limit);
    mru.clear();
    mru.addAll(l1);
    initL2();
    initLRev();
}
void initL2()
{
    l2.clear();
    l2.addAll(l1);
}
void initLRev()
{
    lrev.clear();
    lrev.addAll(l1);
    Collections.reverse(lrev);
}

List<String> l1 = List.of("one", "two", "three", "four", "five");
List<String> l2 = new ArrayList<>();
List<String> lrev = new ArrayList<>();

@SuppressWarnings("UseOfSystemOutOrSystemErr")
void reportTest(String test)
{
    System.out.println(testType  + ": " + test);
}

/**
 * Test of addItem method, of class MRU.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testAddItem()
{
    reportTest("addItem");

    ArrayList<String> l;

    // initial state
    l = new ArrayList<>(mru);
    assertEquals(l1, l);

    // adding the MRU should not change anything, return false
    boolean result = mru.addItem("one");
    assertEquals(false, result);

    l = new ArrayList<>(mru);
    assertEquals(l1, l);

    result = mru.addItem("two"); // true, changed the list
    assertEquals(true, result);

    l = new ArrayList<>(mru);
    l2.remove("two");
    l2.add(0, "two");
    assertEquals(l2, l);

    initL2();
    for(String key : l1) {
        mru.addItem(key);
    }
    l = new ArrayList<>(mru);
    assertEquals(lrev, l);
}

/**
 * Test of removeItem method, of class MRU.
 * @param which
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testRemoveItem()
{
    reportTest("removeItem");

    ArrayList<String> l;
    boolean result;

    result = mru.removeItem("foobar");
    assertEquals(false, result);

    result = mru.removeItem("four");
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    l2.remove("four");
    assertEquals(l2, l);

    result = mru.removeItem("three");
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    l2.remove("three");
    assertEquals(l2, l);
}

/**
 * Actually test setCapacity
 * @param which
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testSetMaxSize()
{
    reportTest("trim");

    ArrayList<String> l;
    boolean result;

    // trim does nothing, returns false if within bounds
    result = mru.setCapacity(mru.getCapacity());
    assertEquals(false, result);
    l = new ArrayList<>(mru);
    assertEquals(l1, l);

    limit = 4;
    result = mru.setCapacity(limit);
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    l2.remove("five");
    assertEquals(l2, l);

    // make sure initMRU creates full list
    initMRU();
    l = new ArrayList<>(mru);
    assertEquals(l1, l);

    limit = 3;
    result = mru.setCapacity(limit);
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    l2.removeAll(List.of("four", "five"));
    assertEquals(l2, l);

    initMRU();
    limit = 0;
    result = mru.setCapacity(limit);
    assertEquals(true, result);
    l = new ArrayList<>(mru);
    assertEquals(List.of(), l);

    // check trim operates during addAll
    // Note that l2 was not modified since previous initMRU
    limit = 3;
    mru.setCapacity(limit);
    mru.clear();
    mru.addAll(l1);
    l = new ArrayList<>(mru);
    l2.removeAll(List.of("four", "five"));
    assertEquals(l2, l);
}
/**
 * Test of peekMRU method, of class MRU.
 * @param which
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testPeekMRU()
{
    reportTest("peekMRU");

    assertEquals("one", mru.peekMRU());

    mru.removeItem("three");
    assertEquals("one", mru.peekMRU());

    assertEquals(4, mru.size());
    mru.addItem("x");
    assertEquals("x", mru.peekMRU());

    assertEquals(5, mru.size());

    mru.addItem("y");
    assertEquals("y", mru.peekMRU());
    assertEquals(5, mru.size());
}

/**
 * Test of exceptions , of class MRU.
 * @param which
 */
@Test
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "ThrowableResultIgnored"})
public void testExceptions()
{
    reportTest("exceptions");

    assertThrows(UnsupportedOperationException.class, () -> mru.remove("two"));
    assertEquals(5, mru.size());
    assertThrows(UnsupportedOperationException.class, () -> mru.add("x"));
    assertThrows(UnsupportedOperationException.class, () -> mru.add("y"));
    assertEquals(5, mru.size());
    assertThrows(UnsupportedOperationException.class, () -> mru.removeAll(l2));
    assertEquals(5, mru.size());
}

static { if(Boolean.FALSE) sf("foo"); }
static String sf(String fmt, Object... args)
{
    return String.format(fmt, args);
}
}
