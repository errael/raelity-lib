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
 * with {@link #setPrefGraphicsDev(String)} or {@link #setPrefGraphicsDev(int)}.
 * If not set it defaults to "JAVA_PREFERRED_SCREEN";
 * if the index is not present or out of bound then
 * {@link GraphicsEnvironment#getDefaultScreenDevice()}.{@link GraphicsDevice#getDefaultConfiguration() getDefaultConfiguration()}
 * is used.
 */
// TODO: should examine/validate for multithreading
public class Screens {
  private Screens() {
  }
  private static boolean isVirtual;
  private static Rectangle virtualBounds;

  static {
    initVirtualParams();
  }

  /**
   *
   * @return true if virtual
   */
  public static boolean isVirtualEnvironment() {
    return isVirtual;
  }

  /**
   * The screens in sorted order; see {@link #getScreenLayout() }.
   * The elements are obtained by {@link GraphicsDevice#getDefaultConfiguration() }.
   * @return 
   */
  public static List<GraphicsConfiguration> getScreenSort() {
    return ScreenOrder.getScreenSort();
  }

  /**
   * The screens are laid out as a list of rows;
   * each row is a list of {@link GraphicsConfiguration}s (screens) left-to-right.
   * The elements are obtained by {@link GraphicsDevice#getDefaultConfiguration() }.
   * @return 
   */
  public static List<List<GraphicsConfiguration>> getScreenLayout() {
    return ScreenOrder.getScreenLayout();
  }

  /**
   * Return index into linear list of screens.
   * Both row and indexIntoRow are pegged at any edge if out of range.
   * @param _row
   * @param _indexInRow
   * @return 
   */
  public static int getIndex(int _row, int _indexInRow) {
    List<List<GraphicsConfiguration>> screenLayout = getScreenLayout();
    int row = _row < 0 ? 0 : _row >= screenLayout.size()
                             ? screenLayout.size() -1 : _row;
    int indexInRow = _indexInRow < 0 ? 0 : _indexInRow;
    List<GraphicsConfiguration> rowList = null;
    // index into single/linear list
    int index = 0;
    for(int rowIndex = 0; rowIndex < screenLayout.size(); rowIndex++) {
      rowList = screenLayout.get(rowIndex);
      if (row == rowIndex)
        break;
      index += rowList.size();
    }
    assert rowList != null;
    if (indexInRow >= rowList.size())
      indexInRow = rowList.size() - 1;
    return index + indexInRow;
  }

  /**
   * @return default graphics configuration of the preferred device
   * @deprecated use getPrefScreen
   */
  @Deprecated
  public static GraphicsConfiguration getPrefGraphicsConfiguration() {
    return getPrefScreen();
  }

  /**
   * @return default graphics configuration of the preferred device
   */
  public static GraphicsConfiguration getPrefScreen() {
    return getPrefGraphicsDev().getDefaultConfiguration();
  }

  /**
   * Get the indexed GraphicsConfiguration in sorted list; if index is out
   * of range, get the default screen's GraphicConfiguration.
   * @param index index into sorted list
   * @return the indexed GraphicsConfiguration in sorted list, or default
   */
  public static GraphicsConfiguration getScreen(int index) {
    List<GraphicsConfiguration> screenSort = ScreenOrder.getScreenSort();
    if (index < 0 || index >= screenSort.size())
      return GraphicsEnvironment.getLocalGraphicsEnvironment()
          .getDefaultScreenDevice().getDefaultConfiguration();
    return screenSort.get(index);
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
   */
  public static void translateToPrefScreen(Window window) {
    //window.setLocation(translateToScreen(window.getLocation(), getPrefGraphicsConfiguration()));
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
    // if(target == null)
    //   return;
    // window.setLocation(translateToScreen(window.getLocation(), target));
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

  /** If the location is not on the preferred screen, then translate it (the
   * same object) to the preferred screen.
   *
   * @param location a point on the scre
   * @return return for convenience, same object as argument
   */
  public static Point translateToPrefScreen(Point location) {
    return translateToScreen(location, getPrefScreen());
  }

  /** If the location is not on the specified screen, then translate it (the
   * same object) to the specified screen.
   *
   * @param _location a point on the scre
   * @param gc
   * @return return for convenience, same object as argument
   */
  public static Point translateToScreen(Point _location, GraphicsConfiguration gc) {
    Point location = (Point)_location.clone();
    Rectangle targetScreenBounds = gc.getBounds();
    if(!targetScreenBounds.contains(location)) {
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
    if(target == null)
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
   */
  public static Rectangle getPrefScreenBounds() {
    return getPrefScreen().getBounds();
  }

  /**
   * @param index
   * @return screen bounds of the specified device
   */
  public static Rectangle getScreenBounds(int index) {
    return getScreen(index).getBounds();
  }

  /**
   * Center the window on the preferred screen.
   *
   * @param window
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
  // TODO: Is this right? I guess it centers/setLocation in something
  //       like the GraphicsConfig's Rect((0,0), screenSize).
  //       Then translates it to the GraphicsConfig's screen.
  //       The logic here feels messy, but might be OK.
  public static void centerOnScreen(Window window, GraphicsConfiguration gc) {
    Dimension screenSize = gc.getBounds().getSize();
    Dimension size = window.getSize();
    window.setLocation((screenSize.width - size.width) / 2,
                       (screenSize.height - size.height) / 2);
    translateToScreen(window, gc);
  }


  
  /** the preferred graphics device */
  private static GraphicsDevice preferredGraphicsDevice = null;
  //* {@link GraphicsEnvironment#getLocalGraphicsEnvironment#getScreenDevices()}

  /**
   * If there's an environment variable "JAVA_PREFERRED_SCREEN", then use that
   * as an index into {@link GraphicsEnvironment#getScreenDevices()} and return
   * that device; otherwise return the default screen device.
   *
   * @return
   */
  public static GraphicsDevice getPrefGraphicsDev() {
    if(preferredGraphicsDevice == null) {
      GraphicsDevice gdev = setPrefGraphicsDev("JAVA_PREFERRED_SCREEN");
      if(gdev == null)
        preferredGraphicsDevice = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }
    return preferredGraphicsDevice;
  }

  /**
   * This sets the preferred graphics device, use by the methods in this class,
   * according the value of the specified String. First check System properties,
   * if not found then check environment variables.
   * If neither is found then the existing preferred device is not changed.
   *
   * @param vari check this property/environment variable's value
   * @return updated preferred graphics dev or null if bad argument
   */
  public static GraphicsDevice setPrefGraphicsDev(String vari) {
    return setPrefGraphicsDev(getIndex(vari));
  }

  /**
   * This sets the preferred graphics device, used by the methods in this class,
   * according the specified value. If null is returned, then the existing
   * preferred device is not changed.
   *
   * @param screenIdx look for this screen device index.
   * @return updated preferred graphics dev or null if bad argument
   */
  public static GraphicsDevice setPrefGraphicsDev(int screenIdx) {
    GraphicsDevice gdev = ScreenOrder.getGraphicsDevice(screenIdx);
    if(gdev != null)
      preferredGraphicsDevice = gdev;
    return gdev;
  }

  /**
   * Check each screen Device finding the defaultConfiguration that contains the
   * given point. Return defaultScreenDevice if not found.
   *
   * @param location
   * @return
   */
  public static GraphicsConfiguration getGraphicsConfiguration(Point location) {
    GraphicsDevice[] gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    for(GraphicsDevice gd : gs) {
      GraphicsConfiguration config = gd.getDefaultConfiguration();
      if(config.getBounds().contains(location))
        return config;
    }
    return GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration();
  }

  /** This is only invoked from ScreenOrder (or class initialization) */
  static void initVirtualParams() {
    Point zeroPoint = new Point();
    virtualBounds = new Rectangle();
    isVirtual = false;
    GraphicsEnvironment ge = GraphicsEnvironment.
        getLocalGraphicsEnvironment();
    GraphicsDevice[] gs = ge.getScreenDevices();
    for(GraphicsDevice gd : gs) {
      for(GraphicsConfiguration gc : gd.getConfigurations()) {
        if(!gc.getBounds().getLocation().equals(zeroPoint))
          isVirtual = true;
        virtualBounds = virtualBounds.union(gc.getBounds());
      }
    }
  }
}
// vi: set sw=2 ts=8:
