package com.avrix.ui.widgets;

import com.avrix.ui.NanoDrawer;
import com.avrix.ui.NanoColor;

/**
 * The {@link  ScrollPanelWidget} class represents a panel widget that supports scrolling.
 * It extends the {@link  PanelWidget} class and adds functionality for managing scrollbars.
 */
public class ScrollPanelWidget extends PanelWidget {
    /**
     * The vertical scrollbar {@link Widget} used for scrolling content in the vertical direction.
     * This scrollbar allows users to navigate through content that exceeds the visible area vertically.
     */
    protected ScrollbarWidget verticalScrollbar;

    /**
     * The horizontal scrollbar {@link Widget} used for scrolling content in the horizontal direction.
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
        this(x, y, width, height, 0, NanoColor.LIGHT_BLACK);
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
     * @param backgroundColor the background color of the widget, specified in {@link NanoColor}
     */
    public ScrollPanelWidget(int x, int y, int width, int height, int borderRadius, NanoColor backgroundColor) {
        super(x, y, width, height, borderRadius, backgroundColor);

        this.scrollable = true;
    }

    /**
     * Returns the vertical scrollbar {@link Widget} associated with this container.
     *
     * @return the vertical scrollbar {@link Widget}
     */
    public final ScrollbarWidget getVerticalScrollbar() {
        return verticalScrollbar;
    }

    /**
     * Returns the horizontal scrollbar {@link Widget} associated with this container.
     *
     * @return the horizontal scrollbar {@link Widget}
     */
    public final ScrollbarWidget getHorizontalScrollbar() {
        return horizontalScrollbar;
    }

    /**
     * Initializes the {@link Widget}
     */
    @Override
    public void onInitialize() {
        super.onInitialize();

        horizontalScrollbar = new ScrollbarWidget(true);
        verticalScrollbar = new ScrollbarWidget();

        addChild(horizontalScrollbar);
        addChild(verticalScrollbar);

        horizontalScrollbar.updatePosition();
        verticalScrollbar.updatePosition();
    }

    /**
     * Removes a child widget from this {@link Widget}'s list of children.
     *
     * @param widget the widget to remove from the list of children
     */
    @Override
    public synchronized void removeChild(Widget widget) {
        super.removeChild(widget);
        raiseScrollbars(widget != horizontalScrollbar && widget != verticalScrollbar);
    }

    /**
     * Adds a child widget to this {@link Widget}'s list of children.
     *
     * @param widget the widget to add as a child
     */
    @Override
    public synchronized void addChild(Widget widget) {
        super.addChild(widget);
        raiseScrollbars(widget != horizontalScrollbar && widget != verticalScrollbar);
    }

    /**
     * Raising scrollbars to the top of rendering
     *
     * @param isAnotherWidget flag pointing to other widgets
     */
    private synchronized void raiseScrollbars(boolean isAnotherWidget) {
        if (isAnotherWidget && horizontalScrollbar != null && verticalScrollbar != null) {
            children.remove(horizontalScrollbar);
            children.remove(verticalScrollbar);
            children.add(horizontalScrollbar);
            children.add(verticalScrollbar);
        }
    }

    /**
     * Updates and renders all child widgets of this {@link Widget}.
     * This method recursively calls the update and render methods on each child {@link Widget},
     * ensuring that the rendering order respects the hierarchy of {@link Widget}s.
     */
    @Override
    public void renderChildren() {
        for (Widget child : children) {
            if (!child.isVisible()) continue;

            // Limitation for scroll bars
            if (!child.equals(horizontalScrollbar) && !child.equals(verticalScrollbar)) {
                NanoDrawer.saveRenderState();
                NanoDrawer.intersectScissor(getX(), getY(),
                        verticalScrollbar.isVisible() ? getWidth() - verticalScrollbar.width - verticalScrollbar.borderOffset * 2 : getWidth(),
                        horizontalScrollbar.isVisible() ? getHeight() - horizontalScrollbar.height - horizontalScrollbar.borderOffset * 2 : getHeight());
            }

            // Calculate absolute positions considering scrolling
            int absoluteX = child.isScrollLock() ? getX() + child.getX() : getX() + child.getX() - scrollX;
            int absoluteY = child.isScrollLock() ? getY() + child.getY() : getY() + child.getY() - scrollY;

            // Save original positions to restore later
            int originalX = child.getX();
            int originalY = child.getY();

            // Set the child's absolute position
            child.setXA(absoluteX);
            child.setYA(absoluteY);

            // Set the child's position to the absolute position
            child.setX(absoluteX);
            child.setY(absoluteY);

            NanoDrawer.saveRenderState();
            NanoDrawer.intersectScissor(absoluteX, absoluteY, child.getWidth(), child.getHeight());

            // Render child and its children
            child.preRender();
            child.update();
            child.render();
            child.renderChildren();
            child.postRender();

            // Restore the original positions
            NanoDrawer.restoreRenderState();
            child.setX(originalX);
            child.setY(originalY);

            // Limitation for scroll bars
            if (!child.equals(horizontalScrollbar) && !child.equals(verticalScrollbar)) {
                NanoDrawer.restoreRenderState();
            }
        }
    }

    /**
     * Updates the maximum scroll offsets based on the coordinates and sizes of child {@link Widget}s.
     * This method ensures the scroll limits are correctly set even if {@link Widget}s overlap or are larger than the parent {@link Widget}.
     */
    @Override
    protected void updateMaxScrollOffset() {
        super.updateMaxScrollOffset();

        originalMaxScrollY = maxScrollY;
        originalMaxScrollX = maxScrollX;
    }

    /**
     * Sets the maximum horizontal scroll offset of the widget.
     * This value determines the limit for horizontal scrolling based on the content width.
     *
     * @param maxScrollX the new maximum horizontal scroll offset
     */
    @Override
    public void setMaxScrollX(int maxScrollX) {
        super.setMaxScrollX(maxScrollX);
        originalMaxScrollX = maxScrollX;
    }

    /**
     * Sets the maximum vertical scroll offset of the widget.
     * This value determines the limit for vertical scrolling based on the content height.
     *
     * @param maxScrollY the new maximum vertical scroll offset
     */
    @Override
    public void setMaxScrollY(int maxScrollY) {
        super.setMaxScrollY(maxScrollY);
        originalMaxScrollY = maxScrollY;
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();

        horizontalScrollbar.setVisible(maxScrollX > 0);
        verticalScrollbar.setVisible(maxScrollY > 0);

        if (verticalScrollbar.isVisible()) {
            maxScrollY = originalMaxScrollY + horizontalScrollbar.height + horizontalScrollbar.borderOffset * 2;
        }

        if (horizontalScrollbar.isVisible()) {
            maxScrollX = originalMaxScrollX + verticalScrollbar.width + verticalScrollbar.borderOffset * 2;
        }

        if (verticalScrollbar.isVisible() && horizontalScrollbar.isVisible()) {
            horizontalScrollbar.width = horizontalScrollbar.compressedWidth;
        }

        horizontalScrollbar.updatePosition();
        verticalScrollbar.updatePosition();
    }
}