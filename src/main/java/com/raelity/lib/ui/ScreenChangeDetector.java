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
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Simple polling change detector. Compares bounds by value, which catches
 * rearrangement and resolution/scale changes as well as added or removed
 * screens (GraphicsDevice identity alone does not; the JDK may reuse
 * device objects across a configuration change).
 * <p>
 * The devices come from suppliers; {@link #createDefault()} uses the local
 * GraphicsEnvironment, reached lazily, and treats headless as no screens.
 * Nothing here throws HeadlessException.
 */
final class ScreenChangeDetector {
  private final Supplier<GraphicsDevice[]> deviceSource;
  private final Supplier<GraphicsDevice> defaultDeviceSource;

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
    private final List<Rectangle> bounds;   // platform order, private
    private final List<ScreenOrder.ScreenItem> sort;
    private final List<GraphicsConfiguration> screenSort;
    private final List<List<GraphicsConfiguration>> screenLayout;
    private final boolean isVirtual;

    private Snapshot(int generation, GraphicsDevice[] devs) {
      this.generation = generation;
      this.devices = devs.clone();
      this.bounds = ScreenOrder.screenBoundsOf(devices);
      this.sort = Collections.unmodifiableList(ScreenOrder.screenSort(bounds));

      List<GraphicsConfiguration> gcSort = new ArrayList<>(sort.size());
      for(ScreenOrder.ScreenItem item : sort)
        gcSort.add(devices[item.getIndex()].getDefaultConfiguration());
      this.screenSort = Collections.unmodifiableList(gcSort);

      // rows of indexes into devices
      List<List<GraphicsConfiguration>> layout = new ArrayList<>();
      for(List<Integer> listIdx : ScreenOrder.screenLayoutIndex(sort)) {
        layout.add(listIdx.stream()
            .map(idx -> devices[idx].getDefaultConfiguration()).toList());
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

    private boolean sameBounds(GraphicsDevice[] devs) {
      if(devs.length != bounds.size())
        return false;
      for(int i = 0; i < devs.length; i++) {
        if(!devs[i].getDefaultConfiguration().getBounds().equals(bounds.get(i)))
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
    this.deviceSource = Objects.requireNonNull(deviceSource);
    this.defaultDeviceSource = Objects.requireNonNull(defaultDeviceSource);
  }

  /** Use the local graphics environment; headless has no screens. */
  static ScreenChangeDetector createDefault() {
    return new ScreenChangeDetector(ScreenChangeDetector::localScreenDevices,
                                    ScreenChangeDetector::localDefaultScreenDevice);
  }

  private static GraphicsDevice[] localScreenDevices() {
    if(GraphicsEnvironment.isHeadless())
      return new GraphicsDevice[0];
    try {
      return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    } catch(HeadlessException ex) {
      return new GraphicsDevice[0];
    }
  }

  private static GraphicsDevice localDefaultScreenDevice() {
    if(GraphicsEnvironment.isHeadless())
      return null;
    try {
      return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    } catch(HeadlessException ex) {
      return null;
    }
  }

  /** Check for a change, update the snapshot if needed and return it. */
  synchronized Snapshot snapshot() {
    GraphicsDevice[] devs = deviceSource.get();
    if(devs == null)
      devs = new GraphicsDevice[0];
    if(snapshot == null || !snapshot.sameBounds(devs))
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
