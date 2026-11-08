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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.List;

/**
 * Support for multi-screen environments; there are methods for moving things
 * around between screens. They operate on the preferred
 * screen and/or screens referenced by a string name or index.
 * This class sorts the screens by their bounding box,
 * {@link GraphicsConfiguration#getBounds() GraphicsDevice.getDefaultConfiguration().getBounds()}.
 * See {@link #getScreenSort()} and {@link #getScreenLayout()}.
 * A screen/GraphicsConfiguration is referenced by an index; additionally a string
 * can be used. A string is searched as a System.property, then an environment variable;
 * when found it is converted to an integer index.
 * <p>
 * In this class a screen is a {@link GraphicsConfiguration}, the
 * {@link GraphicsDevice#getDefaultConfiguration() default configuration}
 * of a screen device; use {@link GraphicsConfiguration#getDevice()} as needed.
 * <p>
 * <b>Usage.</b> Get a screen, then pass it to an operation.
 * Get a screen with {@link #getScreen(int)} (an index into
 * {@link #getScreenSort()}, see also {@link #getIndex(int, int)} and
 * {@link #getIndex(String)}), {@link #getScreen(Point)},
 * {@link #getScreen(Rectangle)}, {@link #getScreen(Window)} or
 * {@link #getPrefScreen()}. The operations translate, center, check and fit;
 * for example:
 * <pre>
 *   Screens.translateToScreen(window, Screens.getScreen(otherWindow));
 *   Screens.centerOnScreen(dialog, Screens.getPrefScreen());
 *   GraphicsConfiguration gc = Screens.getScreen(Screens.getIndex(1, 0));
 *   Screens.translateToScreen(window, gc);
 *   if(!Screens.isWithinScreen(window, gc))
 *     Screens.fitOnScreen(window, gc, !Screens.canFitWithinScreen(window, gc));
 * </pre>
 * <p>
 * The preferred screen is set with {@link #setPrefScreen(String)} or
 * {@link #setPrefScreen(int)}.
 * If not set it defaults to "JAVA_PREFERRED_SCREEN";
 * if the index is not present or out of bound then
 * {@link GraphicsEnvironment#getDefaultScreenDevice()}.{@link GraphicsDevice#getDefaultConfiguration() getDefaultConfiguration()}
 * is used.
 * <p>
 * The screens are checked for changes on each call; an index from an earlier
 * call may be out of range, it is handled like any other out of range index.
 * <p>
 * <b>No screens.</b> When there are no screens, for example
 * {@link GraphicsEnvironment#isHeadless() headless}, {@link #hasScreens()}
 * is false; the lists are empty, the index methods return -1,
 * {@link #setPrefScreen(int)} returns null and
 * {@link #translateToScreen(Point, GraphicsConfiguration)} returns the
 * location unchanged. Methods that
 * must return a screen or bounds throw {@link HeadlessException}.
 * <p>
 * <b>Threads and changes.</b> The screen information is an immutable snapshot,
 * refreshed when the screens change; the methods that only query screens
 * can be used from any thread. The preferred screen is shared; it is kept
 * in a volatile field. Each call looks at the current screens, so two calls
 * may see different screens if they change in between. To do several
 * things against the same screens, compare {@link #getGeneration()} before
 * and after; if it changed, start over.
 * Methods that change a {@link Window} must, like any AWT/Swing component
 * change, be called on the event dispatch thread once the window is
 * displayable; they throw IllegalStateException if not.
 */
public class Screens {
  private Screens() {
  }

  /**
   * The screens' generation; it changes whenever the screens change,
   * for example a screen added, removed, moved or resized.
   * Use it to check that several calls saw the same screens;
   * see the class comment.
   * @return the current generation
   */
  public static int getGeneration() {
    return ScreenOrder.snapshot().generation();
  }

  /**
   * Check if there are any screens; there are none when headless.
   * When false, methods that return a screen throw HeadlessException.
   * @return true if there is at least one screen
   */
  public static boolean hasScreens() {
    return ScreenOrder.snapshot().size() > 0;
  }

  /**
   * A virtual environment has a screen whose origin is not (0,0).
   * @return true if virtual
   */
  public static boolean isVirtualEnvironment() {
    return ScreenOrder.snapshot().isVirtual();
  }

  /**
   * The screens in sorted order; see {@link #getScreenLayout() }.
   * The elements are obtained by {@link GraphicsDevice#getDefaultConfiguration() }.
   * @return unmodifiable list of screens, empty if no screens
   */
  public static List<GraphicsConfiguration> getScreenSort() {
    return ScreenOrder.getScreenSort();
  }

  /**
   * The screens are laid out as a list of rows;
   * each row is a list of {@link GraphicsConfiguration}s (screens) left-to-right.
   * The elements are obtained by {@link GraphicsDevice#getDefaultConfiguration() }.
   * @return unmodifiable list of unmodifiable rows, empty if no screens
   */
  public static List<List<GraphicsConfiguration>> getScreenLayout() {
    return ScreenOrder.getScreenLayout();
  }

  /**
   * Return index into linear list of screens.
   * Both row and indexInRow are pegged at any edge if out of range.
   * @param _row row in {@link #getScreenLayout()}
   * @param _indexInRow index in the row
   * @return index into {@link #getScreenSort()}, -1 if no screens
   */
  public static int getIndex(int _row, int _indexInRow) {
    List<List<GraphicsConfiguration>> screenLayout = getScreenLayout();
    if(screenLayout.isEmpty())
      return -1;
    int row = Math.max(0, Math.min(_row, screenLayout.size() - 1));
    // index into single/linear list
    int index = 0;
    for(int rowIndex = 0; rowIndex < row; rowIndex++)
      index += screenLayout.get(rowIndex).size();
    int rowSize = screenLayout.get(row).size();
    int indexInRow = Math.max(0, Math.min(_indexInRow, rowSize - 1));
    return index + indexInRow;
  }

  /**
   * The preferred screen, used by the methods in this class that don't take
   * a screen. If not set by {@link #setPrefScreen(int)}, then if there's a
   * System property or environment variable "JAVA_PREFERRED_SCREEN", use
   * that as an index into {@link #getScreenSort()}; otherwise use the
   * default screen. If the preferred screen is no longer one of the
   * screens, this is re-evaluated.
   *
   * @return the preferred screen
   * @throws HeadlessException if no screens
   */
  public static GraphicsConfiguration getPrefScreen() {
    return prefDevice().getDefaultConfiguration();
  }

  /**
   * Get the indexed GraphicsConfiguration in sorted list; if index is out
   * of range, get the default screen's GraphicConfiguration.
   * <p>
   * Note that a bad index is not reported; the default screen is returned.
   * An index from an earlier call is out of range if screens were removed.
   * @param index index into sorted list
   * @return the indexed GraphicsConfiguration in sorted list, or default
   * @throws HeadlessException if no screens
   */
  public static GraphicsConfiguration getScreen(int index) {
    List<GraphicsConfiguration> screenSort = ScreenOrder.getScreenSort();
    if (index < 0 || index >= screenSort.size())
      return defaultScreenDevice().getDefaultConfiguration();
    return screenSort.get(index);
  }

  /** @throws HeadlessException if no screens */
  private static GraphicsDevice defaultScreenDevice() {
    GraphicsDevice gdev = ScreenOrder.getDefaultScreenDevice();
    if(gdev == null)
      throw new HeadlessException("No screens");
    return gdev;
  }

  /**
   * First check for System property, then evironment variable. Convert the
   * first one found's value to an integer and return it.
   * @param vari
   * @return index, or -1 if not found, not an integer, or negative
   */
  public static int getIndex(String vari) {
    int screenIdx = -1;
    String sint;
    sint = System.getProperty(vari);
    if(sint == null)
      sint = System.getenv(vari);
    if(sint != null) {
      try {
        screenIdx = Integer.parseInt(sint);
      } catch(NumberFormatException ex) {
      }
    }
    return screenIdx < 0 ? -1 : screenIdx;
  }

  /** If the Window is not on the preferred screen,
   * then translate it to the preferred screen.
   * See {@link #translateToScreen(Window, GraphicsConfiguration)}.
   *
   * @param window
   * @throws HeadlessException if no screens
   * @throws IllegalStateException if window is displayable and not on the EDT
   */
  public static void translateToPrefScreen(Window window) {
    checkEDT(window);
    translateToScreen(window, getPrefScreen());
  }

  /** If the Window is not on the specified screen,
   * then translate it to the specified screen.
   * The Window is on the screen it overlaps the most, see
   * {@link #getScreen(Rectangle)}. It keeps its offset from
   * the screen's origin; it may not be completely within the new screen,
   * see {@link #isWithinScreen(Window, GraphicsConfiguration)} and
   * {@link #fitOnScreen(Window, GraphicsConfiguration)}.
   *
   * @param window
   * @param gc
   * @throws IllegalStateException if window is displayable and not on the EDT
   */
  public static void translateToScreen(Window window, GraphicsConfiguration gc) {
    checkEDT(window);
    window.setLocation(translatedLocation(window.getBounds(), gc));
  }

  /**
   * Methods that change a Window must be called on the event dispatch thread
   * once the window is displayable (has a native peer). Before that, for
   * example positioning a new frame before it is shown, any thread is
   * allowed.
   * @throws IllegalStateException if window is displayable and not on the EDT
   */
  private static void checkEDT(Window window) {
    if(window.isDisplayable() && !EventQueue.isDispatchThread())
      throw new IllegalStateException(
          "Screens: displayable Window changed off the event dispatch thread");
  }

  /**
   * The location of bounds after translating it to gc; see
   * {@link #translateToScreen(Window, GraphicsConfiguration)}.
   * @param bounds a window's bounds
   * @param gc target screen
   * @return the new location; unchanged if already on gc or no screens
   */
  static Point translatedLocation(Rectangle bounds, GraphicsConfiguration gc) {
    Point location = bounds.getLocation();
    if(!hasScreens())
      return location;
    GraphicsConfiguration from = getScreen(bounds);
    // Compare devices, not configurations; a caller's gc, for example from
    // window.getGraphicsConfiguration(), may not be the default configuration.
    if(from.getDevice() != gc.getDevice()) {
      Rectangle fromBounds = from.getBounds();
      Rectangle toBounds = gc.getBounds();
      location.translate(toBounds.x - fromBounds.x, toBounds.y - fromBounds.y);
    }
    return location;
  }

  /** If the location is not on the specified screen, then translate it (the
   * same object) to the specified screen.
   *
   * @param location a point on the screen
   * @param gc
   * @return return for convenience, same object as argument;
   *         unchanged if no screens
   */
  public static Point translateToScreen(Point location, GraphicsConfiguration gc) {
    Rectangle targetScreenBounds = gc.getBounds();
    if(!targetScreenBounds.contains(location) && hasScreens()) {
      Rectangle locScreenBounds = getScreen(location).getBounds();
      location.translate(targetScreenBounds.x - locScreenBounds.x,
                         targetScreenBounds.y - locScreenBounds.y);
    }
    return location;
  }

  /**
   * Center the window on the specified screen's usable area,
   * see {@link #getUsableBounds(GraphicsConfiguration)}.
   * A window bigger than the usable area extends past it equally on
   * both sides.
   *
   * @param window
   * @param gc target screen
   * @throws IllegalStateException if window is displayable and not on the EDT
   */
  public static void centerOnScreen(Window window, GraphicsConfiguration gc) {
    checkEDT(window);
    window.setLocation(centeredLocation(getUsableBounds(gc), window.getSize()));
  }

  /**
   * The location to center something of the given size in the bounds.
   * @param bounds center in this
   * @param size size of thing to center
   * @return the location
   */
  static Point centeredLocation(Rectangle bounds, Dimension size) {
    return new Point(bounds.x + (bounds.width - size.width) / 2,
                     bounds.y + (bounds.height - size.height) / 2);
  }

  /** See {@link #getPrefScreen()}; @throws HeadlessException if no screens */
  private static GraphicsDevice prefDevice() {
    ScreenChangeDetector detector = ScreenOrder.detector();
    GraphicsDevice gdev = detector.getPreferredDevice();
    if(gdev == null || !detector.snapshot().contains(gdev)) {
      gdev = setPrefDevice(detector, getIndex("JAVA_PREFERRED_SCREEN"));
      if(gdev == null) {
        gdev = detector.getDefaultScreenDevice();
        if(gdev == null)
          throw new HeadlessException("No screens");
        detector.setPreferredDevice(gdev);
      }
    }
    return gdev;
  }

  /**
   * This sets the preferred screen, used by the methods in this class,
   * according the value of the specified String. First check System properties,
   * if not found then check environment variables.
   * If neither is found, or the value is not a valid index,
   * then the existing preferred screen is not changed.
   * <p>
   * Null is returned whether the variable is not set, is not an integer,
   * or is out of range; in each case the preferred screen is unchanged.
   * To distinguish, check the property or environment variable directly.
   *
   * @param vari check this property/environment variable's value
   * @return the new preferred screen, or null if unchanged
   */
  public static GraphicsConfiguration setPrefScreen(String vari) {
    return setPrefScreen(getIndex(vari));
  }

  /**
   * This sets the preferred screen, used by the methods in this class,
   * according the specified value. If null is returned, then the existing
   * preferred screen is not changed.
   *
   * @param screenIdx index into {@link #getScreenSort()}
   * @return the new preferred screen or null if bad argument
   */
  public static GraphicsConfiguration setPrefScreen(int screenIdx) {
    GraphicsDevice gdev = setPrefDevice(ScreenOrder.detector(), screenIdx);
    return gdev == null ? null : gdev.getDefaultConfiguration();
  }

  private static GraphicsDevice setPrefDevice(ScreenChangeDetector detector,
                                             int screenIdx) {
    GraphicsDevice gdev = detector.getGraphicsDevice(screenIdx);
    if(gdev != null)
      detector.setPreferredDevice(gdev);
    return gdev;
  }

  /**
   * Find the screen that contains the given point; if no screen contains
   * it, the screen nearest the point. Ties go to the first in
   * {@link #getScreenSort()}.
   *
   * @param location
   * @return the screen containing location, else the nearest screen
   * @throws HeadlessException if no screens
   */
  public static GraphicsConfiguration getScreen(Point location) {
    List<GraphicsConfiguration> screenSort = ScreenOrder.getScreenSort();
    GraphicsConfiguration gc = screenAt(screenSort, location.x, location.y);
    return gc != null ? gc : nearestScreen(screenSort, location.x, location.y);
  }

  /** @return the first screen containing the point, null if none */
  private static GraphicsConfiguration screenAt(List<GraphicsConfiguration> screenSort,
                                                double x, double y) {
    for(GraphicsConfiguration config : screenSort) {
      if(config.getBounds().contains(x, y))
        return config;
    }
    return null;
  }

  /**
   * @return the screen nearest the point, first in sort order for a tie
   * @throws HeadlessException if no screens
   */
  private static GraphicsConfiguration nearestScreen(List<GraphicsConfiguration> screenSort,
                                                     double x, double y) {
    GraphicsConfiguration best = null;
    double bestDist = Double.MAX_VALUE;
    for(GraphicsConfiguration config : screenSort) {
      Rectangle r = config.getBounds();
      double dx = Math.max(0, Math.max(r.x - x, x - r.getMaxX()));
      double dy = Math.max(0, Math.max(r.y - y, y - r.getMaxY()));
      double dist = dx * dx + dy * dy;
      if(dist < bestDist) {
        bestDist = dist;
        best = config;
      }
    }
    if(best == null)
      throw new HeadlessException("No screens");
    return best;
  }

  /**
   * Find the screen the window is on; see {@link #getScreen(Rectangle)}.
   * @param window
   * @return the screen the window is on
   * @throws HeadlessException if no screens
   */
  public static GraphicsConfiguration getScreen(Window window) {
    return getScreen(window.getBounds());
  }

  /**
   * Find the screen that a window with these bounds is on: the screen it
   * overlaps the most; if it doesn't overlap any screen, then as
   * {@link #getScreen(Point)} of its center.
   * Ties go to the first in {@link #getScreenSort()}.
   *
   * @param bounds for example a window's bounds
   * @return the screen the bounds are on
   * @throws HeadlessException if no screens
   */
  public static GraphicsConfiguration getScreen(Rectangle bounds) {
    List<GraphicsConfiguration> screenSort = ScreenOrder.getScreenSort();
    GraphicsConfiguration best = null;
    long bestArea = 0;
    for(GraphicsConfiguration config : screenSort) {
      Rectangle overlap = config.getBounds().intersection(bounds);
      if(!overlap.isEmpty()) {
        long area = (long)overlap.width * overlap.height;
        if(area > bestArea) {
          bestArea = area;
          best = config;
        }
      }
    }
    if(best != null)
      return best;
    // No overlap, for example empty bounds; like getScreen(Point) of center.
    double cx = bounds.getCenterX();
    double cy = bounds.getCenterY();
    best = screenAt(screenSort, cx, cy);
    return best != null ? best : nearestScreen(screenSort, cx, cy);
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Is a window within a screen; fitting a window on a screen.
  // These use the screen's usable area, the bounds less the insets
  // (taskbars, panels).
  //

  /**
   * The screen's bounds less its insets; the area where a window should go.
   * @param gc the screen
   * @return the usable bounds
   */
  public static Rectangle getUsableBounds(GraphicsConfiguration gc) {
    Rectangle b = gc.getBounds();
    Insets in = ScreenOrder.getScreenInsets(gc);
    return new Rectangle(b.x + in.left, b.y + in.top,
                         Math.max(0, b.width - in.left - in.right),
                         Math.max(0, b.height - in.top - in.bottom));
  }

  /**
   * Is the window completely within the screen's usable area; for example,
   * after a translate, check if {@link #fitOnScreen} might be wanted.
   * @param window
   * @param gc the screen
   * @return true if within
   */
  public static boolean isWithinScreen(Window window, GraphicsConfiguration gc) {
    return isWithinScreen(window.getBounds(), gc);
  }

  /**
   * Are the bounds completely within the screen's usable area.
   * @param bounds
   * @param gc the screen
   * @return true if within
   */
  public static boolean isWithinScreen(Rectangle bounds, GraphicsConfiguration gc) {
    return getUsableBounds(gc).contains(bounds);
  }

  /**
   * Can the window fit within the screen's usable area without being
   * resized; if not, {@link #fitOnScreen(Window, GraphicsConfiguration, boolean)}
   * with resize might be wanted.
   * @param window
   * @param gc the screen
   * @return true if it fits
   */
  public static boolean canFitWithinScreen(Window window, GraphicsConfiguration gc) {
    return canFitWithinScreen(window.getSize(), gc);
  }

  /**
   * Can something of this size fit within the screen's usable area.
   * @param size
   * @param gc the screen
   * @return true if it fits
   */
  public static boolean canFitWithinScreen(Dimension size, GraphicsConfiguration gc) {
    Rectangle usable = getUsableBounds(gc);
    return size.width <= usable.width && size.height <= usable.height;
  }

  /**
   * Move the window, if needed, so that it is within the screen's usable
   * area; the size is not changed. A window that is too big is placed at
   * the usable area's left and/or top edge.
   * @param window
   * @param gc the screen
   * @throws IllegalStateException if window is displayable and not on the EDT
   */
  public static void fitOnScreen(Window window, GraphicsConfiguration gc) {
    fitOnScreen(window, gc, false);
  }

  /**
   * Move the window, if needed, so that it is within the screen's usable
   * area. If resize, a window that is too big is first made smaller to fit;
   * otherwise it is placed at the usable area's left and/or top edge.
   * @param window
   * @param gc the screen
   * @param resize if true, the window may be made smaller
   * @throws IllegalStateException if window is displayable and not on the EDT
   */
  public static void fitOnScreen(Window window, GraphicsConfiguration gc,
                                 boolean resize) {
    checkEDT(window);
    Rectangle bounds = window.getBounds();
    Rectangle fitted = fittedBounds(bounds, getUsableBounds(gc), resize);
    if(!fitted.getSize().equals(bounds.getSize()))
      window.setSize(fitted.getSize());
    if(!fitted.getLocation().equals(bounds.getLocation()))
      window.setLocation(fitted.getLocation());
  }

  /**
   * The bounds moved, and if resize made smaller, to be within area.
   * Too big without resize: placed at area's left/top edge.
   * @param bounds
   * @param area
   * @param resize
   * @return new bounds
   */
  static Rectangle fittedBounds(Rectangle bounds, Rectangle area, boolean resize) {
    Rectangle r = new Rectangle(bounds);
    if(resize) {
      r.width = Math.min(r.width, area.width);
      r.height = Math.min(r.height, area.height);
    }
    r.x = Math.max(area.x, Math.min(r.x, area.x + area.width - r.width));
    r.y = Math.max(area.y, Math.min(r.y, area.y + area.height - r.height));
    return r;
  }
}
// vi: set sw=2 ts=8:
