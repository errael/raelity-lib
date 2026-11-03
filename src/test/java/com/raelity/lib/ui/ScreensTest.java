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

package com.raelity.lib.ui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.raelity.lib.ui.ScreenOrder.ScreenItem;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author err
 */
public class ScreensTest
{

    /** x */
public ScreensTest()
{
}

/** x */
@BeforeAll
public static void setUpClass()
{
}

/** x */
@AfterAll
public static void tearDownClass()
{
}

/** x */
@BeforeEach
public void setUp()
{
}

/** x */
@AfterEach
public void tearDown()
{
}

/**
 * Test of setPrefGraphicsDev method, of class Screens.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testSetPrefGraphicsDev_String()
{
    System.out.println("setPrefGraphicsDev");

    // NOTE: can not depend on what current might be.
    GraphicsDevice current;

    String envPropVar = "SOME_RANDOM_ENV_PROP_THAT_NEVER_MATCHES";
    GraphicsDevice result = Screens.setPrefGraphicsDev(envPropVar);
    assertNull(result);

    System.setProperty(envPropVar, "100");
    result = Screens.setPrefGraphicsDev(envPropVar);
    assertNull(result);

    System.setProperty(envPropVar, "0");
    result = Screens.setPrefGraphicsDev(envPropVar);
    assertNotNull(result);
    // not a null result, so current may have changed
    current = Screens.getPrefGraphicsDev();
    assertEquals(current, result);

    System.setProperty(envPropVar, "100");
    result = Screens.setPrefGraphicsDev(envPropVar);
    assertNull(result);

    // doing prev with "100" should not have changed anything
    GraphicsDevice pref = Screens.getPrefGraphicsDev();
    assertEquals(current, pref);
}

static int K1 = 1000;
static int K2 = 2000;
/**
 * Each box is approx 2K wide, 1K high.
 * The params are the x,y postion and are multiplied by 1K.
 * The width/hieght are tweaked 
 * @param x
 * @param y
 * @return 
 */
ScreenItem r(int index, double x, double y) {
    ScreenItem item = new ScreenItem(index, new Rectangle((int)(x*K2), (int)(y*K1), K2, K1));
    return item;
}

// This is the expected result ordering.
private final List<ScreenItem> initialScreenItems
    = Collections.unmodifiableList(new ArrayList<>(List.of(
        r(0, 0, 0), r(1, 2, 0),  r(2, 4, 0),  r(3, 6, 0),
        r(4, 0, 1), r(5, 2, 1),  r(6, 4, 1),  r(7, 6, 1),
        r(8, 0, 2), r(9, 2, 2), r(10, 4, 2), r(11, 6, 2)
)));
// When the screens are sorted expect 0 - 11 from initialScreenItems
private final List<Integer> expectedOrder = IntStream.range(0, 12).boxed().toList();

private final Random random = new Random(37);
/** return randomized list of ScreenItem. */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
List<ScreenItem> randomItems() {
    List<ScreenItem> items = new ArrayList<>(initialScreenItems);
    Collections.shuffle(items, random);
    List<Integer> order = items.stream().map((item) -> item.getIndex()).toList();
    System.out.printf("Random_order: %s\n", order);
    return items;
}

/**
 * Test screen bounding sort.
 */
@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testScreenOrderGetScreenSort()
{
    System.out.println("ScreenOrder.getScreenSort");

    List<ScreenItem> randomItems = randomItems();
    // Same order as randomItems, arg for getScreenSort.
    List<Rectangle> randomRects = randomItems.stream().map((item) -> item.getBounds()).toList();

    List<ScreenItem> sorted = ScreenOrder.screenSort(randomRects);
    // The index of a sorted ScreenItem works with either radomItems or randomRects
    List<Integer> result = sorted.stream()
        .map(sortedItem -> randomItems.get(sortedItem.getIndex()))
        .map(randomItem -> randomItem.getIndex())
        .toList();
    assertEquals(expectedOrder, result);
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

// The following are all disabled

/**
 * Test of isVirtualEnvironment method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testIsVirtualEnvironment()
{
    System.out.println("isVirtualEnvironment");
    boolean expResult = false;
    boolean result = Screens.isVirtualEnvironment();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of getPrefGraphicsConfiguration method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetPrefGraphicsConfiguration()
{
    System.out.println("getPrefGraphicsConfiguration");
    GraphicsConfiguration expResult = null;
    GraphicsConfiguration result = Screens.getPrefGraphicsConfiguration();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of translateToPrefScreen method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testTranslateToPrefScreen_Window()
{
    System.out.println("translateToPrefScreen");
    Window window = null;
    Screens.translateToPrefScreen(window);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of translateToPrefScreen method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testTranslateToPrefScreen_Window_Point()
{
    System.out.println("translateToPrefScreen");
    Window window = null;
    Point target = null;
    Screens.translateToPrefScreen(window, target);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of translateToPrefScreen method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testTranslateToPrefScreen_Point()
{
    System.out.println("translateToPrefScreen");
    Point location = null;
    Point expResult = null;
    Point result = Screens.translateToPrefScreen(location);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of translateToScreen method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testTranslateToScreen()
{
    System.out.println("translateToScreen");
    Point location = null;
    Point target = null;
    Point expResult = null;
    Point result = Screens.translateToScreen(location, target);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of getPrefScreenBounds method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetPrefScreenBounds()
{
    System.out.println("getPrefScreenBounds");
    Rectangle expResult = null;
    Rectangle result = Screens.getPrefScreenBounds();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of centerOnPrefScreen method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testCenterOnPrefScreen()
{
    System.out.println("centerOnPrefScreen");
    Window window = null;
    Screens.centerOnPrefScreen(window);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of getPrefGraphicsDev method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetPrefGraphicsDev()
{
    System.out.println("getPrefGraphicsDev");
    GraphicsDevice expResult = null;
    GraphicsDevice result = Screens.getPrefGraphicsDev();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of setPrefGraphicsDev method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testSetPrefGraphicsDev_int()
{
    System.out.println("setPrefGraphicsDev");
    int screenIdx = 0;
    GraphicsDevice expResult = null;
    GraphicsDevice result = Screens.setPrefGraphicsDev(screenIdx);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

/**
 * Test of getGraphicsConfiguration method, of class Screens.
 */
//@Test
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public void testGetGraphicsConfiguration()
{
    System.out.println("getGraphicsConfiguration");
    Point location = null;
    GraphicsConfiguration expResult = null;
    GraphicsConfiguration result
            = Screens.getGraphicsConfiguration(location);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
}

}
