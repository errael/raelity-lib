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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A fake multi-screen environment for tests; works headless.
 * <p>
 * While open, {@link GraphicsEnvironment#getLocalGraphicsEnvironment()}
 * returns a {@link FakeEnv} whose screens are set with {@link #setScreens}.
 * The screens can be changed while open, to simulate a monitor being
 * added, removed or rearranged.
 * <p>
 * Opening also resets the static state cached by {@link Screens} and
 * {@link ScreenOrder}, so each test starts clean.
 * <p>
 * The static mock is thread local; use it only from the test's thread.
 * <pre>
 * try(FakeScreens fs = FakeScreens.open(0, new Rectangle(0, 0, 1920, 1080), ...)) {
 *   ...
 * }
 * </pre>
 */
final class FakeScreens implements AutoCloseable {

  private final FakeEnv env = new FakeEnv();
  private final MockedStatic<GraphicsEnvironment> mockedGE;

  private FakeScreens() {
    // CALLS_REAL_METHODS: only getLocalGraphicsEnvironment is faked;
    // in particular isHeadless() keeps its real behavior.
    mockedGE = Mockito.mockStatic(GraphicsEnvironment.class,
                                  Mockito.CALLS_REAL_METHODS);
    mockedGE.when(GraphicsEnvironment::getLocalGraphicsEnvironment)
        .thenReturn(env);
  }

  /**
   * Install the fake environment.
   * @param defaultIdx index into bounds of the default screen device
   * @param bounds each screen's bounds, in platform order
   * @return the fake, close it when done
   */
  static FakeScreens open(int defaultIdx, Rectangle... bounds) {
    FakeScreens fs = new FakeScreens();
    try {
      fs.setScreens(defaultIdx, bounds);
      resetStaticState();
    } catch(RuntimeException ex) {
      fs.close();
      throw ex;
    }
    return fs;
  }

  /**
   * Replace all the screens. New device objects are created.
   * @param defaultIdx index into bounds of the default screen device
   * @param bounds each screen's bounds, in platform order
   */
  void setScreens(int defaultIdx, Rectangle... bounds) {
    List<FakeDevice> devs = new ArrayList<>(bounds.length);
    for(int i = 0; i < bounds.length; i++)
      devs.add(new FakeDevice("screen" + i, bounds[i]));
    setDevices(defaultIdx, devs.toArray(FakeDevice[]::new));
  }

  /**
   * Set the platform device array; use existing devices to simulate
   * the platform reordering the same device objects.
   * @param defaultIdx index into devs of the default screen device,
   *                   -1 for no default (only sensible with no devices)
   * @param devs the devices in platform order
   */
  void setDevices(int defaultIdx, FakeDevice... devs) {
    env.devices = devs.clone();
    env.defaultDevice = defaultIdx < 0 ? null : env.devices[defaultIdx];
  }

  FakeEnv env() {
    return env;
  }

  /** @return the device in platform order */
  FakeDevice dev(int platformIdx) {
    return env.devices[platformIdx];
  }

  /** @return the default config of the device in platform order */
  FakeConfig gc(int platformIdx) {
    return env.devices[platformIdx].config;
  }

  @Override
  public void close() {
    try {
      resetStaticState();
    } finally {
      mockedGE.close();
    }
  }

  /**
   * Clear the preferred screen and force the change detector to re-poll.
   * Without this, state from one test leaks into the next.
   */
  static void resetStaticState() {
    try {
      // Force class init (which polls the environment) while mocked.
      Screens.isVirtualEnvironment();
      setField(Screens.class, null, "preferredGraphicsDevice", null);
      Object detector = getField(ScreenOrder.class, null, "changeDetector");
      // detector's poll() does a full refresh when it has no history
      synchronized(detector) {
        setField(ScreenChangeDetector.class, detector, "lastScreenBounds", null);
      }
      Screens.initVirtualParams();
    } catch(ReflectiveOperationException ex) {
      throw new IllegalStateException("FakeScreens: reset failed", ex);
    }
  }

  private static Object getField(Class<?> clazz, Object obj, String name)
      throws ReflectiveOperationException {
    Field f = clazz.getDeclaredField(name);
    f.setAccessible(true);
    return f.get(obj);
  }

  private static void setField(Class<?> clazz, Object obj, String name, Object val)
      throws ReflectiveOperationException {
    Field f = clazz.getDeclaredField(name);
    f.setAccessible(true);
    f.set(obj, val);
  }

  /**
   * A Mockito Window mock that remembers its location and has the given size.
   * Only getLocation/setLocation/getSize/getX/getY are meaningful.
   * Creating it does not run Window's constructor, so it works headless.
   */
  static Window mockWindow(Rectangle bounds) {
    Window w = mock(Window.class);
    Rectangle r = new Rectangle(bounds);
    when(w.getLocation()).thenAnswer(inv -> r.getLocation());
    when(w.getSize()).thenAnswer(inv -> r.getSize());
    when(w.getX()).thenAnswer(inv -> r.x);
    when(w.getY()).thenAnswer(inv -> r.y);
    when(w.getBounds()).thenAnswer(inv -> new Rectangle(r));
    doAnswer(inv -> {
      Point p = inv.getArgument(0);
      r.setLocation(p);
      return null;
    }).when(w).setLocation(any(Point.class));
    doAnswer(inv -> {
      r.setLocation((int)inv.getArgument(0), (int)inv.getArgument(1));
      return null;
    }).when(w).setLocation(anyInt(), anyInt());
    return w;
  }

  //////////////////////////////////////////////////////////////////////
  //
  // Fakes; just enough for Screens/ScreenOrder.
  //

  static final class FakeEnv extends GraphicsEnvironment {
    private FakeDevice[] devices = new FakeDevice[0];
    private FakeDevice defaultDevice;

    @Override
    public GraphicsDevice[] getScreenDevices() {
      return devices.clone(); // like the real ones, a fresh array
    }

    @Override
    public GraphicsDevice getDefaultScreenDevice() {
      return defaultDevice;
    }

    @Override
    public Graphics2D createGraphics(BufferedImage img) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Font[] getAllFonts() {
      return new Font[0];
    }

    @Override
    public String[] getAvailableFontFamilyNames() {
      return new String[0];
    }

    @Override
    public String[] getAvailableFontFamilyNames(java.util.Locale l) {
      return new String[0];
    }
  }

  static final class FakeDevice extends GraphicsDevice {
    private final String id;
    private final FakeConfig config;

    FakeDevice(String id, Rectangle bounds) {
      this.id = id;
      this.config = new FakeConfig(this, bounds);
    }

    /** Simulate a resolution/position change of this screen, same device. */
    void setBounds(Rectangle bounds) {
      config.bounds = new Rectangle(bounds);
    }

    @Override
    public int getType() {
      return TYPE_RASTER_SCREEN;
    }

    @Override
    public String getIDstring() {
      return id;
    }

    @Override
    public GraphicsConfiguration[] getConfigurations() {
      return new GraphicsConfiguration[] { config };
    }

    @Override
    public FakeConfig getDefaultConfiguration() {
      return config;
    }

    @Override
    public String toString() {
      return "FakeDevice[" + id + ", " + config.bounds + "]";
    }
  }

  static final class FakeConfig extends GraphicsConfiguration {
    private final FakeDevice device;
    private Rectangle bounds;

    FakeConfig(FakeDevice device, Rectangle bounds) {
      this.device = device;
      this.bounds = new Rectangle(bounds);
    }

    @Override
    public GraphicsDevice getDevice() {
      return device;
    }

    @Override
    public Rectangle getBounds() {
      return new Rectangle(bounds); // like the real ones, a copy
    }

    @Override
    public ColorModel getColorModel() {
      return null;
    }

    @Override
    public ColorModel getColorModel(int transparency) {
      return null;
    }

    @Override
    public AffineTransform getDefaultTransform() {
      return new AffineTransform();
    }

    @Override
    public AffineTransform getNormalizingTransform() {
      return new AffineTransform();
    }

    @Override
    public String toString() {
      return "FakeConfig[" + device.id + ", " + bounds + "]";
    }
  }
}
// vi: set sw=2 ts=8:
