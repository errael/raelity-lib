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
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
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
import static org.mockito.Mockito.when;

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
  public void testGetScreen_Bounds() {
    assertEquals(P_BOUNDS, Screens.getScreen(0).getBounds());
    assertEquals(R_BOUNDS, Screens.getScreen(1).getBounds());
    assertEquals(B_BOUNDS, Screens.getScreen(2).getBounds());
    assertEquals(P_BOUNDS, Screens.getScreen(3).getBounds()); // default
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
    assertEquals(-1, Screens.getIndex(prop("idx")), "any negative is -1");
    System.setProperty(prop("idx"), "0");
    assertEquals(0, Screens.getIndex(prop("idx")));
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
    assertSame(fs.gc(P), Screens.getPrefScreen());
    assertSame(gc(P), Screens.getPrefScreen());
    assertEquals(P_BOUNDS, Screens.getPrefScreen().getBounds());
  }

  @Test
  public void testGetPrefGraphicsDev_JavaPreferredScreen() {
    System.setProperty(PREF_SCREEN, "2"); // sorted index
    assertSame(fs.gc(B), Screens.getPrefScreen());
  }

  @Test
  public void testGetPrefGraphicsDev_JavaPreferredScreen_OutOfRange() {
    System.setProperty(PREF_SCREEN, "3");
    assertSame(fs.gc(P), Screens.getPrefScreen());
  }

  @Test
  public void testSetPrefScreen_Int() {
    assertSame(gc(R), Screens.setPrefScreen(1));
    assertSame(fs.gc(R), Screens.getPrefScreen());
    assertSame(gc(R), Screens.getPrefScreen());
    assertEquals(R_BOUNDS, Screens.getPrefScreen().getBounds());

    assertSame(gc(B), Screens.setPrefScreen(2));
    assertSame(fs.gc(B), Screens.getPrefScreen());

    assertSame(gc(P), Screens.setPrefScreen(0));
    assertSame(fs.gc(P), Screens.getPrefScreen());
  }

  @Test
  public void testSetPrefScreen_Int_Bad() {
    Screens.setPrefScreen(1);
    assertNull(Screens.setPrefScreen(-1));
    assertNull(Screens.setPrefScreen(3));
    assertSame(fs.gc(R), Screens.getPrefScreen(), "unchanged");
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
    assertSame(fs.gc(R), Screens.getPrefScreen());

    System.setProperty(vari, "100");
    assertNull(Screens.setPrefScreen(vari));
    assertSame(fs.gc(R), Screens.getPrefScreen(), "unchanged");
  }

  //////////////////////////////////////////////////////////////////////
  //
  // getScreen(Point)
  //

  @Test
  public void testGetScreen_Point() {
    assertSame(gc(P), Screens.getScreen(new Point(10, 10)));
    assertSame(gc(R), Screens.getScreen(new Point(2000, 10)));
    assertSame(gc(B), Screens.getScreen(new Point(10, 1100)));
  }

  @Test
  public void testGetScreen_Point_Edges() {
    // Rectangle.contains: x <= px < x+width
    assertSame(gc(P), Screens.getScreen(new Point(0, 0)));
    assertSame(gc(P), Screens.getScreen(new Point(1919, 1079)));
    assertSame(gc(R), Screens.getScreen(new Point(1920, 0)));
    assertSame(gc(B), Screens.getScreen(new Point(0, 1080)));
    assertSame(gc(R), Screens.getScreen(new Point(4479, 1439)));
  }

  @Test
  public void testGetScreen_Point_NotOnAnyScreen() {
    // the nearest screen
    assertSame(gc(P), Screens.getScreen(new Point(-1, -1)));
    assertSame(gc(R), Screens.getScreen(new Point(4480, 0)));
    assertSame(gc(B), Screens.getScreen(new Point(2000, 2000)), "80 from B");
    assertSame(gc(R), Screens.getScreen(new Point(2500, 1500)), "60 from R");
    assertSame(gc(B), Screens.getScreen(new Point(100, 5000)));
    // tie: as far from B (x) as from R (y); first in sort order
    assertSame(gc(R), Screens.getScreen(new Point(1940, 1460)));
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

    result = Screens.translateToScreen(new Point(2020, 100), gc(B)); // R to B
    assertEquals(new Point(100, 1180), result);
  }

  @Test
  public void testTranslateToScreen_PointGc_AlreadyOnScreen() {
    Point loc = new Point(2020, 100); // on R
    assertSame(loc, Screens.translateToScreen(loc, gc(R)));
    assertEquals(new Point(2020, 100), loc);
  }

  /** Like AWT's translate, the argument is modified and returned. */
  @Test
  public void testTranslateToScreen_PointGc_MutatesArgument() {
    Point loc = new Point(2020, 100); // on R
    assertSame(loc, Screens.translateToScreen(loc, gc(P)));
    assertEquals(new Point(100, 100), loc);
    assertSame(loc, Screens.translateToScreen(loc, gc(B)));
    assertEquals(new Point(100, 1180), loc);
  }

  @Test
  public void testTranslateToScreen_PointGc_FromOffScreen() {
    // A point on no screen is treated as being on the nearest screen, B.
    Point result = Screens.translateToScreen(new Point(2000, 2000), gc(P));
    assertEquals(new Point(2000, 920), result);
    result = Screens.translateToScreen(new Point(2000, 2000), gc(B));
    assertEquals(new Point(2000, 2000), result, "already 'on' B");
  }

  @Test
  public void testTranslateToScreen_Point_PrefScreen() {
    assertEquals(new Point(100, 100), Screens.translateToScreen(new Point(2020, 100), Screens.getPrefScreen()));
    Screens.setPrefScreen(2); // B
    assertEquals(new Point(100, 1180), Screens.translateToScreen(new Point(2020, 100), Screens.getPrefScreen()));
  }

  @Test
  public void testTranslateToScreen_Point_PrefScreen_MutatesArgument() {
    Point loc = new Point(2020, 100); // on R, pref is P
    assertSame(loc, Screens.translateToScreen(loc, Screens.getPrefScreen()));
    assertEquals(new Point(100, 100), loc);
  }

  @Test
  public void testTranslateToScreen_Point_ScreenOfPoint() {
    Point loc = new Point(100, 100); // on P
    Point result = Screens.translateToScreen(loc, Screens.getScreen(new Point(3000, 1300))); // on R
    assertEquals(new Point(2020, 100), result);

    result = Screens.translateToScreen(new Point(2020, 100), Screens.getScreen(new Point(5, 2000))); // on B
    assertEquals(new Point(100, 1180), result);
  }

  @Test
  public void testTranslateToScreen_Point_ScreenOfPoint_MutatesArgument() {
    Point loc = new Point(100, 100);
    Point result = Screens.translateToScreen(loc, Screens.getScreen(new Point(3000, 1300)));
    assertSame(loc, result);
    assertEquals(new Point(2020, 100), loc);
  }

  @Test
  public void testTranslateToScreen_Point_ScreenOfPoint_SameScreen() {
    Point loc = new Point(2020, 100);
    Point result = Screens.translateToScreen(loc, Screens.getScreen(new Point(3000, 1300)));
    assertSame(loc, result);
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
  public void testTranslateToScreen_Window_ScreenOfPoint() {
    Window w = window();
    Screens.translateToScreen(w, Screens.getScreen(new Point(3000, 1300)));
    assertEquals(new Point(2020, 100), w.getLocation());
    Screens.translateToScreen(w, Screens.getScreen(new Point(5, 2000)));
    assertEquals(new Point(100, 1180), w.getLocation());
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

  /** Centered in the usable area, not under the taskbar/panel. */
  @Test
  public void testCenterOnScreen_Insets() {
    fs.dev(P).setInsets(new Insets(30, 100, 50, 0)); // usable (100,30,1820,1000)
    Window w = FakeScreens.mockWindow(rect(0, 0, 800, 600));
    Screens.centerOnScreen(w, gc(P));
    assertEquals(new Point(100 + 510, 30 + 200), w.getLocation());
    assertTrue(Screens.isWithinScreen(w, gc(P)));

    fs.dev(R).setInsets(new Insets(0, 0, 40, 0));
    Screens.centerOnScreen(w, gc(R));
    assertEquals(centered(rect(1920, 0, 2560, 1400), 800, 600), w.getLocation());

    Screens.setPrefScreen(0); // P
    Screens.centerOnScreen(w, Screens.getPrefScreen());
    assertEquals(new Point(610, 230), w.getLocation());
  }

  @Test
  public void testCenterOnScreen_PrefScreen() {
    Window w = window();
    Screens.centerOnScreen(w, Screens.getPrefScreen());
    assertEquals(centered(P_BOUNDS, 800, 600), w.getLocation());
    Screens.setPrefScreen(1); // R
    Screens.centerOnScreen(w, Screens.getPrefScreen());
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
  // Which screen a window is on; within and fit.
  //

  private static Rectangle rect(int x, int y, int w, int h) {
    return new Rectangle(x, y, w, h);
  }

  @Test
  public void testGetScreen_Rectangle() {
    assertSame(gc(P), Screens.getScreen(rect(100, 100, 800, 600)));
    assertSame(gc(B), Screens.getScreen(rect(100, 1200, 800, 600)));
    // straddles P and R, mostly R
    assertSame(gc(R), Screens.getScreen(rect(1800, 100, 800, 600)));
    // top-left corner on P, but mostly on R
    assertSame(gc(R), Screens.getScreen(rect(1900, 100, 800, 600)));
    // straddles P, R and B: overlaps 9600, 112000, 38400
    assertSame(gc(R), Screens.getScreen(rect(1800, 1000, 400, 400)));
  }

  @Test
  public void testGetScreen_Rectangle_Tie() {
    // equal overlap of P and B; first in sort order
    assertSame(gc(P), Screens.getScreen(rect(100, 980, 100, 200)));
  }

  @Test
  public void testGetScreen_Rectangle_NoOverlap() {
    // nearest the center
    assertSame(gc(R), Screens.getScreen(rect(5000, 100, 100, 100)));
    assertSame(gc(B), Screens.getScreen(rect(100, 3000, 100, 100)));
    assertSame(gc(P), Screens.getScreen(rect(-500, -500, 100, 100)));
    // in the gap right of B, below P; nearest is B, then R
    assertSame(gc(B), Screens.getScreen(rect(1930, 2000, 10, 10)));
  }

  @Test
  public void testGetScreen_Rectangle_Empty() {
    // no size, works like a point
    assertSame(gc(R), Screens.getScreen(rect(2000, 10, 0, 0)));
    assertSame(gc(B), Screens.getScreen(rect(10, 1100, 0, 0)));
    // on the edge between P and R, like getScreen(Point)
    assertSame(gc(R), Screens.getScreen(rect(1920, 10, 0, 0)));
    assertSame(Screens.getScreen(new Point(1920, 10)), Screens.getScreen(rect(1920, 10, 0, 0)));
  }

  @Test
  public void testGetScreen_Window() {
    assertSame(gc(P), Screens.getScreen(FakeScreens.mockWindow(rect(100, 100, 800, 600))));
    assertSame(gc(R), Screens.getScreen(FakeScreens.mockWindow(rect(1900, 100, 800, 600))));
    assertSame(gc(B), Screens.getScreen(FakeScreens.mockWindow(rect(100, 5000, 800, 600))));
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Window methods and the event dispatch thread
  //

  private static Window displayableWindow() {
    Window w = FakeScreens.mockWindow(rect(100, 100, 800, 600));
    when(w.isDisplayable()).thenReturn(true);
    return w;
  }

  /** A displayable window changed off the EDT throws. */
  @Test
  public void testEDT_DisplayableOffEDT() {
    assertFalse(EventQueue.isDispatchThread());
    Window w = displayableWindow();
    assertThrows(IllegalStateException.class, () -> Screens.translateToScreen(w, gc(R)));
    assertThrows(IllegalStateException.class, () -> Screens.translateToPrefScreen(w));
    assertThrows(IllegalStateException.class, () -> Screens.centerOnScreen(w, gc(R)));
    assertThrows(IllegalStateException.class, () -> Screens.fitOnScreen(w, gc(R)));
    assertThrows(IllegalStateException.class, () -> Screens.fitOnScreen(w, gc(R), true));
    assertEquals(new Point(100, 100), w.getLocation(), "not moved");
    // queries are fine
    assertSame(gc(P), Screens.getScreen(w));
    assertTrue(Screens.isWithinScreen(w, gc(P)));
    assertTrue(Screens.canFitWithinScreen(w, gc(P)));
  }

  /** A displayable window changed on the EDT is fine. */
  @Test
  public void testEDT_DisplayableOnEDT() throws Exception {
    Window w = displayableWindow();
    EventQueue.invokeAndWait(() -> {
      Screens.translateToScreen(w, gc(R));
      Screens.fitOnScreen(w, gc(R), true);
      Screens.centerOnScreen(w, gc(B));
      Screens.translateToPrefScreen(w);
    });
    assertEquals(new Point(560, 240), w.getLocation());
  }

  /** Not displayable, for example a new frame not yet shown: any thread. */
  @Test
  public void testEDT_NotDisplayable() {
    Window w = FakeScreens.mockWindow(rect(100, 100, 800, 600));
    assertFalse(w.isDisplayable());
    Screens.centerOnScreen(w, gc(R));
    assertEquals(centered(R_BOUNDS, 800, 600), w.getLocation());
  }

  /** The window is on the screen it overlaps most, not its corner's screen. */
  @Test
  public void testTranslateToScreen_WindowMostlyOnOtherScreen() {
    Window w = FakeScreens.mockWindow(rect(1900, 100, 800, 600)); // mostly R
    Screens.translateToScreen(w, gc(R));
    assertEquals(new Point(1900, 100), w.getLocation(), "already on R");
    Screens.translateToScreen(w, gc(P));
    assertEquals(new Point(-20, 100), w.getLocation(), "R to P");

    w = FakeScreens.mockWindow(rect(1900, 100, 800, 600));
    Screens.translateToScreen(w, gc(B));
    assertEquals(new Point(-20, 1180), w.getLocation(), "R to B");

    w = FakeScreens.mockWindow(rect(1900, 100, 800, 600));
    Screens.translateToScreen(w, Screens.getScreen(new Point(3000, 1300))); // target on R
    assertEquals(new Point(1900, 100), w.getLocation());
  }

  /** A configuration of the same device is the same screen. */
  @Test
  public void testTranslateToScreen_WindowOtherConfigSameDevice() {
    FakeScreens.FakeConfig otherR = new FakeScreens.FakeConfig(fs.dev(R), R_BOUNDS);
    assertNotSame(gc(R), otherR);
    Window w = FakeScreens.mockWindow(rect(2000, 100, 800, 600));
    Screens.translateToScreen(w, otherR);
    assertEquals(new Point(2000, 100), w.getLocation());
  }

  @Test
  public void testGetUsableBounds() {
    assertEquals(P_BOUNDS, Screens.getUsableBounds(gc(P)));
    fs.dev(P).setInsets(new Insets(30, 10, 40, 50));
    assertEquals(rect(10, 30, 1860, 1010), Screens.getUsableBounds(gc(P)));
    fs.dev(R).setInsets(new Insets(0, 0, 2000, 0)); // bigger than the screen
    assertEquals(rect(1920, 0, 2560, 0), Screens.getUsableBounds(gc(R)));
  }

  @Test
  public void testIsWithinScreen() {
    Window w = FakeScreens.mockWindow(rect(100, 100, 800, 600));
    assertTrue(Screens.isWithinScreen(w, gc(P)));
    assertFalse(Screens.isWithinScreen(w, gc(R)));
    assertTrue(Screens.isWithinScreen(P_BOUNDS, gc(P)), "exact fit");
    assertFalse(Screens.isWithinScreen(rect(1900, 100, 800, 600), gc(P)));
    assertFalse(Screens.isWithinScreen(rect(1900, 100, 800, 600), gc(R)));
    fs.dev(P).setInsets(new Insets(150, 0, 0, 0));
    assertFalse(Screens.isWithinScreen(w, gc(P)), "under the panel");
  }

  @Test
  public void testCanFitWithinScreen() {
    assertTrue(Screens.canFitWithinScreen(new Dimension(1920, 1080), gc(P)));
    assertFalse(Screens.canFitWithinScreen(new Dimension(1921, 1080), gc(P)));
    assertFalse(Screens.canFitWithinScreen(new Dimension(1920, 1081), gc(P)));
    Window w = FakeScreens.mockWindow(rect(0, 0, 2000, 1200));
    assertFalse(Screens.canFitWithinScreen(w, gc(P)));
    assertTrue(Screens.canFitWithinScreen(w, gc(R)));
    fs.dev(P).setInsets(new Insets(0, 0, 40, 0));
    assertFalse(Screens.canFitWithinScreen(new Dimension(1920, 1080), gc(P)));
    assertTrue(Screens.canFitWithinScreen(new Dimension(1920, 1040), gc(P)));
  }

  @Test
  public void testFitOnScreen_Move() {
    Window w = FakeScreens.mockWindow(rect(-20, 100, 800, 600));
    Screens.fitOnScreen(w, gc(P));
    assertEquals(rect(0, 100, 800, 600), w.getBounds());

    w = FakeScreens.mockWindow(rect(1500, 900, 800, 600)); // off bottom right
    Screens.fitOnScreen(w, gc(P));
    assertEquals(rect(1120, 480, 800, 600), w.getBounds());
    verify(w, never()).setSize(any(Dimension.class));

    fs.dev(P).setInsets(new Insets(0, 0, 40, 0));
    Screens.fitOnScreen(w, gc(P));
    assertEquals(rect(1120, 440, 800, 600), w.getBounds(), "above the taskbar");
  }

  @Test
  public void testFitOnScreen_AlreadyWithin() {
    Window w = FakeScreens.mockWindow(rect(100, 100, 800, 600));
    Screens.fitOnScreen(w, gc(P), true);
    assertEquals(rect(100, 100, 800, 600), w.getBounds());
    verify(w, never()).setLocation(any(Point.class));
    verify(w, never()).setSize(any(Dimension.class));
  }

  @Test
  public void testFitOnScreen_TooBig() {
    Window w = FakeScreens.mockWindow(rect(100, 100, 2000, 1200));
    Screens.fitOnScreen(w, gc(P));
    assertEquals(rect(0, 0, 2000, 1200), w.getBounds(), "left/top edge");
    verify(w, never()).setSize(any(Dimension.class));

    w = FakeScreens.mockWindow(rect(100, 100, 2000, 1200));
    Screens.fitOnScreen(w, gc(P), true);
    assertEquals(P_BOUNDS, w.getBounds(), "resized");

    fs.dev(P).setInsets(new Insets(30, 0, 0, 0));
    w = FakeScreens.mockWindow(rect(100, 100, 2000, 1200));
    Screens.fitOnScreen(w, gc(P), true);
    assertEquals(rect(0, 30, 1920, 1050), w.getBounds());

    w = FakeScreens.mockWindow(rect(100, 100, 2000, 500)); // only too wide
    Screens.fitOnScreen(w, gc(P), true);
    assertEquals(rect(0, 100, 1920, 500), w.getBounds());
  }

  /** The kind of thing a program might do after a translate. */
  @Test
  public void testTranslateThenFit() {
    Window w = FakeScreens.mockWindow(rect(3600, 1000, 800, 400)); // on R
    Screens.translateToScreen(w, gc(P));
    assertEquals(rect(1680, 1000, 800, 400), w.getBounds());
    assertFalse(Screens.isWithinScreen(w, gc(P)));
    assertTrue(Screens.canFitWithinScreen(w, gc(P)));
    Screens.fitOnScreen(w, gc(P));
    assertEquals(rect(1120, 680, 800, 400), w.getBounds());
    assertTrue(Screens.isWithinScreen(w, gc(P)));
  }

  @Test
  public void testFittedBounds() {
    Rectangle area = rect(-1000, -500, 1000, 500);
    assertEquals(rect(-900, -400, 100, 100),
                 Screens.fittedBounds(rect(-900, -400, 100, 100), area, false));
    assertEquals(rect(-1000, -500, 100, 100),
                 Screens.fittedBounds(rect(-5000, -5000, 100, 100), area, false));
    assertEquals(rect(-100, -100, 100, 100),
                 Screens.fittedBounds(rect(5000, 5000, 100, 100), area, false));
    assertEquals(rect(-1000, -200, 2000, 100),
                 Screens.fittedBounds(rect(0, -200, 2000, 100), area, false));
    assertEquals(rect(-1000, -200, 1000, 100),
                 Screens.fittedBounds(rect(0, -200, 2000, 100), area, true));
    Rectangle in = rect(0, 0, 10, 10);
    Screens.fittedBounds(in, area, true);
    assertEquals(rect(0, 0, 10, 10), in, "argument not modified");
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

  @Test
  public void testGetGeneration() {
    int gen = Screens.getGeneration();
    assertEquals(gen, Screens.getGeneration(), "no change");
    Screens.getScreenSort();
    assertEquals(gen, Screens.getGeneration(), "no change");
    fs.dev(R).setBounds(new Rectangle(-2560, 0, 2560, 1440));
    int gen2 = Screens.getGeneration();
    assertNotEquals(gen, gen2, "moved");
    fs.setScreens(P, R_BOUNDS, B_BOUNDS, P_BOUNDS);
    assertNotEquals(gen2, Screens.getGeneration(), "new devices");
  }

  /** The pattern in the class comment. */
  @Test
  public void testGetGeneration_SeveralCalls() {
    int gen = Screens.getGeneration();
    List<List<GraphicsConfiguration>> layout = Screens.getScreenLayout();
    GraphicsConfiguration gc = Screens.getScreen(Screens.getIndex(1, 0));
    assertEquals(gen, Screens.getGeneration());
    assertSame(layout.get(1).get(0), gc);

    gen = Screens.getGeneration();
    layout = Screens.getScreenLayout();
    fs.setScreens(1, R_BOUNDS, P_BOUNDS); // B unplugged between calls
    gc = Screens.getScreen(Screens.getIndex(1, 0));
    assertNotEquals(gen, Screens.getGeneration(), "caller should start over");
  }

  /** The preferred device is re-evaluated when its screen goes away. */
  @Test
  public void testScreenChange_PreferredScreenRemoved() {
    assertSame(gc(B), Screens.setPrefScreen(2));
    fs.setScreens(1, R_BOUNDS, P_BOUNDS); // B unplugged
    assertSame(fs.gc(1), Screens.getPrefScreen(), "default");
    assertSame(fs.gc(1), Screens.getPrefScreen());
  }

  /** The preferred device is kept when the screens are rearranged. */
  @Test
  public void testScreenChange_PreferredScreenKept() {
    FakeScreens.FakeDevice r = fs.dev(R);
    assertSame(r.getDefaultConfiguration(), Screens.setPrefScreen(1));
    r.setBounds(new Rectangle(-2560, 0, 2560, 1440));
    assertSame(r.getDefaultConfiguration(), Screens.getPrefScreen());
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
    assertEquals(P_BOUNDS, Screens.getScreen(idx).getBounds());
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
    assertSame(fs.gc(P), Screens.getPrefScreen());
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
    assertSame(p, Screens.translateToScreen(p, new FakeScreens.FakeDevice("x",
        new Rectangle(5000, 0, 100, 100)).getDefaultConfiguration()));
    assertEquals(new Point(10, 20), p);

    assertThrows(HeadlessException.class, () -> Screens.getScreen(0));
    assertThrows(HeadlessException.class, () -> Screens.getPrefScreen());
    assertThrows(HeadlessException.class, () -> Screens.getScreen(window()));
    assertThrows(HeadlessException.class, () -> Screens.getScreen(p));
    assertThrows(HeadlessException.class,
                 () -> Screens.getScreen(new Rectangle(0, 0, 10, 10)));
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
      assertEquals(loc, Screens.translateToScreen(new Point(loc), Screens.getPrefScreen()));
      Window w = window();
      Screens.centerOnScreen(w, Screens.getPrefScreen());
      assertEquals(centered(P_BOUNDS, 800, 600), w.getLocation());
    }
  }
}
// vi: set sw=2 ts=8:
