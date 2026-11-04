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
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple polling change detector. Compares bounds by value, which catches
 * rearrangement and resolution/scale changes as well as added or removed
 * screens (GraphicsDevice identity alone does not).
 */
final class ScreenChangeDetector {
    /** Returned by poll, use it to detect a change. */
    private int generation;
    private List<Rectangle> lastScreenBounds;
    private GraphicsDevice[] lastDevices;
    private List<ScreenOrder.ScreenItem> lastScreenItemSort;
    private List<List<GraphicsConfiguration>> lastScreenLayout;

    /** Returns true, and remembers the new state, if the bounds changed. */
    synchronized int poll() {
        GraphicsDevice[] devs
            = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if(lastScreenBounds != null && devs.length == lastScreenBounds.size()) {
            int i = 0;
            while(i < devs.length &&
                devs[i].getDefaultConfiguration().getBounds().equals(lastScreenBounds.get(i)))
                i++;
            if(i == devs.length)
                return generation;
        }
        // A screen change has been detected, bump generation, update cached items.
        generation++;
        lastDevices = devs;
        lastScreenBounds = ScreenOrder.screenBoundsOf(devs); // make unmodifiable if exposed
        lastScreenItemSort = Collections.unmodifiableList(ScreenOrder.screenSort(lastScreenBounds));
        Screens.initVirtualParams();

        // Get list of rows of indexes into devs.
        List<List<Integer>> screenLayoutIndexes = ScreenOrder.screenLayoutIndex(lastScreenItemSort);
        lastScreenLayout = new ArrayList<>(screenLayoutIndexes.size());
        for(List<Integer> listIdx : screenLayoutIndexes) {
          List<GraphicsConfiguration> gcRow
              = listIdx.stream().map(idx -> devs[idx].getDefaultConfiguration()).toList();
          lastScreenLayout.add(gcRow);
        }
        lastScreenLayout = Collections.unmodifiableList(lastScreenLayout);
        return generation;
    }

    synchronized List<GraphicsConfiguration> getScreenSort() {
        poll();
        List<GraphicsConfiguration> result = new ArrayList<>(lastScreenItemSort.size());
        lastScreenItemSort.forEach(screenItem
            -> result.add(lastDevices[screenItem.getIndex()].getDefaultConfiguration()));
        return result;
    }

    synchronized GraphicsDevice getGraphicsDevice(int screenIdx) {
        poll();
        GraphicsDevice gdev = null;
        if(screenIdx >= 0 && screenIdx < lastScreenItemSort.size())
            gdev = lastDevices[lastScreenItemSort.get(screenIdx).getIndex()];
        return gdev;
    }

    synchronized List<List<GraphicsConfiguration>> getScreenLayout() {
      poll();
      return lastScreenLayout;
    }
}
// vi: set sw=2 ts=8:
