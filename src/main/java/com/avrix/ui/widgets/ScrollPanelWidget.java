package com.avrix.ui.widgets;

import com.avrix.ui.UIColor;

/**
 * The {@code ScrollPanelWidget} class represents a panel widget that supports scrolling.
 * It extends the {@code PanelWidget} class and adds functionality for managing scrollbars.
 */
public class ScrollPanelWidget extends PanelWidget {
    /**
     * The vertical scrollbar widget used for scrolling content in the vertical direction.
     * This scrollbar allows users to navigate through content that exceeds the visible area vertically.
     */
    protected ScrollbarWidget verticalScrollbar;

    /**
     * The horizontal scrollbar widget used for scrolling content in the horizontal direction.
     * This scrollbar allows users to navigate through content that exceeds the visible area horizontally.
     */
    protected ScrollbarWidget horizontalScrollbar;

    /**
     * The original maximum scroll value in the vertical direction before any scrolling adjustments.
     * This value is used to restore or reference the initial scrollable area height.
     */
    protected int originalMaxScrollY = 0;

    /**
     * The original maximum scroll value in the horizontal direction before any scrolling adjustments.
     * This value is used to restore or reference the initial scrollable area width.
     */
    protected int originalMaxScrollX = 0;

    /**
     * Constructs a new {@link ScrollPanelWidget} with the specified position and size.
     *
     * @param x      the x-coordinate of the {@link Widget}'s position
     * @param y      the y-coordinate of the {@link Widget}'s position
     * @param width  the width of the {@link Widget}
     * @param height the height of the {@link Widget}
     */
    public ScrollPanelWidget(int x, int y, int width, int height) {
        this(x, y, width, height, 0, UIColor.LIGHT_BLACK);
    }

    /**
     * Constructs a new {@link ScrollPanelWidget} with the specified position, size, border radius,
     * and background color.
     *
     * @param x               the X-coordinate of the {@link Widget}'s position
     * @param y               the Y-coordinate of the {@link Widget}'s position
     * @param width           the width of the widget
     * @param height          the height of the widget
     * @param borderRadius    the radius of the corner rounding in pixels
     * @param backgroundColor the background color of the widget, specified in {@link UIColor}
     */
    public ScrollPanelWidget(int x, int y, int width, int height, int borderRadius, UIColor backgroundColor) {
        super(x, y, width, height, borderRadius, backgroundColor);

        this.scrollable = true;
    }

    /**
     * Returns the vertical scrollbar widget associated with this container.
     *
     * @return the vertical scrollbar widget
     */
    public final ScrollbarWidget getVerticalScrollbar() {
        return this.verticalScrollbar;
    }

    /**
     * Returns the horizontal scrollbar widget associated with this container.
     *
     * @return the horizontal scrollbar widget
     */
    public final ScrollbarWidget getHorizontalScrollbar() {
        return this.horizontalScrollbar;
    }

    /**
     * Initializes the {@link Widget}
     */
    @Override
    public void onInitialize() {
        super.onInitialize();

        this.horizontalScrollbar = new ScrollbarWidget(true);
        this.verticalScrollbar = new ScrollbarWidget();

        addChild(this.horizontalScrollbar);
        addChild(this.verticalScrollbar);

        this.horizontalScrollbar.initPosition();
        this.verticalScrollbar.initPosition();
    }

    /**
     * Removes a child widget from this widget's list of children.
     *
     * @param widget the widget to remove from the list of children
     */
    @Override
    public synchronized void addChild(Widget widget) {
        super.addChild(widget);
        bringToTopScrollbars(widget != this.horizontalScrollbar && widget != this.verticalScrollbar);
    }

    /**
     * Adds a child widget to this widget's list of children.
     *
     * @param widget the widget to add as a child
     */
    @Override
    public synchronized void removeChild(Widget widget) {
        super.removeChild(widget);
        bringToTopScrollbars(widget == this.horizontalScrollbar || widget == this.verticalScrollbar);
    }

    private void bringToTopScrollbars(boolean isAnotherWidget) {
        if (isAnotherWidget) {
            this.children.remove(this.horizontalScrollbar);
            this.children.remove(this.verticalScrollbar);
            this.children.add(this.horizontalScrollbar);
            this.children.add(this.verticalScrollbar);
        }
    }

    /**
     * Updates and renders all child widgets of this {@link Widget}.
     * This method recursively calls the update and render methods on each child widget,
     * ensuring that the rendering order respects the hierarchy of widgets.
     */
    @Override
    public void renderChildren() {
        for (Widget child : this.children) {
            if (!child.isVisible()) continue;

            if (child.getContext() == null) {
                child.setContext(this.context);
            }

            // Limitation for scroll bars
            if (!child.equals(this.horizontalScrollbar) && !child.equals(this.verticalScrollbar)) {
                saveRenderState();
                intersectScissor(getX(), getY(),
                        this.verticalScrollbar.isVisible() ? this.getWidth() - this.verticalScrollbar.width - this.verticalScrollbar.borderOffset * 2 : this.getWidth(),
                        this.horizontalScrollbar.isVisible() ? this.getHeight() - this.horizontalScrollbar.height - this.horizontalScrollbar.borderOffset * 2 : this.getHeight());
            }

            // Calculate absolute positions considering scrolling
            int absoluteX = child.isScrollLock() ? getX() + child.getX() : getX() + child.getX() - this.scrollX;
            int absoluteY = child.isScrollLock() ? getY() + child.getY() : getY() + child.getY() - this.scrollY;

            // Save original positions to restore later
            int originalX = child.getX();
            int originalY = child.getY();

            // Set the child's position to the absolute position
            child.setX(absoluteX);
            child.setY(absoluteY);

            child.saveRenderState();
            child.intersectScissor(absoluteX, absoluteY, child.getWidth(), child.getHeight());

            // Render child and its children
            child.preRender();
            child.update();
            child.render();
            child.renderChildren();
            child.postRender();

            // Restore the original positions
            child.restoreRenderState();
            child.setX(originalX);
            child.setY(originalY);

            // Limitation for scroll bars
            if (!child.equals(this.horizontalScrollbar) && !child.equals(this.verticalScrollbar)) {
                restoreRenderState();
            }
        }
    }

    /**
     * Updates the maximum scroll offsets based on the coordinates and sizes of child widgets.
     * This method ensures the scroll limits are correctly set even if widgets overlap or are larger than the parent widget.
     */
    @Override
    protected void updateMaxScrollOffset() {
        super.updateMaxScrollOffset();

        this.originalMaxScrollY = this.maxScrollY;
        this.originalMaxScrollX = this.maxScrollX;
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();

        this.horizontalScrollbar.setVisible(maxScrollX > 0);
        this.verticalScrollbar.setVisible(maxScrollY > 0);

        if (this.verticalScrollbar.isVisible()) {
            this.maxScrollY = originalMaxScrollY + this.horizontalScrollbar.height + this.horizontalScrollbar.borderOffset * 2;
        }

        if (this.horizontalScrollbar.isVisible()) {
            this.maxScrollX = originalMaxScrollX + this.verticalScrollbar.width + this.verticalScrollbar.borderOffset * 2;
        }

        if (this.verticalScrollbar.isVisible() && this.horizontalScrollbar.isVisible()) {
            this.horizontalScrollbar.width = this.horizontalScrollbar.compressedWidth;
        }
    }
}