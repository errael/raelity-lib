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
import java.util.Comparator;
import java.util.List;

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

  static final class ScreenItem {
    private final int index;
    private final Rectangle bounds;

    public ScreenItem(int index, Rectangle bounds) {
      this.index = index;
      this.bounds = new Rectangle(bounds);
    }

    /** Index into the platform-ordered array/list this item came from. */
    public int getIndex() {
      return index;
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
  public static List<ScreenItem> screenSort(List<Rectangle> screenBounds) {
    List<ScreenItem> items = new ArrayList<>(screenBounds.size());
    for(int i = 0; i < screenBounds.size(); i++)
      items.add(new ScreenItem(i, screenBounds.get(i)));
    items.sort(BY_Y);
    List<ScreenItem> result = new ArrayList<>(items.size());
    List<ScreenItem> row = new ArrayList<>();
    double rowBottom = 0;
    for(ScreenItem it : items) {
      if(!row.isEmpty() && it.getCenterY() >= rowBottom) {
        row.sort(BY_X);
        result.addAll(row);
        row.clear();
      }
      if(row.isEmpty())
        rowBottom = it.getMaxY();
      row.add(it);
    }
    row.sort(BY_X);
    result.addAll(row);
    return result;
  }

  /**
   * Return the GraphicsDevice from the screen sorted list.
   * @param screenIdx
   * @return
   */
  public static GraphicsDevice getGraphicsDevice(int screenIdx) {
    return changeDetector.getGraphicsDevice(screenIdx);
  }

  /**
   * Logical sort of the current screens. Each item's index refers to the
   * array from the same getScreenDevices() call, so fetch the devices and
   * the sort together if you need to map back to a GraphicsDevice.
   */
  public static List<GraphicsDevice> getScreenSort() {
    return changeDetector.getScreenSort();
  }

  public static List<Rectangle> boundsOf(GraphicsDevice[] devices) {
    List<Rectangle> bounds = new ArrayList<>(devices.length);
    for(GraphicsDevice d : devices)
      bounds.add(d.getDefaultConfiguration().getBounds());
    return bounds;
  }
  private static final ScreenChangeDetector changeDetector = new ScreenChangeDetector();
}
// vi: set sw=2 ts=8: