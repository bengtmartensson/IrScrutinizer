/*
Copyright (C) 2018 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.guicomponents;

import java.awt.Cursor;
import java.awt.Window;

/**
 * Class for setting and resetting busy cursor of a java.awt.Window.
 *
 */
public class BusyWindow {
    private final static Cursor WAIT_CURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    
    private final Window window;
    private Cursor oldCursor;
    
    /**
     * Static convenience function, factory function, that combines the constructor and the busy() call.
     * @param window
     * @return newly constructed BusyWindow.
     */
    public static BusyWindow mkBusyWindow(Window window) {
        BusyWindow busyWindow = new BusyWindow(window);
        busyWindow.busy();
        return busyWindow;
    }
     
    /**
     * @param window 
     */
    public BusyWindow(Window window) {
        this.window = window;
        oldCursor = null;
    }
    
    /**
     * Sets the wait cursor
     */
    public void busy() {
        oldCursor = window.getCursor();
        window.setCursor(WAIT_CURSOR);
    }
    
    /**
     * Resets cursor
     */
    public void unBusy() {
        if (oldCursor == null)
            throw new NullPointerException("Calling BusyWindow.unBusy() without preceeding busy()");
        window.setCursor(oldCursor);
        oldCursor = null;
    }
    
    /**
     * Returns status
     * @return 
     */
    public boolean isBusy() {
        return oldCursor != null;
    }
}
