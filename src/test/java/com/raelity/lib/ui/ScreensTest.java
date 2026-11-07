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

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Screens}, run against fake screens,
 * see {@link FakeScreens}; these work headless.
 * <p>
 * Most tests use this layout; the platform order is deliberately not the
 * sorted order and the default device is not the first platform device.
 * <pre>
 *   platform 2: P (0,0)      1920x1080   default device
 *   platform 0: R (1920,0)   2560x1440   right of P, taller
 *   platform 1: B (0,1080)   1920x1080   below P
 *
 *   +-------+-----------+
 *   |   P   |           |
 *   +-------+     R     |
 *   |   B   |           |
 *   +-------+-----------+
 *
 *   sorted: P, R, B    layout: [[P, R], [B]]
 * </pre>
 */
public class ScreensTest {

  static final Rectangle R_BOUNDS = new Rectangle(1920, 0, 2560, 1440);
  static final Rectangle B_BOUNDS = new Rectangle(0, 1080, 1920, 1080);
  static final Rectangle P_BOUNDS = new Rectangle(0, 0, 1920, 1080);
  static final int R = 0, B = 1, P = 2; // platform indexes

  /** Properties set by a test, cleared after it. */
  private static final String PROP_PREFIX = "com.raelity.lib.ui.ScreensTest.";
  private static final String PREF_SCREEN = "JAVA_PREFERRED_SCREEN";

  private FakeScreens fs;

  @BeforeEach
  public void setUp() {
    fs = FakeScreens.open(P, R_BOUNDS, B_BOUNDS, P_BOUNDS);
    clearPrefScreen();
  }

  /**
   * A system property takes precedence over an environment variable;
   * an invalid index means use the default screen. This keeps the
   * developer's JAVA_PREFERRED_SCREEN environment variable out of the tests.
   */
  private static void clearPrefScreen() {
    System.setProperty(PREF_SCREEN, "-1");
  }

  @AfterEach
  public void tearDown() {
    System.getProperties().stringPropertyNames().stream()
        .filter(name -> name.startsWith(PROP_PREFIX))
        .forEach(System::clearProperty);
    System.clearProperty(PREF_SCREEN);
    fs.close();
  }

  private static String prop(String name) {
    return PROP_PREFIX + name;
  }

  private GraphicsConfiguration gc(int platformIdx) {
    return fs.gc(platformIdx);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Sort and layout
  //

  @Test
  public void testGetScreenSort() {
    assertEquals(List.of(gc(P), gc(R), gc(B)), Screens.getScreenSort());
  }

  @Test
  public void testGetScreenLayout() {
    List<List<GraphicsConfiguration>> layout = Screens.getScreenLayout();
    assertEquals(List.of(List.of(gc(P), gc(R)), List.of(gc(B))), layout);
    assertThrows(UnsupportedOperationException.class, () -> layout.add(List.of()));
    assertThrows(UnsupportedOperationException.class, () -> layout.get(0).add(gc(B)));
  }

  @Test
  public void testGetIndex_RowCol() {
    assertEquals(0, Screens.getIndex(0, 0));
    assertEquals(1, Screens.getIndex(0, 1));
    assertEquals(2, Screens.getIndex(1, 0));
  }

  @Test
  public void testGetIndex_RowCol_Pegged() {
    assertEquals(0, Screens.getIndex(-1, -1));
    assertEquals(0, Screens.getIndex(-5, 0));
    assertEquals(0, Screens.getIndex(0, -5));
    assertEquals(1, Screens.getIndex(0, 99));  // end of row 0
    assertEquals(1, Screens.getIndex(-1, 99));
    assertEquals(2, Screens.getIndex(1, 99));  // row 1 has one screen
    assertEquals(2, Screens.getIndex(99, 0));  // last row
    assertEquals(2, Screens.getIndex(99, 99));
  }

  @Test
  public void testGetIndex_RowCol_IsUsableWithGetScreen() {
    List<List<GraphicsConfiguration>> layout = Screens.getScreenLayout();
    for(int row = 0; row < layout.size(); row++)
      for(int col = 0; col < layout.get(row).size(); col++)
        assertSame(layout.get(row).get(col),
                   Screens.getScreen(Screens.getIndex(row, col)),
                   "row " + row + " col " + col);
  }

  @Test
  public void testGetScreen() {
    assertSame(gc(P), Screens.getScreen(0));
    assertSame(gc(R), Screens.getScreen(1));
    assertSame(gc(B), Screens.getScreen(2));
  }

  @Test
  public void testGetScreen_OutOfRangeIsDefault() {
    // make the default not be sorted index 0, so the check means something
    fs.setScreens(B, R_BOUNDS, B_BOUNDS, P_BOUNDS);
    assertSame(gc(B), Screens.getScreen(-1));
    assertSame(gc(B), Screens.getScreen(3));
    assertSame(gc(P), Screens.getScreen(0));
  }

  @Test
  public void testGetScreenBounds() {
    assertEquals(P_BOUNDS, Screens.getScreenBounds(0));
    assertEquals(R_BOUNDS, Screens.getScreenBounds(1));
    assertEquals(B_BOUNDS, Screens.getScreenBounds(2));
    assertEquals(P_BOUNDS, Screens.getScreenBounds(3)); // default
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Virtual
  //

  @Test
  public void testIsVirtualEnvironment() {
    assertTrue(Screens.isVirtualEnvironment());
  }

  @Test
  public void testIsVirtualEnvironment_SingleScreen() {
    fs.setScreens(0, P_BOUNDS);
    assertFalse(Screens.isVirtualEnvironment());

    fs.setScreens(0, R_BOUNDS); // single screen, not at origin
    assertTrue(Screens.isVirtualEnvironment());
  }

  //////////////////////////////////////////////////////////////////////
  //
  // getIndex(String)
  //

  @Test
  public void testGetIndex_String_Property() {
    System.setProperty(prop("idx"), "2");
    assertEquals(2, Screens.getIndex(prop("idx")));
    System.setProperty(prop("idx"), "-7");
    assertEquals(-7, Screens.getIndex(prop("idx")));
  }

  @Test
  public void testGetIndex_String_Missing() {
    assertEquals(-1, Screens.getIndex(prop("never.set")));
  }

  @Test
  public void testGetIndex_String_NotAnInt() {
    System.setProperty(prop("idx"), "two");
    assertEquals(-1, Screens.getIndex(prop("idx")));
    System.setProperty(prop("idx"), "");
    assertEquals(-1, Screens.getIndex(prop("idx")));
    System.setProperty(prop("idx"), " 1");
    assertEquals(-1, Screens.getIndex(prop("idx")));
  }

  @Test
  public void testGetIndex_String_EnvironmentVariable() {
    // Find some environment variable with an int value, if any.
    String name = System.getenv().entrySet().stream()
        .filter(e -> e.getValue().matches("-?\\d{1,9}"))
        .map(e -> e.getKey())
        .filter(k -> System.getProperty(k) == null)
        .findFirst().orElse(null);
    assumeTrue(name != null, "no environment variable with an int value");
    int expect = Integer.parseInt(System.getenv(name));
    assertEquals(expect, Screens.getIndex(name));

    // A property of the same name has precedence
    try {
      System.setProperty(name, String.valueOf(expect + 1));
      assertEquals(expect + 1, Screens.getIndex(name));
    } finally {
      System.clearProperty(name);
    }
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Preferred screen
  //

  @Test
  public void testGetPrefGraphicsDev_Default() {
    assertSame(fs.dev(P), Screens.getPrefGraphicsDev());
    assertSame(gc(P), Screens.getPrefScreen());
    assertEquals(P_BOUNDS, Screens.getPrefScreenBounds());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testGetPrefGraphicsConfiguration_Deprecated() {
    assertSame(Screens.getPrefScreen(), Screens.getPrefGraphicsConfiguration());
  }

  @Test
  public void testGetPrefGraphicsDev_JavaPreferredScreen() {
    System.setProperty(PREF_SCREEN, "2"); // sorted index
    assertSame(fs.dev(B), Screens.getPrefGraphicsDev());
  }

  @Test
  public void testGetPrefGraphicsDev_JavaPreferredScreen_OutOfRange() {
    System.setProperty(PREF_SCREEN, "3");
    assertSame(fs.dev(P), Screens.getPrefGraphicsDev());
  }

  @Test
  public void testSetPrefScreen_Int() {
    assertSame(gc(R), Screens.setPrefScreen(1));
    assertSame(fs.dev(R), Screens.getPrefGraphicsDev());
    assertSame(gc(R), Screens.getPrefScreen());
    assertEquals(R_BOUNDS, Screens.getPrefScreenBounds());

    assertSame(gc(B), Screens.setPrefScreen(2));
    assertSame(fs.dev(B), Screens.getPrefGraphicsDev());

    assertSame(gc(P), Screens.setPrefScreen(0));
    assertSame(fs.dev(P), Screens.getPrefGraphicsDev());
  }

  @Test
  public void testSetPrefScreen_Int_Bad() {
    Screens.setPrefScreen(1);
    assertNull(Screens.setPrefScreen(-1));
    assertNull(Screens.setPrefScreen(3));
    assertSame(fs.dev(R), Screens.getPrefGraphicsDev(), "unchanged");
  }

  @Test
  public void testSetPrefScreen_String() {
    String vari = prop("pref");
    assertNull(Screens.setPrefScreen(vari)); // not set

    System.setProperty(vari, "100");
    assertNull(Screens.setPrefScreen(vari));

    System.setProperty(vari, "junk");
    assertNull(Screens.setPrefScreen(vari));

    System.setProperty(vari, "1");
    assertSame(gc(R), Screens.setPrefScreen(vari));
    assertSame(fs.dev(R), Screens.getPrefGraphicsDev());

    System.setProperty(vari, "100");
    assertNull(Screens.setPrefScreen(vari));
    assertSame(fs.dev(R), Screens.getPrefGraphicsDev(), "unchanged");
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testSetPrefGraphicsDev_Deprecated() {
    assertSame(fs.dev(R), Screens.setPrefGraphicsDev(1));
    assertSame(fs.dev(R), Screens.getPrefGraphicsDev());
    assertNull(Screens.setPrefGraphicsDev(3));
    assertSame(fs.dev(R), Screens.getPrefGraphicsDev(), "unchanged");
    String vari = prop("pref");
    System.setProperty(vari, "2");
    assertSame(fs.dev(B), Screens.setPrefGraphicsDev(vari));
    assertSame(gc(B), Screens.getPrefScreen());
  }

  //////////////////////////////////////////////////////////////////////
  //
  // getGraphicsConfiguration(Point)
  //

  @Test
  public void testGetGraphicsConfiguration() {
    assertSame(gc(P), Screens.getGraphicsConfiguration(new Point(10, 10)));
    assertSame(gc(R), Screens.getGraphicsConfiguration(new Point(2000, 10)));
    assertSame(gc(B), Screens.getGraphicsConfiguration(new Point(10, 1100)));
  }

  @Test
  public void testGetGraphicsConfiguration_Edges() {
    // Rectangle.contains: x <= px < x+width
    assertSame(gc(P), Screens.getGraphicsConfiguration(new Point(0, 0)));
    assertSame(gc(P), Screens.getGraphicsConfiguration(new Point(1919, 1079)));
    assertSame(gc(R), Screens.getGraphicsConfiguration(new Point(1920, 0)));
    assertSame(gc(B), Screens.getGraphicsConfiguration(new Point(0, 1080)));
    assertSame(gc(R), Screens.getGraphicsConfiguration(new Point(4479, 1439)));
  }

  @Test
  public void testGetGraphicsConfiguration_NotOnAnyScreen() {
    assertSame(gc(P), Screens.getGraphicsConfiguration(new Point(-1, -1)));
    assertSame(gc(P), Screens.getGraphicsConfiguration(new Point(4480, 0)));
    assertSame(gc(P), Screens.getGraphicsConfiguration(new Point(2000, 2000)));
  }

  //////////////////////////////////////////////////////////////////////
  //
  // translate Point
  //

  @Test
  public void testTranslateToScreen_PointGc() {
    Point loc = new Point(100, 100); // on P
    Point result = Screens.translateToScreen(loc, gc(R));
    assertEquals(new Point(2020, 100), result);
    assertEquals(new Point(100, 100), loc, "argument not modified");

    result = Screens.translateToScreen(new Point(2020, 100), gc(B)); // R to B
    assertEquals(new Point(100, 1180), result);
  }

  @Test
  public void testTranslateToScreen_PointGc_AlreadyOnScreen() {
    Point loc = new Point(2020, 100); // on R
    assertEquals(loc, Screens.translateToScreen(loc, gc(R)));
  }

  /**
   * Returns a copy; contrast with
   * {@link #testTranslateToScreen_PointPoint_MutatesArgument()}.
   */
  @Test
  public void testTranslateToScreen_PointGc_ReturnsCopy() {
    Point loc = new Point(2020, 100);
    assertNotSame(loc, Screens.translateToScreen(loc, gc(R)));
    assertNotSame(loc, Screens.translateToScreen(loc, gc(P)));
  }

  @Test
  public void testTranslateToScreen_PointGc_FromOffScreen() {
    // A point on no screen is treated as being on the default screen.
    Point result = Screens.translateToScreen(new Point(2000, 2000), gc(B));
    assertEquals(new Point(2000, 3080), result);
  }

  @Test
  public void testTranslateToPrefScreen_Point() {
    assertEquals(new Point(100, 100), Screens.translateToPrefScreen(new Point(2020, 100)));
    Screens.setPrefScreen(2); // B
    assertEquals(new Point(100, 1180), Screens.translateToPrefScreen(new Point(2020, 100)));
  }

  @Test
  public void testTranslateToScreen_PointPoint() {
    Point loc = new Point(100, 100); // on P
    Point result = Screens.translateToScreen(loc, new Point(3000, 1300)); // on R
    assertEquals(new Point(2020, 100), result);

    result = Screens.translateToScreen(new Point(2020, 100), new Point(5, 2000)); // on B
    assertEquals(new Point(100, 1180), result);
  }

  @Test
  public void testTranslateToScreen_PointPoint_MutatesArgument() {
    Point loc = new Point(100, 100);
    Point result = Screens.translateToScreen(loc, new Point(3000, 1300));
    assertSame(loc, result);
    assertEquals(new Point(2020, 100), loc);
  }

  @Test
  public void testTranslateToScreen_PointPoint_SameScreen() {
    Point loc = new Point(2020, 100);
    Point result = Screens.translateToScreen(loc, new Point(3000, 1300));
    assertSame(loc, result);
    assertEquals(new Point(2020, 100), loc);
  }

  @Test
  public void testTranslateToScreen_PointPoint_NullTarget() {
    Point loc = new Point(2020, 100);
    assertSame(loc, Screens.translateToScreen(loc, (Point)null));
    assertEquals(new Point(2020, 100), loc);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // translate Window
  //

  private static Window window() {
    return FakeScreens.mockWindow(new Rectangle(100, 100, 800, 600)); // on P
  }

  @Test
  public void testTranslateToScreen_WindowGc() {
    Window w = window();
    Screens.translateToScreen(w, gc(R));
    assertEquals(new Point(2020, 100), w.getLocation());
    Screens.translateToScreen(w, gc(R)); // already there
    assertEquals(new Point(2020, 100), w.getLocation());
    Screens.translateToScreen(w, gc(B));
    assertEquals(new Point(100, 1180), w.getLocation());
  }

  @Test
  public void testTranslateToScreen_WindowPoint() {
    Window w = window();
    Screens.translateToScreen(w, new Point(3000, 1300));
    assertEquals(new Point(2020, 100), w.getLocation());
    Screens.translateToScreen(w, new Point(5, 2000));
    assertEquals(new Point(100, 1180), w.getLocation());
  }

  @Test
  public void testTranslateToScreen_WindowPoint_NullTarget() {
    Window w = window();
    Screens.translateToScreen(w, (Point)null);
    verify(w, never()).setLocation(any(Point.class));
    verify(w, never()).setLocation(anyInt(), anyInt());
    assertEquals(new Point(100, 100), w.getLocation());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testTranslateToPrefScreen_WindowPoint_Deprecated() {
    Window w = window();
    Screens.translateToPrefScreen(w, new Point(3000, 1300));
    assertEquals(new Point(2020, 100), w.getLocation());
    Screens.translateToPrefScreen(w, (Point)null);
    assertEquals(new Point(2020, 100), w.getLocation());
  }

  @Test
  public void testTranslateToPrefScreen_Window() {
    Window w = window();
    Screens.translateToPrefScreen(w); // pref is P, already there
    assertEquals(new Point(100, 100), w.getLocation());
    Screens.setPrefScreen(1); // R
    Screens.translateToPrefScreen(w);
    assertEquals(new Point(2020, 100), w.getLocation());
  }

  //////////////////////////////////////////////////////////////////////
  //
  // center Window
  //

  /** Where a window of size should be when centered on bounds. */
  private static Point centered(Rectangle bounds, int width, int height) {
    return new Point(bounds.x + (bounds.width - width) / 2,
                     bounds.y + (bounds.height - height) / 2);
  }

  @Test
  public void testCenterOnScreen() {
    Window w = FakeScreens.mockWindow(new Rectangle(3000, 1200, 800, 600)); // on R
    Screens.centerOnScreen(w, gc(P));
    assertEquals(centered(P_BOUNDS, 800, 600), w.getLocation());
    Screens.centerOnScreen(w, gc(R));
    assertEquals(centered(R_BOUNDS, 800, 600), w.getLocation());
    Screens.centerOnScreen(w, gc(B));
    assertEquals(centered(B_BOUNDS, 800, 600), w.getLocation());
    assertEquals(new Point(560, 1320), w.getLocation());
    assertEquals(new Rectangle(560, 1320, 800, 600), w.getBounds());
  }

  @Test
  public void testCenterOnPrefScreen() {
    Window w = window();
    Screens.centerOnPrefScreen(w);
    assertEquals(centered(P_BOUNDS, 800, 600), w.getLocation());
    Screens.setPrefScreen(1); // R
    Screens.centerOnPrefScreen(w);
    assertEquals(centered(R_BOUNDS, 800, 600), w.getLocation());
  }

  @Test
  public void testCenterOnScreen_WindowLargerThanScreen() {
    Window w = FakeScreens.mockWindow(new Rectangle(0, 0, 2000, 1200));
    Screens.centerOnScreen(w, gc(B));
    assertEquals(centered(B_BOUNDS, 2000, 1200), w.getLocation());
  }

  /**
   * centerOnScreen used to center relative to (0,0), then translate from
   * whatever screen that point happened to be on. When the window is larger
   * than the screen, the relative point is negative; if there is a screen
   * up and to the left, the translation was wrong: (1880,1020).
   */
  @Test
  public void testCenterOnScreen_WindowLargerThanScreen_ScreenUpLeft() {
    Rectangle upLeft = new Rectangle(-1920, -1080, 1920, 1080);
    fs.setScreens(0, P_BOUNDS, upLeft);
    Window w = FakeScreens.mockWindow(new Rectangle(0, 0, 2000, 1200));
    Screens.centerOnScreen(w, gc(0));
    assertEquals(centered(P_BOUNDS, 2000, 1200), w.getLocation());
  }

  @Test
  public void testCenteredLocation() {
    assertEquals(new Point(560, 240),
                 Screens.centeredLocation(P_BOUNDS, new Dimension(800, 600)));
    assertEquals(new Point(2800, 420),
                 Screens.centeredLocation(R_BOUNDS, new Dimension(800, 600)));
    assertEquals(new Point(-40, 1020),
                 Screens.centeredLocation(B_BOUNDS, new Dimension(2000, 1200)));
    assertEquals(new Point(-1920, -1080),
                 Screens.centeredLocation(new Rectangle(-1920, -1080, 1920, 1080),
                                          new Dimension(1920, 1080)));
    // odd difference rounds toward zero
    assertEquals(new Point(0, 0),
                 Screens.centeredLocation(new Rectangle(0, 0, 11, 11), new Dimension(10, 10)));
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Screen changes
  //

  @Test
  public void testScreenChange_Rearranged() {
    assertEquals(List.of(gc(P), gc(R), gc(B)), Screens.getScreenSort());

    // Move R to the left of P; R is still platform 0.
    fs.dev(R).setBounds(new Rectangle(-2560, 0, 2560, 1440));
    assertEquals(List.of(gc(R), gc(P), gc(B)), Screens.getScreenSort());
    assertEquals(List.of(List.of(gc(R), gc(P)), List.of(gc(B))),
                 Screens.getScreenLayout());
    assertSame(gc(R), Screens.getScreen(0));
    assertSame(gc(R), Screens.setPrefScreen(0));
  }

  @Test
  public void testScreenChange_ScreenRemoved() {
    assertEquals(3, Screens.getScreenSort().size());
    fs.setScreens(1, R_BOUNDS, P_BOUNDS); // B unplugged
    assertEquals(List.of(gc(1), gc(0)), Screens.getScreenSort());
    assertEquals(List.of(List.of(gc(1), gc(0))), Screens.getScreenLayout());
    assertNull(Screens.setPrefScreen(2));
    assertEquals(1, Screens.getIndex(99, 99));
  }

  /** The preferred device is re-evaluated when its screen goes away. */
  @Test
  public void testScreenChange_PreferredScreenRemoved() {
    assertSame(gc(B), Screens.setPrefScreen(2));
    fs.setScreens(1, R_BOUNDS, P_BOUNDS); // B unplugged
    assertSame(fs.dev(1), Screens.getPrefGraphicsDev(), "default");
    assertSame(fs.gc(1), Screens.getPrefScreen());
  }

  /** The preferred device is kept when the screens are rearranged. */
  @Test
  public void testScreenChange_PreferredScreenKept() {
    FakeScreens.FakeDevice r = fs.dev(R);
    assertSame(r.getDefaultConfiguration(), Screens.setPrefScreen(1));
    r.setBounds(new Rectangle(-2560, 0, 2560, 1440));
    assertSame(r, Screens.getPrefGraphicsDev());
    assertSame(r, Screens.getScreen(0).getDevice());
  }

  @Test
  public void testScreenChange_ScreenAdded() {
    Rectangle left = new Rectangle(-1280, 0, 1280, 1024);
    fs.setScreens(P, R_BOUNDS, B_BOUNDS, P_BOUNDS, left);
    assertEquals(List.of(gc(3), gc(P), gc(R), gc(B)), Screens.getScreenSort());
    assertEquals(List.of(List.of(gc(3), gc(P), gc(R)), List.of(gc(B))),
                 Screens.getScreenLayout());
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Never crash; stale and out-of-range indexes, empty layout, headless.
  //

  /** An index computed from an older, larger layout falls back. */
  @Test
  public void testStaleIndexFromLargerLayout() {
    int idx = Screens.getIndex(1, 0);
    assertEquals(2, idx);
    fs.setScreens(1, R_BOUNDS, P_BOUNDS); // B unplugged
    assertSame(gc(1), Screens.getScreen(idx), "default screen");
    assertEquals(P_BOUNDS, Screens.getScreenBounds(idx));
    assertNull(Screens.setPrefScreen(idx));
  }

  /** Out-of-range index from outside, through a property. */
  @Test
  public void testOutOfRangeIndexFromProperty() {
    System.setProperty(prop("idx"), "99");
    int idx = Screens.getIndex(prop("idx"));
    assertSame(gc(P), Screens.getScreen(idx));
    assertNull(Screens.setPrefScreen(prop("idx")));
    System.setProperty(PREF_SCREEN, "99");
    assertSame(fs.dev(P), Screens.getPrefGraphicsDev());
  }

  @Test
  public void testNoScreens_SortAndLayoutEmpty() {
    fs.setDevices(-1);
    assertEquals(List.of(), Screens.getScreenSort());
    assertEquals(List.of(), Screens.getScreenLayout());
    assertNull(Screens.setPrefScreen(0));
  }

  /** With an empty layout, getIndex(row, col) returns -1. */
  @Test
  public void testNoScreens_GetIndexRowCol() {
    fs.setDevices(-1);
    assertEquals(-1, Screens.getIndex(0, 0));
    assertEquals(-1, Screens.getIndex(-3, 3));
    assertEquals(-1, Screens.getIndex(3, 3));
  }

  @Test
  public void testNoScreens() {
    fs.setDevices(-1);
    assertNoScreens();
  }

  /** What is documented for no screens; same as headless. */
  private static void assertNoScreens() {
    assertFalse(Screens.hasScreens());
    assertFalse(Screens.isVirtualEnvironment());
    assertEquals(List.of(), Screens.getScreenSort());
    assertEquals(List.of(), Screens.getScreenLayout());
    assertEquals(-1, Screens.getIndex(0, 0));
    assertNull(Screens.setPrefScreen(0));
    assertNull(Screens.setPrefScreen(prop("never.set")));

    Point p = new Point(10, 20);
    assertEquals(p, Screens.translateToPrefScreen(p));
    assertSame(p, Screens.translateToScreen(p, new Point(3000, 0)));
    assertEquals(new Point(10, 20), p);

    assertThrows(HeadlessException.class, () -> Screens.getScreen(0));
    assertThrows(HeadlessException.class, () -> Screens.getScreenBounds(0));
    assertThrows(HeadlessException.class, () -> Screens.getPrefScreen());
    assertThrows(HeadlessException.class, () -> Screens.getPrefScreenBounds());
    assertThrows(HeadlessException.class, () -> Screens.getPrefGraphicsDev());
    assertThrows(HeadlessException.class, () -> Screens.getGraphicsConfiguration(p));
  }

  @Test
  public void testHasScreens() {
    assertTrue(Screens.hasScreens());
    fs.setDevices(-1);
    assertFalse(Screens.hasScreens());
    fs.setScreens(0, P_BOUNDS);
    assertTrue(Screens.hasScreens());
  }

  /**
   * The real environment, headless: behaves as no screens. Screens doesn't
   * touch the GraphicsEnvironment during class initialization, so this
   * can't make Screens unusable.
   */
  @Test
  public void testRealHeadless() {
    fs.close(); // back to the real environment
    try {
      assumeTrue(GraphicsEnvironment.isHeadless(), "not headless");
      assertNoScreens();
    } finally {
      fs = FakeScreens.open(P, R_BOUNDS, B_BOUNDS, P_BOUNDS); // for tearDown
    }
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Single screen
  //

  @Nested
  class SingleScreen {
    @BeforeEach
    public void setUp() {
      fs.close();
      fs = FakeScreens.open(0, P_BOUNDS);
    }

    @Test
    public void testSortAndLayout() {
      assertEquals(List.of(gc(0)), Screens.getScreenSort());
      assertEquals(List.of(List.of(gc(0))), Screens.getScreenLayout());
      assertEquals(0, Screens.getIndex(0, 0));
      assertEquals(0, Screens.getIndex(5, 5));
      assertFalse(Screens.isVirtualEnvironment());
    }

    @Test
    public void testTranslateAndCenter() {
      Point loc = new Point(100, 100);
      assertEquals(loc, Screens.translateToPrefScreen(new Point(loc)));
      Window w = window();
      Screens.centerOnPrefScreen(w);
      assertEquals(centered(P_BOUNDS, 800, 600), w.getLocation());
    }
  }
}
// vi: set sw=2 ts=8:
