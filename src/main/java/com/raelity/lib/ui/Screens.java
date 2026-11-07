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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
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
 * Note, in this class screen typically refers to a {@link GraphicsConfiguration},
 * use {@link GraphicsConfiguration#getDevice()} as needed.
 * Many methods have two forms, one operates on a specified screen and the other
 * that operates on the default screen. The default can be set
 * with {@link #setPrefScreen(String)} or {@link #setPrefScreen(int)}.
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
 * the Point translate methods return the location unchanged. Methods that
 * must return a screen, device or bounds throw {@link HeadlessException}.
 */
// TODO: should examine/validate for multithreading
public class Screens {
  private Screens() {
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
   * @return default graphics configuration of the preferred device
   * @throws HeadlessException if no screens
   * @deprecated use getPrefScreen
   */
  @Deprecated
  public static GraphicsConfiguration getPrefGraphicsConfiguration() {
    return getPrefScreen();
  }

  /**
   * @return default graphics configuration of the preferred device
   * @throws HeadlessException if no screens
   */
  public static GraphicsConfiguration getPrefScreen() {
    return getPrefGraphicsDev().getDefaultConfiguration();
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
   * @return index or -1 if not found or conversion to in fails
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
    return screenIdx;
  }

  /** Using the location of the Window, if it is not on the preferred screen,
   * then translate it to the preferred screen.
   *
   * @param window
   * @throws HeadlessException if no screens
   */
  public static void translateToPrefScreen(Window window) {
    translateToScreen(window, getPrefScreen());
  }

  /** Using the location of the Window, if it is not on the specified screen,
   * then translate it to the specified screen.
   *
   * @param window
   * @param gc
   */
  public static void translateToScreen(Window window, GraphicsConfiguration gc) {
    window.setLocation(translateToScreen(window.getLocation(), gc));
  }

  /** If Window is not on the same screen as target, then translate Window to
   * target's screen.
   *
   * @param window
   * @param target
   * @deprecated use translateToScreen(window, target)
   */
  @Deprecated
  public static void translateToPrefScreen(Window window, Point target) {
    translateToScreen(window, target);
  }

  /** If Window is not on the same screen as target, then translate Window to
   * target's screen.
   *
   * @param window
   * @param target
   */
  public static void translateToScreen(Window window, Point target) {
    if(target == null)
      return;
    window.setLocation(translateToScreen(window.getLocation(), target));
  }

  /** If the location is not on the preferred screen, then return a copy
   * translated to the preferred screen.
   *
   * @param location a point on the screen
   * @return a new Point; unchanged copy if no screens
   */
  public static Point translateToPrefScreen(Point location) {
    if(!hasScreens())
      return new Point(location);
    return translateToScreen(location, getPrefScreen());
  }

  /** If the location is not on the specified screen, then return a copy
   * translated to the specified screen.
   *
   * @param _location a point on the screen
   * @param gc
   * @return a new Point; unchanged copy if no screens
   */
  public static Point translateToScreen(Point _location, GraphicsConfiguration gc) {
    Point location = new Point(_location);
    Rectangle targetScreenBounds = gc.getBounds();
    if(!targetScreenBounds.contains(location) && hasScreens()) {
      Rectangle locScreenBounds = getGraphicsConfiguration(location).getBounds();
      location.translate(targetScreenBounds.x - locScreenBounds.x,
                         targetScreenBounds.y - locScreenBounds.y);
    }
    return location;
  }

  /** If location is not on the same screen as the target then translate it (the
   * same object) to the target's screen.
   *
   * @param location move this point to the target screen
   * @param target   used to find destination screen
   * @return return for convenience, same object as argument
   */
  public static Point translateToScreen(Point location, Point target) {
    if(target == null || !hasScreens())
      return location;
    Rectangle screenBounds = getGraphicsConfiguration(target).getBounds();
    if(!screenBounds.contains(location)) {
      Rectangle locScreenBounds = getGraphicsConfiguration(location).getBounds();
      location.translate(screenBounds.x - locScreenBounds.x,
                         screenBounds.y - locScreenBounds.y);
    }
    return location;
  }

  /**
   * @return screen bounds of the preferred device
   * @throws HeadlessException if no screens
   */
  public static Rectangle getPrefScreenBounds() {
    return getPrefScreen().getBounds();
  }

  /**
   * @param index
   * @return screen bounds of the specified device, or the default
   *         screen's if index is out of range; see {@link #getScreen(int)}
   * @throws HeadlessException if no screens
   */
  public static Rectangle getScreenBounds(int index) {
    return getScreen(index).getBounds();
  }

  /**
   * Center the window on the preferred screen.
   *
   * @param window
   * @throws HeadlessException if no screens
   */
  public static void centerOnPrefScreen(Window window) {
    centerOnScreen(window, getPrefScreen());
  }

  /**
   * Center the window on the specified screen.
   *
   * @param window
   * @param gc target screen
   */
  public static void centerOnScreen(Window window, GraphicsConfiguration gc) {
    window.setLocation(centeredLocation(gc.getBounds(), window.getSize()));
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

  /**
   * If there's an environment variable "JAVA_PREFERRED_SCREEN", then use that
   * as an index into {@link #getScreenSort()} and return
   * that device; otherwise return the default screen device.
   * If the preferred device is no longer one of the screens, this is
   * re-evaluated.
   *
   * @return the preferred device
   * @throws HeadlessException if no screens
   */
  public static GraphicsDevice getPrefGraphicsDev() {
    ScreenChangeDetector detector = ScreenOrder.detector();
    GraphicsDevice gdev = detector.getPreferredDevice();
    if(gdev == null || !detector.snapshot().contains(gdev)) {
      gdev = setPrefGraphicsDev(detector, getIndex("JAVA_PREFERRED_SCREEN"));
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
   *
   * @param vari check this property/environment variable's value
   * @return the new preferred screen or null if bad argument
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
    GraphicsDevice gdev = setPrefGraphicsDev(ScreenOrder.detector(), screenIdx);
    return gdev == null ? null : gdev.getDefaultConfiguration();
  }

  /**
   * @param vari check this property/environment variable's value
   * @return updated preferred graphics dev or null if bad argument
   * @deprecated use {@link #setPrefScreen(String)}
   */
  @Deprecated
  public static GraphicsDevice setPrefGraphicsDev(String vari) {
    return setPrefGraphicsDev(getIndex(vari));
  }

  /**
   * @param screenIdx index into {@link #getScreenSort()}
   * @return updated preferred graphics dev or null if bad argument
   * @deprecated use {@link #setPrefScreen(int)}
   */
  @Deprecated
  public static GraphicsDevice setPrefGraphicsDev(int screenIdx) {
    return setPrefGraphicsDev(ScreenOrder.detector(), screenIdx);
  }

  private static GraphicsDevice setPrefGraphicsDev(ScreenChangeDetector detector,
                                                   int screenIdx) {
    GraphicsDevice gdev = detector.getGraphicsDevice(screenIdx);
    if(gdev != null)
      detector.setPreferredDevice(gdev);
    return gdev;
  }

  /**
   * Check each screen finding the one that contains the
   * given point. Return the default screen if not found.
   * <p>
   * Note that a point that is not on any screen is treated as if it were
   * on the default screen; for example, the translate methods translate it
   * relative to the default screen's origin, the result may be off screen.
   *
   * @param location
   * @return the screen containing location, else the default screen
   * @throws HeadlessException if no screens
   */
  public static GraphicsConfiguration getGraphicsConfiguration(Point location) {
    for(GraphicsConfiguration config : ScreenOrder.getScreenSort()) {
      if(config.getBounds().contains(location))
        return config;
    }
    return defaultScreenDevice().getDefaultConfiguration();
  }
}
// vi: set sw=2 ts=8:
