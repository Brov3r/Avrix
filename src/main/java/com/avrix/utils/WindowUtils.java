package com.avrix.utils;

import zombie.core.Core;

/**
 * A set of tools for managing the game window
 */
public class WindowUtils {
    /**
     * Returns the height of the window.
     *
     * @return The height of the window.
     */
    public static int getWindowHeight() {
        return Core.getInstance().getScreenHeight();
    }

    /**
     * Returns the width of the window.
     *
     * @return The width of the window.
     */
    public static int getWindowWidth() {
        return Core.getInstance().getScreenWidth();
    }
}