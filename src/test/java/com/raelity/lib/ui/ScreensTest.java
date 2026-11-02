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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author err
 */
public class ScreensTest
{

public ScreensTest()
{
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
}

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
