package com.avrix.ui.widgets;

/**
 * A {@link Widget} that serves as a base class for layout containers like {@link VerticalBoxWidget} and {@link HorizontalBoxWidget}.
 * It provides common functionality for arranging child widgets with a specified margin between them.
 */
public class BoxLayoutWidget extends Widget {
    /**
     * The margin between the child {@link Widget}s.
     */
    protected int margin = 5;

    /**
     * Whether the {@link Widget} should automatically resize to fit its children.
     */
    protected boolean autoresize;

    /**
     * Indicates if the layout is currently being updated to avoid recursive updates.
     */
    protected boolean updatingLayout = false;

    /**
     * Last saved {@link Widget} height
     */
    protected int lastHeight;

    /**
     * Last saved {@link Widget} width
     */
    protected int lastWidth;

    /**
     * Constructs a new {@link Widget} with the specified position and size.
     *
     * @param x          the x-coordinate of the {@link Widget}
     * @param y          the y-coordinate of the {@link Widget}
     * @param width      the width of the {@link Widget}
     * @param height     the height of the {@link Widget}
     * @param autoresize whether the {@link Widget} should automatically resize to fit its children
     */
    public BoxLayoutWidget(int x, int y, int width, int height, boolean autoresize) {
        super(x, y, width, height);
        this.autoresize = autoresize;

        this.lastHeight = height;
        this.lastWidth = width;
    }

    /**
     * Adds a child widget to this widget's list of children.
     *
     * @param widget the widget to add as a child
     */
    @Override
    public synchronized void addChild(Widget widget) {
        widget.parent = this;
        children.add(widget);

        updateMaxScrollOffset();

        updateLayout();
    }

    /**
     * Removes a child widget from this widget's list of children.
     *
     * @param widget the widget to remove from the list of children
     */
    @Override
    public synchronized void removeChild(Widget widget) {
        widget.parent = null;
        children.remove(widget);

        updateMaxScrollOffset();

        updateLayout();
    }

    /**
     * Updates the layout of the {@link Widget}, positioning its children
     * with the specified margin between them. Adjusts the size of the widget if autoresize is enabled.
     */
    public void updateLayout() {
    }

    /**
     * Checks if the layout is currently being updated.
     *
     * @return true if the layout is being updated, false otherwise
     */
    public boolean isUpdatingLayout() {
        return updatingLayout;
    }

    /**
     * Sets the updating layout status.
     *
     * @param updatingLayout true if the layout is being updated, false otherwise
     */
    public void setUpdatingLayout(boolean updatingLayout) {
        this.updatingLayout = updatingLayout;
    }

    /**
     * Returns the margin between child {@link Widget}s.
     *
     * @return the margin between child {@link Widget}s
     */
    public final int getMargin() {
        return margin;
    }

    /**
     * Sets the margin between child {@link Widget}s.
     *
     * @param margin the margin between child {@link Widget}s
     */
    public final void setMargin(int margin) {
        this.margin = margin;
    }

    /**
     * Returns whether the {@link Widget} automatically resizes to fit its children.
     *
     * @return true if the {@link Widget} automatically resizes to fit its children, false otherwise
     */
    public final boolean isAutoresize() {
        return autoresize;
    }

    /**
     * Sets whether the {@link Widget} should automatically resize to fit its children.
     *
     * @param autoresize true if the {@link Widget} should automatically resize to fit its children, false otherwise
     */
    public final void setAutoresize(boolean autoresize) {
        this.autoresize = autoresize;
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
    }
}