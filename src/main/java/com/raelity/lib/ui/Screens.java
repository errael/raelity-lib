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

/**
 * Support methods for multiscreen environments that operate on the preferred
 * screen.
 * <p>
 * The default screen can be specified by the environment variable:
 * "JAVA_PREFERRED_SCREEN"; in addition if the environment variable is not
 * present, then look for a java property by that name. If the environment
 * variable is not present or out of bound, the default is used.
 * </p>
 * See {@link #setPrefGraphicsDev(java.lang.String) }
 * or {@link #setPrefGraphicsDev(int) } to set the preferred graphics device
 * used by these methods.
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
   * @return default graphics configuration of the preferred device
   */
  public static GraphicsConfiguration getPrefGraphicsConfiguration() {
    return getPrefGraphicsDev().getDefaultConfiguration();
  }

  /** Using the location of the Window, if it is not on the preferred screen,
   * then translate it to the preferred screen.
   *
   * @param window
   */
  public static void translateToPrefScreen(Window window) {
    window.setLocation(translateToPrefScreen(window.getLocation()));
  }

  /** If Window is not on the same screen as target, then translate Window to
   * target's screen.
   *
   * @param window
   * @param target
   */
  public static void translateToPrefScreen(Window window, Point target) {
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
    Rectangle screenBounds = getPrefScreenBounds();
    if(!screenBounds.contains(location)) {
      Rectangle locScreenBounds = getGraphicsConfiguration(location).getBounds();
      location.translate(screenBounds.x - locScreenBounds.x,
                         screenBounds.y - locScreenBounds.y);
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
    return getPrefGraphicsConfiguration().getBounds();
  }

  /**
   * Center the window on the preferred screen.
   *
   * @param window
   */
  public static void centerOnPrefScreen(Window window) {
    //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    //Rectangle screenBounds = Jvi.getScreenBounds();
    Dimension screenSize = getPrefScreenBounds().getSize();
    Dimension size = window.getSize();
    window.setLocation((screenSize.width - size.width) / 2,
                       (screenSize.height - size.height) / 2);
    translateToPrefScreen(window);
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
    GraphicsDevice gdev = null;
    int screenIdx;
    String sint;
    sint = System.getProperty(vari);
    if(sint == null)
      sint = System.getenv(vari);
    if(sint != null) {
      try {
        screenIdx = Integer.parseInt(sint);
        gdev = setPrefGraphicsDev(screenIdx);
      } catch(NumberFormatException ex) {
      }
    }
    return gdev;
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
