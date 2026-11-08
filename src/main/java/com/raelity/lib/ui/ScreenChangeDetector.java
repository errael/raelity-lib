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
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simple polling change detector. A change is any difference, by platform
 * index, in the GraphicsDevice object, its default GraphicsConfiguration
 * object, or the bounds (by value). Bounds catch rearrangement and
 * resolution/scale changes where the JDK reuses the device and
 * configuration objects; identity catches new objects with the same
 * bounds, so the cached objects are always the current ones.
 * <p>
 * The devices come from suppliers; {@link #createDefault()} uses the local
 * GraphicsEnvironment, reached lazily, and treats headless as no screens.
 * Nothing here throws HeadlessException.
 */
final class ScreenChangeDetector {
  private final Supplier<GraphicsDevice[]> deviceSource;
  private final Supplier<GraphicsDevice> defaultDeviceSource;
  private final Function<GraphicsConfiguration, Insets> insetsSource;

  /** Returned by poll, use it to detect a change. */
  private int generation;
  private Snapshot snapshot;
  /** Owned by Screens; kept here so it goes away with the device sources. */
  private volatile GraphicsDevice preferredDevice;

  /**
   * Everything derived from one getScreenDevices() call; immutable.
   * Indexes in sort refer to devices.
   */
  static final class Snapshot {
    private final int generation;
    private final GraphicsDevice[] devices; // platform order, private copy
    private final GraphicsConfiguration[] configs; // default configs, platform order
    private final List<Rectangle> bounds;   // platform order, private
    private final List<ScreenOrder.ScreenItem> sort;
    private final List<GraphicsConfiguration> screenSort;
    private final List<List<GraphicsConfiguration>> screenLayout;
    private final boolean isVirtual;

    private Snapshot(int generation, GraphicsDevice[] devs) {
      this.generation = generation;
      this.devices = devs.clone();
      this.configs = new GraphicsConfiguration[devices.length];
      List<Rectangle> b = new ArrayList<>(devices.length);
      for(int i = 0; i < devices.length; i++) {
        configs[i] = devices[i].getDefaultConfiguration();
        b.add(configs[i].getBounds());
      }
      this.bounds = b;
      this.sort = Collections.unmodifiableList(ScreenOrder.screenSort(bounds));

      List<GraphicsConfiguration> gcSort = new ArrayList<>(sort.size());
      for(ScreenOrder.ScreenItem item : sort)
        gcSort.add(configs[item.getIndex()]);
      this.screenSort = Collections.unmodifiableList(gcSort);

      // rows of indexes into devices
      List<List<GraphicsConfiguration>> layout = new ArrayList<>();
      for(List<Integer> listIdx : ScreenOrder.screenLayoutIndex(sort)) {
        layout.add(listIdx.stream()
            .map(idx -> configs[idx]).toList());
      }
      this.screenLayout = Collections.unmodifiableList(layout);

      boolean virtual = false;
      Point zeroPoint = new Point();
      for(GraphicsDevice gd : devices) {
        for(GraphicsConfiguration gc : gd.getConfigurations()) {
          if(!gc.getBounds().getLocation().equals(zeroPoint))
            virtual = true;
        }
      }
      this.isVirtual = virtual;
    }

    int generation() {
      return generation;
    }

    /** @return number of screens */
    int size() {
      return devices.length;
    }

    /** @return the device at sorted index, null if out of range */
    GraphicsDevice getGraphicsDevice(int screenIdx) {
      return screenIdx >= 0 && screenIdx < sort.size()
             ? devices[sort.get(screenIdx).getIndex()] : null;
    }

    /** @return true if dev is one of this snapshot's devices */
    boolean contains(GraphicsDevice dev) {
      for(GraphicsDevice d : devices)
        if(d == dev)
          return true;
      return false;
    }

    /** Unmodifiable */
    List<GraphicsConfiguration> getScreenSort() {
      return screenSort;
    }

    /** Unmodifiable */
    List<List<GraphicsConfiguration>> getScreenLayout() {
      return screenLayout;
    }

    boolean isVirtual() {
      return isVirtual;
    }

    /** Same devices, default configurations and bounds, in the same order. */
    private boolean sameScreens(GraphicsDevice[] devs) {
      if(devs.length != devices.length)
        return false;
      for(int i = 0; i < devs.length; i++) {
        if(devs[i] != devices[i])
          return false;
        GraphicsConfiguration gc = devs[i].getDefaultConfiguration();
        if(gc != configs[i] || !gc.getBounds().equals(bounds.get(i)))
          return false;
      }
      return true;
    }
  }

  /**
   * @param deviceSource the screens in platform order; never null,
   *                     may be empty
   * @param defaultDeviceSource the default screen; null if no screens
   */
  ScreenChangeDetector(Supplier<GraphicsDevice[]> deviceSource,
                       Supplier<GraphicsDevice> defaultDeviceSource) {
    this(deviceSource, defaultDeviceSource, gc -> new Insets(0, 0, 0, 0));
  }

  /**
   * @param deviceSource the screens in platform order; never null,
   *                     may be empty
   * @param defaultDeviceSource the default screen; null if no screens
   * @param insetsSource a screen's insets (taskbars, panels); not cached,
   *                     they may change at any time
   */
  ScreenChangeDetector(Supplier<GraphicsDevice[]> deviceSource,
                       Supplier<GraphicsDevice> defaultDeviceSource,
                       Function<GraphicsConfiguration, Insets> insetsSource) {
    this.deviceSource = Objects.requireNonNull(deviceSource);
    this.defaultDeviceSource = Objects.requireNonNull(defaultDeviceSource);
    this.insetsSource = Objects.requireNonNull(insetsSource);
  }

  /** Use the local graphics environment; headless has no screens. */
  static ScreenChangeDetector createDefault() {
    return new ScreenChangeDetector(ScreenChangeDetector::localScreenDevices,
                                    ScreenChangeDetector::localDefaultScreenDevice,
                                    ScreenChangeDetector::localScreenInsets);
  }

  // These throw HeadlessException only if isHeadless() is true.

  private static GraphicsDevice[] localScreenDevices() {
    if(GraphicsEnvironment.isHeadless())
      return new GraphicsDevice[0];
    return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
  }

  private static GraphicsDevice localDefaultScreenDevice() {
    if(GraphicsEnvironment.isHeadless())
      return null;
    return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
  }

  private static Insets localScreenInsets(GraphicsConfiguration gc) {
    if(GraphicsEnvironment.isHeadless())
      return new Insets(0, 0, 0, 0);
    return Toolkit.getDefaultToolkit().getScreenInsets(gc);
  }

  /** @return the screen's insets, never null */
  Insets getScreenInsets(GraphicsConfiguration gc) {
    Insets insets = insetsSource.apply(gc);
    return insets == null ? new Insets(0, 0, 0, 0) : (Insets)insets.clone();
  }

  /** Check for a change, update the snapshot if needed and return it. */
  synchronized Snapshot snapshot() {
    GraphicsDevice[] devs = deviceSource.get();
    if(devs == null)
      devs = new GraphicsDevice[0];
    if(snapshot == null || !snapshot.sameScreens(devs))
      snapshot = new Snapshot(++generation, devs);
    return snapshot;
  }

  /** @return the generation, it changes when the screens change */
  int poll() {
    return snapshot().generation();
  }

  /** @return the default screen device, null if there are no screens */
  GraphicsDevice getDefaultScreenDevice() {
    return snapshot().size() == 0 ? null : defaultDeviceSource.get();
  }

  List<GraphicsConfiguration> getScreenSort() {
    return snapshot().getScreenSort();
  }

  GraphicsDevice getGraphicsDevice(int screenIdx) {
    return snapshot().getGraphicsDevice(screenIdx);
  }

  List<List<GraphicsConfiguration>> getScreenLayout() {
    return snapshot().getScreenLayout();
  }

  GraphicsDevice getPreferredDevice() {
    return preferredDevice;
  }

  void setPreferredDevice(GraphicsDevice dev) {
    preferredDevice = dev;
  }
}
// vi: set sw=2 ts=8:
