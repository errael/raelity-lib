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

import java.awt.Rectangle;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ScreenChangeDetector}; uses a private detector instance,
 * not the one shared by {@link ScreenOrder}/{@link Screens}.
 */
public class ScreenChangeDetectorTest {

  static final Rectangle LEFT = new Rectangle(0, 0, 1920, 1080);
  static final Rectangle RIGHT = new Rectangle(1920, 0, 1920, 1080);
  static final Rectangle BELOW = new Rectangle(0, 1080, 1920, 1080);

  private FakeScreens fs;
  private ScreenChangeDetector detector;

  @BeforeEach
  public void setUp() {
    fs = FakeScreens.open(1, RIGHT, LEFT);
    detector = new ScreenChangeDetector();
  }

  @AfterEach
  public void tearDown() {
    fs.close();
  }

  @Test
  public void testPoll_NoChange() {
    int gen = detector.poll();
    assertEquals(gen, detector.poll());
    assertEquals(gen, detector.poll());
  }

  @Test
  public void testPoll_FirstPollIsAChange() {
    assertEquals(1, detector.poll());
  }

  @Test
  public void testPoll_BoundsChanged() {
    int gen = detector.poll();
    fs.dev(0).setBounds(new Rectangle(1920, 0, 2560, 1440)); // resolution
    int gen2 = detector.poll();
    assertNotEquals(gen, gen2);
    assertEquals(gen2, detector.poll());

    fs.dev(0).setBounds(new Rectangle(-2560, 0, 2560, 1440)); // moved
    assertNotEquals(gen2, detector.poll());
  }

  @Test
  public void testPoll_ScreenAddedRemoved() {
    int gen = detector.poll();
    fs.setScreens(1, RIGHT, LEFT, BELOW);
    int gen2 = detector.poll();
    assertNotEquals(gen, gen2);
    fs.setScreens(0, LEFT);
    assertNotEquals(gen2, detector.poll());
  }

  @Test
  public void testPoll_PlatformOrderChanged() {
    int gen = detector.poll();
    fs.setScreens(0, LEFT, RIGHT); // same screens, different platform order
    assertNotEquals(gen, detector.poll());
    assertEquals(List.of(fs.gc(0), fs.gc(1)), detector.getScreenSort());
  }

  /**
   * The same device objects, reordered in the platform array; the bounds
   * list differs by position, so a change is seen and the sort's indexes
   * match the new array.
   */
  @Test
  public void testPoll_SameDevicesReordered() {
    FakeScreens.FakeDevice right = fs.dev(0), left = fs.dev(1);
    int gen = detector.poll();
    assertSame(left, detector.getGraphicsDevice(0));
    fs.setDevices(0, left, right);
    assertNotEquals(gen, detector.poll());
    assertSame(left, detector.getGraphicsDevice(0));
    assertSame(right, detector.getGraphicsDevice(1));
    assertEquals(List.of(left.getDefaultConfiguration(), right.getDefaultConfiguration()),
                 detector.getScreenSort());
  }

  /** Same device objects, rearranged by setBounds; sort follows bounds. */
  @Test
  public void testPoll_SameDevicesRearranged() {
    FakeScreens.FakeDevice right = fs.dev(0), left = fs.dev(1);
    detector.poll();
    right.setBounds(new Rectangle(-1920, 0, 1920, 1080)); // now left of "left"
    assertSame(right, detector.getGraphicsDevice(0));
    assertSame(left, detector.getGraphicsDevice(1));
    assertEquals(List.of(List.of(right.getDefaultConfiguration(),
                                 left.getDefaultConfiguration())),
                 detector.getScreenLayout());
  }

  @Test
  public void testNoScreens() {
    fs.setDevices(-1);
    assertEquals(List.of(), detector.getScreenSort());
    assertEquals(List.of(), detector.getScreenLayout());
    assertNull(detector.getGraphicsDevice(0));
  }

  /**
   * By design only bounds are compared. If the devices are replaced
   * by new device objects with the same bounds, no change is detected and
   * the old device objects continue to be returned.
   */
  @Test
  public void testPoll_NewDevicesSameBounds() {
    int gen = detector.poll();
    FakeScreens.FakeDevice oldLeft = fs.dev(1);
    fs.setScreens(1, RIGHT, LEFT);
    assertEquals(gen, detector.poll());
    assertSame(oldLeft, detector.getGraphicsDevice(0));
    assertNotSame(fs.dev(1), detector.getGraphicsDevice(0));
  }

  @Test
  public void testGetScreenSort() {
    assertEquals(List.of(fs.gc(1), fs.gc(0)), detector.getScreenSort());
    // a new list each time, caller may modify it
    List<?> sort = detector.getScreenSort();
    assertNotSame(sort, detector.getScreenSort());
    sort.clear();
    assertEquals(2, detector.getScreenSort().size());
  }

  @Test
  public void testGetScreenLayout() {
    assertEquals(List.of(List.of(fs.gc(1), fs.gc(0))), detector.getScreenLayout());
    fs.setScreens(1, RIGHT, LEFT, BELOW);
    assertEquals(List.of(List.of(fs.gc(1), fs.gc(0)), List.of(fs.gc(2))),
                 detector.getScreenLayout());
    // unchanged, the same unmodifiable list is returned
    assertSame(detector.getScreenLayout(), detector.getScreenLayout());
  }

  @Test
  public void testGetGraphicsDevice() {
    assertSame(fs.dev(1), detector.getGraphicsDevice(0));
    assertSame(fs.dev(0), detector.getGraphicsDevice(1));
    assertNull(detector.getGraphicsDevice(-1));
    assertNull(detector.getGraphicsDevice(2));
  }

  @Test
  public void testGetGraphicsDevice_AfterChange() {
    assertNull(detector.getGraphicsDevice(2));
    fs.setScreens(1, RIGHT, LEFT, BELOW);
    assertSame(fs.dev(2), detector.getGraphicsDevice(2));
  }

  @Test
  public void testPoll_UpdatesScreensVirtual() {
    fs.setScreens(0, LEFT);
    detector.poll();
    assertFalse(Screens.isVirtualEnvironment());
    fs.setScreens(0, LEFT, RIGHT);
    detector.poll();
    assertTrue(Screens.isVirtualEnvironment());
  }
}
// vi: set sw=2 ts=8:
