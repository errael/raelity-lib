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
    private List<Rectangle> lastBounds;
    private GraphicsDevice[] lastDevices;
    private List<ScreenOrder.ScreenItem> lastSort;

    /** Returns true, and remembers the new state, if the bounds changed. */
    synchronized int poll() {
        GraphicsDevice[] devs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if(lastBounds != null && devs.length == lastBounds.size()) {
            int i = 0;
            while(i < devs.length &&
                devs[i].getDefaultConfiguration().getBounds().equals(lastBounds.get(i)))
                i++;
            if(i == devs.length)
                return generation;
        }
        // A screen change has been detected, bump generation, update cached items.
        generation++;
        lastDevices = devs;
        lastBounds = ScreenOrder.boundsOf(devs); // make unmodifiable if exposed
        lastSort = Collections.unmodifiableList(ScreenOrder.screenSort(lastBounds));
        Screens.initVirtualParams();
        return generation;
    }

    synchronized List<GraphicsDevice> getScreenSort() {
        poll();
        List<GraphicsDevice> result = new ArrayList<>(lastSort.size());
        lastSort.forEach(screenItem -> result.add(lastDevices[screenItem.getIndex()]));
        return result;
    }

    synchronized GraphicsDevice getGraphicsDevice(int screenIdx) {
        poll();
        GraphicsDevice gdev = null;
        if(screenIdx >= 0 && screenIdx < lastSort.size())
            gdev = lastDevices[lastSort.get(screenIdx).getIndex()];
        return gdev;
    }
}
// vi: set sw=2 ts=8:
