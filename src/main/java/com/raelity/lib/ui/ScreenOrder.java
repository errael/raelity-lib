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
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

//
// ScreenOrder - thanks to claude for something to start with
//
/**
 * Logical ordering of screens: rows top to bottom, screens left to right
 * within a row. A screen joins the current row if its center y is above the
 * bottom of the row's first (topmost-centered) screen.
 */
final class ScreenOrder {
  private ScreenOrder() {
  }

  /** Only written to by sort method */
  static final class ScreenItem {
    private final int index; // 0 to nScreen-1; an index into the original list
    private final Rectangle bounds;
    private int rowIndex; // 0 to nRows-1; result of the sort.

    ScreenItem(int index, Rectangle bounds) {
      this.index = index;
      this.bounds = new Rectangle(bounds);
    }

    /** Index into the platform-ordered array/list this item came from. */
    public int getIndex() {
      return index;
    }

    /** Sorted row index */
    public int getRowIndex() {
      return rowIndex;
    }

    /** Defensive copy; the sort uses the accessors below instead. */
    public Rectangle getBounds() {
      return new Rectangle(bounds);
    }

    public double getCenterX() {
      return bounds.getCenterX();
    }

    public double getCenterY() {
      return bounds.getCenterY();
    }

    public double getMaxY() {
      return bounds.getMaxY();
    }

    @Override
    public String toString() {
      return "ScreenItem[" + index + ", " + bounds + "]";
    }
  }
  // List.sort is stable, so ties keep their prior order; no tie-breaker needed.
  private static final Comparator<ScreenItem> BY_Y
      = Comparator.comparingDouble(ScreenItem::getCenterY);
  private static final Comparator<ScreenItem> BY_X
      = Comparator.comparingDouble(ScreenItem::getCenterX);

  /** Testable core: takes the screen bounds in platform order. */
  static List<ScreenItem> screenSort(List<Rectangle> screenBounds) {
    List<ScreenItem> items = new ArrayList<>(screenBounds.size());
    for(int i = 0; i < screenBounds.size(); i++)
      items.add(new ScreenItem(i, screenBounds.get(i)));
    items.sort(BY_Y);
    List<ScreenItem> result = new ArrayList<>(items.size());
    List<ScreenItem> row = new ArrayList<>();
    double rowBottom = 0;
    int rowIndex = 0;
    for(ScreenItem it : items) {
      if(!row.isEmpty() && it.getCenterY() >= rowBottom) {
        row.sort(BY_X);
        result.addAll(row);
        row.clear();
        rowIndex++;
      }
      if(row.isEmpty())
        rowBottom = it.getMaxY();
      it.rowIndex = rowIndex;
      row.add(it);
    }
    row.sort(BY_X);
    result.addAll(row);
    return result;
  }

  /** Return rows of indexes into system's dev array, intermediate step here for testing */
  static List<List<Integer>> screenLayoutIndex(List<ScreenItem> screenItemSort) {
    int rowIndex = -1;
    List<List<Integer>> screenLayoutIndex = new ArrayList<>(5);
    for(ScreenItem screenItem : screenItemSort) {
      if (rowIndex != screenItem.getRowIndex()) {
        rowIndex = screenItem.getRowIndex();
        screenLayoutIndex.add(new ArrayList<>());
      }
      screenLayoutIndex.get(screenLayoutIndex.size()-1).add(screenItem.getIndex());
    }
    return screenLayoutIndex;
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Access to the screens. Every use of the graphics environment goes
  // through the detector; each method reads the detector field once.
  //

  private static volatile ScreenChangeDetector detector
      = ScreenChangeDetector.createDefault();

  /**
   * Use these device sources instead of the local GraphicsEnvironment;
   * for tests. A fresh detector is created, so no cached state, including
   * the preferred device, carries over.
   * @param devices the screens in platform order
   * @param defaultDevice the default screen
   */
  static void setDeviceSources(Supplier<GraphicsDevice[]> devices,
                               Supplier<GraphicsDevice> defaultDevice) {
    detector = new ScreenChangeDetector(devices, defaultDevice);
  }

  /** Go back to the local GraphicsEnvironment, with a fresh detector. */
  static void resetDeviceSources() {
    detector = ScreenChangeDetector.createDefault();
  }

  static ScreenChangeDetector detector() {
    return detector;
  }

  /** The current screens; a consistent snapshot. */
  static ScreenChangeDetector.Snapshot snapshot() {
    return detector.snapshot();
  }

  /**
   * Return the GraphicsDevice from the screen sorted list.
   * @param screenIdx index into sorted list
   * @return the device, null if out of range
   */
  public static GraphicsDevice getGraphicsDevice(int screenIdx) {
    return detector.getGraphicsDevice(screenIdx);
  }

  /**
   * Logical sort of the current screens.
   * @return unmodifiable list
   */
  public static List<GraphicsConfiguration> getScreenSort() {
    return detector.getScreenSort();
  }

  /** @return unmodifiable list of unmodifiable rows */
  public static List<List<GraphicsConfiguration>> getScreenLayout() {
    return detector.getScreenLayout();
  }

  /** @return the default screen device, null if no screens (headless) */
  static GraphicsDevice getDefaultScreenDevice() {
    return detector.getDefaultScreenDevice();
  }

  public static List<Rectangle> screenBoundsOf(GraphicsDevice[] devices) {
    List<Rectangle> bounds = new ArrayList<>(devices.length);
    for(GraphicsDevice d : devices)
      bounds.add(d.getDefaultConfiguration().getBounds());
    return bounds;
  }
}
// vi: set sw=2 ts=8: