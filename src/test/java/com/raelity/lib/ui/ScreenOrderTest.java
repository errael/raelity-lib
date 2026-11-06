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

import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.raelity.lib.ui.ScreenOrder.ScreenItem;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the pure sorting code in {@link ScreenOrder}; no
 * GraphicsEnvironment is needed except for {@link #testScreenBoundsOf()}.
 */
public class ScreenOrderTest {

  static final int K1 = 1000;
  static final int K2 = 2000;

  /**
   * Each box is 2K wide, 1K high.
   * The params are the x,y position in units of box width/height.
   */
  static ScreenItem r(int index, double x, double y) {
    return new ScreenItem(index, new Rectangle((int)(x*K2), (int)(y*K1), K2, K1));
  }

  // This is the expected result ordering.
  private final List<ScreenItem> initialScreenItems
      = List.of(
          r(0, 0, 0), r(1, 1, 0),  r(2, 2, 0),  r(3, 3, 0),
          r(4, 0, 1), r(5, 1, 1),  r(6, 2, 1),  r(7, 3, 1),
          r(8, 0, 2), r(9, 1, 2), r(10, 2, 2), r(11, 3, 2));
  // When the screens are sorted expect 0 - 11 from initialScreenItems
  private final List<Integer> expectedOrder = IntStream.range(0, 12).boxed().toList();
  private final List<List<Integer>> expectedLayoutIndexes = List.of(
      List.of(0, 1, 2, 3),
      List.of(4, 5, 6, 7),
      List.of(8, 9, 10, 11));

  /** Sort, return platform indexes in sorted order. */
  private static List<Integer> sortedIndexes(Rectangle... bounds) {
    return ScreenOrder.screenSort(List.of(bounds)).stream()
        .map(ScreenItem::getIndex).toList();
  }

  /** Sort, return layout of platform indexes. */
  private static List<List<Integer>> layout(Rectangle... bounds) {
    return ScreenOrder.screenLayoutIndex(ScreenOrder.screenSort(List.of(bounds)));
  }

  /**
   * Shuffle a 4x3 grid many ways, check that the sort and layout
   * always recover the grid.
   */
  @Test
  public void testScreenSort_ShuffledGrid() {
    Random random = new Random(37);
    for(int trial = 0; trial < 50; trial++) {
      List<ScreenItem> randomItems = new ArrayList<>(initialScreenItems);
      Collections.shuffle(randomItems, random);
      List<Rectangle> randomRects = randomItems.stream().map(ScreenItem::getBounds).toList();

      List<ScreenItem> sorted = ScreenOrder.screenSort(randomRects);
      // A sorted item's index is into randomRects (same order as randomItems)
      List<Integer> result = sorted.stream()
          .map(sortedItem -> randomItems.get(sortedItem.getIndex()).getIndex())
          .toList();
      assertEquals(expectedOrder, result, "trial " + trial);

      List<List<Integer>> layoutIndexes = ScreenOrder.screenLayoutIndex(sorted).stream()
          .map(row -> row.stream().map(idx -> randomItems.get(idx).getIndex()).toList())
          .toList();
      assertEquals(expectedLayoutIndexes, layoutIndexes, "trial " + trial);
    }
  }

  @Test
  public void testScreenSort_RowIndex() {
    List<ScreenItem> sorted = ScreenOrder.screenSort(
        initialScreenItems.stream().map(ScreenItem::getBounds).toList());
    for(ScreenItem item : sorted)
      assertEquals(item.getIndex() / 4, item.getRowIndex(), item.toString());
  }

  @Test
  public void testScreenSort_Empty() {
    assertEquals(List.of(), ScreenOrder.screenSort(List.of()));
    assertEquals(List.of(), ScreenOrder.screenLayoutIndex(List.of()));
  }

  @Test
  public void testScreenSort_Single() {
    Rectangle b = new Rectangle(-100, 50, 1920, 1080);
    List<ScreenItem> sorted = ScreenOrder.screenSort(List.of(b));
    assertEquals(1, sorted.size());
    assertEquals(0, sorted.get(0).getIndex());
    assertEquals(0, sorted.get(0).getRowIndex());
    assertEquals(b, sorted.get(0).getBounds());
    assertEquals(List.of(List.of(0)), ScreenOrder.screenLayoutIndex(sorted));
  }

  @Test
  public void testScreenSort_HorizontalReversed() {
    Rectangle a = new Rectangle(0, 0, 1920, 1080);
    Rectangle b = new Rectangle(-1920, 0, 1920, 1080);
    Rectangle c = new Rectangle(-3840, 0, 1920, 1080);
    assertEquals(List.of(2, 1, 0), sortedIndexes(a, b, c));
    assertEquals(List.of(List.of(2, 1, 0)), layout(a, b, c));
  }

  @Test
  public void testScreenSort_Vertical() {
    Rectangle a = new Rectangle(0, 1080, 1920, 1080);
    Rectangle b = new Rectangle(0, 0, 1920, 1080);
    Rectangle c = new Rectangle(0, -1080, 1920, 1080);
    assertEquals(List.of(2, 1, 0), sortedIndexes(a, b, c));
    assertEquals(List.of(List.of(2), List.of(1), List.of(0)), layout(a, b, c));
  }

  /**
   * Screens of different sizes and vertical offsets; a screen joins the row
   * when its center y is above the bottom of the row's first screen.
   */
  @Test
  public void testScreenSort_StaggeredRow() {
    Rectangle laptop = new Rectangle(0, 600, 1440, 900);      // cy 1050
    Rectangle big = new Rectangle(1440, 0, 2560, 1440);       // cy 720, first in row
    Rectangle portrait = new Rectangle(4000, 0, 1080, 1920);  // cy 960
    // row bottom is big's bottom 1440; all centers are above that.
    assertEquals(List.of(List.of(0, 1, 2)), layout(laptop, big, portrait));
  }

  @Test
  public void testScreenSort_RowBreakIsCenterAtRowBottom() {
    Rectangle top = new Rectangle(0, 0, 1000, 1000);          // bottom 1000
    Rectangle justIn = new Rectangle(1000, 499, 1000, 1000);  // cy 999
    Rectangle justOut = new Rectangle(2000, 500, 1000, 1000); // cy 1000
    assertEquals(List.of(List.of(0, 1), List.of(2)), layout(top, justIn, justOut));
  }

  /**
   * A screen to the right but low enough is a new row, even though the
   * screen to its left overlaps it vertically.
   */
  @Test
  public void testScreenSort_DescendingStaircase() {
    Rectangle a = new Rectangle(0, 0, 1920, 1080);      // cy 540, bottom 1080
    Rectangle b = new Rectangle(1920, 500, 1920, 1080); // cy 1040, row 0
    Rectangle c = new Rectangle(3840, 1000, 1920, 1080);// cy 1540, row 1
    assertEquals(List.of(List.of(1, 2), List.of(0)), layout(c, a, b));
  }

  /** Identical bounds (mirrored/cloned displays) keep platform order. */
  @Test
  public void testScreenSort_Ties_IdenticalBounds() {
    Rectangle a = new Rectangle(0, 0, 1920, 1080);
    assertEquals(List.of(0, 1, 2), sortedIndexes(a, new Rectangle(a), new Rectangle(a)));
    assertEquals(List.of(List.of(0, 1, 2)), layout(a, new Rectangle(a), new Rectangle(a)));
  }

  /** Mirrored pair in a row with another screen; ties keep platform order. */
  @Test
  public void testScreenSort_Ties_MirroredPairInRow() {
    Rectangle right = new Rectangle(1920, 0, 1920, 1080);
    Rectangle left = new Rectangle(0, 0, 1920, 1080);
    assertEquals(List.of(1, 2, 0), sortedIndexes(right, left, new Rectangle(left)));
    assertEquals(List.of(0, 2, 1), sortedIndexes(new Rectangle(left), right, left));
  }

  /** Same center y, different x; row sort by x, not platform order. */
  @Test
  public void testScreenSort_Ties_SameCenterY() {
    Rectangle a = new Rectangle(3840, 0, 1920, 1080);
    Rectangle b = new Rectangle(0, 0, 1920, 1080);
    Rectangle c = new Rectangle(1920, 0, 1920, 1080);
    assertEquals(List.of(1, 2, 0), sortedIndexes(a, b, c));
  }

  /** 2x2 grid where the primary is bottom right; negative coordinates. */
  @Test
  public void testScreenSort_Grid2x2_Negative() {
    Rectangle br = new Rectangle(0, 0, 1920, 1080);
    Rectangle tl = new Rectangle(-1920, -1080, 1920, 1080);
    Rectangle bl = new Rectangle(-1920, 0, 1920, 1080);
    Rectangle tr = new Rectangle(0, -1080, 1920, 1080);
    assertEquals(List.of(List.of(1, 3), List.of(2, 0)), layout(br, tl, bl, tr));
  }

  @Test
  public void testScreenSort_DoesNotModifyInput() {
    List<Rectangle> in = new ArrayList<>(List.of(
        new Rectangle(1920, 0, 1920, 1080), new Rectangle(0, 0, 1920, 1080)));
    List<Rectangle> copy = in.stream().map(Rectangle::new).toList();
    List<ScreenItem> sorted = ScreenOrder.screenSort(in);
    assertEquals(copy, in);
    // ScreenItem copies the bounds; changes to the input don't affect it.
    in.get(0).setLocation(-5000, -5000);
    assertEquals(copy.get(0), sorted.get(1).getBounds());
    // getBounds is a defensive copy
    sorted.get(1).getBounds().setLocation(7, 7);
    assertEquals(copy.get(0), sorted.get(1).getBounds());
  }

  @Test
  public void testScreenItemAccessors() {
    ScreenItem item = new ScreenItem(3, new Rectangle(100, 200, 1000, 500));
    assertEquals(3, item.getIndex());
    assertEquals(600.0, item.getCenterX());
    assertEquals(450.0, item.getCenterY());
    assertEquals(700.0, item.getMaxY());
    assertTrue(item.toString().contains("3"));
  }

  @Test
  public void testScreenBoundsOf() {
    Rectangle a = new Rectangle(1920, 0, 1920, 1080);
    Rectangle b = new Rectangle(0, 0, 1920, 1080);
    try(FakeScreens fs = FakeScreens.open(1, a, b)) {
      GraphicsDevice[] devs = fs.env().getScreenDevices();
      assertEquals(List.of(a, b), ScreenOrder.screenBoundsOf(devs));
      assertEquals(List.of(), ScreenOrder.screenBoundsOf(new GraphicsDevice[0]));
    }
  }
}
// vi: set sw=2 ts=8:
