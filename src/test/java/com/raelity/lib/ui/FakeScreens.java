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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A fake multi-screen environment for tests; works headless.
 * <p>
 * While open, {@link ScreenOrder} gets its devices from here, see
 * {@link ScreenOrder#setDeviceSources}; the screens are set with
 * {@link #setScreens} and can be changed while open, to simulate a monitor
 * being added, removed or rearranged. Each open/close starts with a fresh
 * change detector, so no cached state leaks between tests.
 * <pre>
 * try(FakeScreens fs = FakeScreens.open(0, new Rectangle(0, 0, 1920, 1080), ...)) {
 *   ...
 * }
 * </pre>
 */
final class FakeScreens implements AutoCloseable {

  private volatile FakeDevice[] devices = new FakeDevice[0];
  private volatile FakeDevice defaultDevice;

  private FakeScreens() {
  }

  /**
   * Install the fake environment.
   * @param defaultIdx index into bounds of the default screen device
   * @param bounds each screen's bounds, in platform order
   * @return the fake, close it when done
   */
  static FakeScreens open(int defaultIdx, Rectangle... bounds) {
    FakeScreens fs = new FakeScreens();
    fs.setScreens(defaultIdx, bounds);
    // like the real ones, a fresh array each time
    ScreenOrder.setDeviceSources(() -> fs.devices.clone(), () -> fs.defaultDevice);
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
    devices = devs.clone();
    defaultDevice = defaultIdx < 0 ? null : devices[defaultIdx];
  }

  /** @return the devices in platform order, a new array */
  FakeDevice[] devices() {
    return devices.clone();
  }

  /** @return the default device, may be null */
  FakeDevice defaultDevice() {
    return defaultDevice;
  }

  /** @return the device in platform order */
  FakeDevice dev(int platformIdx) {
    return devices[platformIdx];
  }

  /** @return the default config of the device in platform order */
  FakeConfig gc(int platformIdx) {
    return devices[platformIdx].config;
  }

  @Override
  public void close() {
    ScreenOrder.resetDeviceSources();
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
  // Fakes; just enough for Screens/ScreenOrder. They don't touch the
  // toolkit, so they work headless.
  //

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
