package com.avrix.ui.widgets;

/**
 * The {@link PopupWidget} class is a custom widget that extends the ScrollPanelWidget.
 * This widget is designed to display a popup with scrollable content.
 * It provides additional functionalities specific to popup behavior.
 */
public class PopupWidget extends ScrollPanelWidget {
    /**
     * Constructs a new {@link PopupWidget} with the specified position and size.
     *
     * @param x      the x-coordinate of the {@link PopupWidget}'s position
     * @param y      the y-coordinate of the {@link PopupWidget}'s position
     * @param width  the width of the {@link PopupWidget}
     * @param height the height of the {@link PopupWidget}
     */
    public PopupWidget(int x, int y, int width, int height) {
        super(x, y, width, height);

        setAlwaysOnTop(true);
    }

    /**
     * Adds a child widget to this {@link PopupWidget}'s list of children.
     *
     * @param widget the widget to add as a child
     */
    @Override
    public synchronized void addChild(Widget widget) {
        super.addChild(widget);
        close();
    }

    /**
     * Handles the left mouse button down event outside any visible {@link PopupWidget}
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    @Override
    public void onLeftMouseDownOutside(int x, int y) {
        super.onLeftMouseDownOutside(x, y);
        close();
    }

    /**
     * Handles the right mouse button down event outside any visible {@link PopupWidget}
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    @Override
    public void onRightMouseDownOutside(int x, int y) {
        super.onRightMouseDownOutside(x, y);
    }

    /**
     * Open current {@link PopupWidget}
     */
    public void show() {
        addToScreen();
    }

    /**
     * Closes the current {@link PopupWidget}
     */
    public void close() {
        removeFromScreen();
    }

    /**
     * Renders the {@link PopupWidget}
     */
    @Override
    public void render() {
        super.render();
    }
}