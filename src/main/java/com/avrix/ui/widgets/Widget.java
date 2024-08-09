package com.avrix.ui.widgets;

import com.avrix.ui.NanoColor;
import com.avrix.ui.NanoDrawer;
import com.avrix.ui.WidgetManager;
import com.avrix.utils.WindowUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base class for all UI {@link Widget}'s. This class provides the basic interface and functionality
 * for UI elements that can be rendered and interacted with.
 */
public abstract class Widget {
    /**
     * Indicates whether the {@link Widget} is currently hovered by the mouse cursor.
     */
    public boolean hovered = false;

    /**
     * Indicates whether the {@link Widget} is currently visible.
     */
    protected boolean visible = true;

    /**
     * Indicates whether the {@link Widget} should always be rendered on top of other {@link Widget}s.
     */
    protected boolean alwaysOnTop = false;

    /**
     * Indicates whether the {@link Widget} will respond to scrolling
     */
    protected boolean scrollLock = false;

    /**
     * Signals whether the {@link Widget} can scroll child elements
     */
    protected boolean scrollable = false;

    /**
     * Signal indicating whether {@link Widget} is currently left-clicked
     */
    protected boolean lmbPressed = false;

    /**
     * Signal indicating whether {@link Widget} is currently right-clicked
     */
    protected boolean rmbPressed = false;

    /**
     * The relative x-coordinate of the {@link Widget}'s position (if not a child, coincides with absolute coordinates).
     */
    protected int x;

    /**
     * The relative y-coordinate of the {@link Widget}'s position (if not a child, coincides with absolute coordinates).
     */
    protected int y;

    /**
     * The absolute x-coordinate of the {@link Widget}'s position.
     */
    protected int absoluteX;

    /**
     * The absolute y-coordinate of the {@link Widget}'s position.
     */
    protected int absoluteY;

    /**
     * The width of the {@link Widget}.
     */
    protected int width;

    /**
     * The height of the {@link Widget}.
     */
    protected int height;

    /**
     * The current horizontal scroll offset of the {@link Widget}.
     */
    protected int scrollX = 0;

    /**
     * The maximum horizontal scroll offset of the {@link Widget}.
     */
    protected int maxScrollX = 0;

    /**
     * The current vertical scroll offset of the {@link Widget}.
     */
    protected int scrollY = 0;

    /**
     * The maximum vertical scroll offset of the {@link Widget}.
     */
    protected int maxScrollY = 0;

    /**
     * The speed at which the {@link Widget} scrolls in response to mouse wheel movements.
     */
    protected int scrollSpeed = 20;

    /**
     * List of all child elements of the {@link Widget}.
     */
    protected List<Widget> children = new CopyOnWriteArrayList<>();

    /**
     * Parent {@link Widget}.
     */
    protected Widget parent = null;

    /**
     * Constructs a new {@link Widget} with the specified position and size.
     *
     * @param x      the x-coordinate of the {@link Widget}'s position
     * @param y      the y-coordinate of the {@link Widget}'s position
     * @param width  the width of the {@link Widget}
     * @param height the height of the {@link Widget}
     */
    public Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.absoluteX = x;
        this.absoluteY = y;
        this.width = width;
        this.height = height;

        onInitialize();

        updateMaxScrollOffset();
    }

    /**
     * Returns whether the {@link Widget} is scrollable.
     *
     * @return {@code true} if the {@link Widget} is scrollable, {@code false} otherwise.
     */
    public boolean isScrollable() {
        return scrollable;
    }

    /**
     * Sets the scrollable property of the {@link Widget}.
     *
     * @param scrollable the new scrollable value to set
     */
    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    /**
     * Checks if scrolling is currently locked for this {@link Widget}.
     *
     * @return {@code true} if scrolling is locked, {@code false} otherwise.
     */
    public boolean isScrollLock() {
        return scrollLock;
    }

    /**
     * Sets the scrolling lock state for this {@link Widget}.
     *
     * @param scrollLock {@code true} to lock scrolling, {@code false} to unlock it.
     */
    public void setScrollLock(boolean scrollLock) {
        this.scrollLock = scrollLock;
    }

    /**
     * Returns the parent {@link Widget} of this {@link Widget}.
     *
     * @return the parent {@link Widget}, or {@code null} if this {@link Widget} does not have a parent
     */
    public Widget getParent() {
        return parent;
    }

    /**
     * Returns the absolute (root) parent of this {@link Widget}.
     * If the {@link Widget} has no parent, it returns {@code null} or itself, depending on the desired behavior.
     *
     * @return the absolute parent of this {@link Widget}, or {@code null} if the {@link Widget} has no parent.
     */
    public Widget getAbsoluteParent() {
        Widget current = parent;

        while (current.getParent() != null) {
            current = current.getParent();
        }

        return current;
    }

    /**
     * Gets the current horizontal scroll offset of the widget.
     *
     * @return the current horizontal scroll offset
     */
    public int getScrollX() {
        return scrollX;
    }

    /**
     * Sets the current horizontal scroll offset of the widget.
     *
     * @param scrollX the new horizontal scroll offset
     */
    public void setScrollX(int scrollX) {
        this.scrollX = scrollX;
    }

    /**
     * Gets the maximum horizontal scroll offset of the widget.
     * This value represents the farthest point that the content can be scrolled horizontally.
     *
     * @return the maximum horizontal scroll offset
     */
    public int getMaxScrollX() {
        return maxScrollX;
    }

    /**
     * Sets the maximum horizontal scroll offset of the widget.
     * This value determines the limit for horizontal scrolling based on the content width.
     *
     * @param maxScrollX the new maximum horizontal scroll offset
     */
    public void setMaxScrollX(int maxScrollX) {
        this.maxScrollX = maxScrollX;
    }

    /**
     * Gets the current vertical scroll offset of the widget.
     *
     * @return the current vertical scroll offset
     */
    public int getScrollY() {
        return scrollY;
    }

    /**
     * Sets the current vertical scroll offset of the widget.
     *
     * @param scrollY the new vertical scroll offset
     */
    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }

    /**
     * Gets the maximum vertical scroll offset of the widget.
     * This value represents the farthest point that the content can be scrolled vertically.
     *
     * @return the maximum vertical scroll offset
     */
    public int getMaxScrollY() {
        return maxScrollY;
    }

    /**
     * Sets the maximum vertical scroll offset of the widget.
     * This value determines the limit for vertical scrolling based on the content height.
     *
     * @param maxScrollY the new maximum vertical scroll offset
     */
    public void setMaxScrollY(int maxScrollY) {
        this.maxScrollY = maxScrollY;
    }

    /**
     * Gets the speed at which the widget scrolls in response to mouse wheel movements.
     * This value determines how many pixels the scroll offset changes per wheel tick.
     *
     * @return the scroll speed
     */
    public int getScrollSpeed() {
        return scrollSpeed;
    }

    /**
     * Sets the speed at which the widget scrolls in response to mouse wheel movements.
     * This value determines how many pixels the scroll offset changes per wheel tick.
     *
     * @param scrollSpeed the new scroll speed
     */
    public void setScrollSpeed(int scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
    }

    /**
     * Updates and renders all child widgets of this {@link Widget}.
     * This method recursively calls the update and render methods on each child widget,
     * ensuring that the rendering order respects the hierarchy of widgets.
     */
    public void renderChildren() {
        for (Widget child : getChildren()) {
            if (!child.isVisible()) continue;

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

            if (child.isVisibleWithinParent() && child.isVisibleWithinWindow()) {
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
            }

            child.setX(originalX);
            child.setY(originalY);
        }
    }

    /**
     * Adds a child widget to this widget's list of children.
     *
     * @param widget the widget to add as a child
     */
    public synchronized void addChild(Widget widget) {
        widget.parent = this;
        children.add(widget);
        updateMaxScrollOffset();
    }

    /**
     * Removes a child widget from this widget's list of children.
     *
     * @param widget the widget to remove from the list of children
     */
    public synchronized void removeChild(Widget widget) {
        widget.parent = null;
        children.remove(widget);
        updateMaxScrollOffset();
    }

    /**
     * Gets an unmodifiable view of the list of child widgets.
     *
     * @return an unmodifiable list of child widgets
     */
    public synchronized List<Widget> getChildren() {
        return children;
    }

    /**
     * Initializes the {@link Widget}
     */
    public void onInitialize() {
    }

    /**
     * Updates the maximum scroll offsets based on the coordinates and sizes of child widgets.
     * This method ensures the scroll limits are correctly set even if widgets overlap or are larger than the parent widget.
     */
    protected void updateMaxScrollOffset() {
        int maxX = 0, maxY = 0;

        // Calculate the bounding box of all children
        for (Widget child : getChildren()) {
            if (child.isScrollLock()) continue;

            int childMaxX = child.getX() + child.getWidth();
            int childMaxY = child.getY() + child.getHeight();

            if (childMaxX > maxX) {
                maxX = childMaxX;
            }
            if (childMaxY > maxY) {
                maxY = childMaxY;
            }
        }

        // Calculate the maximum scroll offsets based on the parent's dimensions
        int newMaxScrollX = Math.max(0, maxX - width);
        int newMaxScrollY = Math.max(0, maxY - height);

        // Update the class fields with the calculated values
        maxScrollX = newMaxScrollX;
        maxScrollY = newMaxScrollY;

        if (maxScrollX == 0) scrollX = 0;
        if (maxScrollY == 0) scrollY = 0;
    }

    /**
     * Called when the mouse is moved over the {@link Widget}.
     *
     * @param x relative x-coordinate of mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onMouseMove(int x, int y) {
        boolean topWidgetHovered = false;

        hovered = true;

        List<Widget> childrenCopy = getChildren();
        for (int i = childrenCopy.size() - 1; i >= 0; i--) {
            Widget child = childrenCopy.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY) && !topWidgetHovered) {
                child.onMouseMove(childRelativeX, childRelativeY);
                child.hovered = true;
                topWidgetHovered = true;
            } else {
                child.hovered = false;
                child.onMouseMoveOutside(x, y);
            }
        }
    }

    /**
     * Called when the left mouse button is pressed down over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onLeftMouseDown(int x, int y) {
        bringToTop();
        List<Widget> childrenCopy = getChildren();
        for (int i = childrenCopy.size() - 1; i >= 0; i--) {
            Widget child = childrenCopy.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY) && child.hovered) {
                child.onLeftMouseDown(childRelativeX, childRelativeY);
            } else {
                child.onLeftMouseDownOutside(x, y);
            }
        }

        lmbPressed = true;
    }

    /**
     * Called when the left mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onLeftMouseUp(int x, int y) {
        List<Widget> childrenCopy = getChildren();
        for (int i = childrenCopy.size() - 1; i >= 0; i--) {
            Widget child = childrenCopy.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY) && child.hovered) {
                child.onLeftMouseUp(childRelativeX, childRelativeY);
            } else {
                child.onLeftMouseUpOutside(x, y);
            }
        }

        lmbPressed = false;
    }

    /**
     * Called when the right mouse button is pressed down over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onRightMouseDown(int x, int y) {
        bringToTop();
        List<Widget> childrenCopy = getChildren();
        for (int i = childrenCopy.size() - 1; i >= 0; i--) {
            Widget child = childrenCopy.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY) && child.hovered) {
                child.onRightMouseDown(childRelativeX, childRelativeY);
            } else {
                child.onRightMouseDownOutside(x, y);
            }
        }

        rmbPressed = true;
    }

    /**
     * Called when the right mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onRightMouseUp(int x, int y) {
        List<Widget> childrenCopy = getChildren();
        for (int i = childrenCopy.size() - 1; i >= 0; i--) {
            Widget child = childrenCopy.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY) && child.hovered) {
                child.onRightMouseUp(childRelativeX, childRelativeY);
            } else {
                child.onRightMouseUpOutside(x, y);
            }
        }

        rmbPressed = false;
    }

    /**
     * Called when the mouse wheel is scrolled over the {@link Widget}.
     *
     * @param x     relative x-coordinate of the mouse position
     * @param y     relative y-coordinate of the mouse position
     * @param delta direction of mouse wheel movement - (1 - up, -1 - down)
     */
    public void onMouseWheel(int x, int y, int delta) {
        // Update child widgets based on the new scroll values
        List<Widget> childrenCopy = getChildren();
        for (int i = childrenCopy.size() - 1; i >= 0; i--) {
            Widget child = childrenCopy.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY) && child.hovered) {
                child.onMouseWheel(childRelativeX, childRelativeY, delta);
                return;
            }
        }

        // Update scroll position
        if (scrollable) {
            scrollY -= delta * scrollSpeed;
            onMouseMove(x, y);
        }

        // Limit the scroll values
        if (scrollY < 0) {
            scrollY = 0;
        } else if (scrollY > maxScrollY) {
            scrollY = maxScrollY;
        }
    }

    /**
     * Called when the mouse cursor enters the bounds of this {@link Widget}.
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onMouseEnter(int x, int y) {
        List<Widget> childrenCopy = getChildren();
        for (int i = childrenCopy.size() - 1; i >= 0; i--) {
            Widget child = childrenCopy.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : scrollY);
            if (child.isPointOver(x, y)) {
                child.onMouseEnter(childRelativeX, childRelativeY);
            }
        }
    }

    /**
     * Called when the mouse cursor exits the bounds of this {@link Widget}.
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onMouseExit(int x, int y) {
        List<Widget> childrenCopy = getChildren();
        for (int i = childrenCopy.size() - 1; i >= 0; i--) {
            Widget child = childrenCopy.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : scrollY);

            child.hovered = false;

            if (child.isPointOver(x, y)) {
                child.onMouseExit(childRelativeX, childRelativeY);
            }
        }
    }

    /**
     * Handles the mouse move event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onMouseMoveOutside(int x, int y) {
        hovered = false;

        for (Widget child : getChildren()) {
            child.onMouseMoveOutside(x, y);

            child.hovered = false;
        }
    }

    /**
     * Handles the left mouse button down event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onLeftMouseDownOutside(int x, int y) {
        for (Widget child : getChildren()) {
            child.onLeftMouseDownOutside(x, y);
        }
    }

    /**
     * Handles the left mouse button up event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onLeftMouseUpOutside(int x, int y) {
        for (Widget child : getChildren()) {
            child.onLeftMouseUpOutside(x, y);
        }

        lmbPressed = false;
    }

    /**
     * Handles the right mouse button down event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onRightMouseDownOutside(int x, int y) {
        for (Widget child : getChildren()) {
            child.onRightMouseDownOutside(x, y);
        }
    }

    /**
     * Handles the right mouse button up event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onRightMouseUpOutside(int x, int y) {
        for (Widget child : getChildren()) {
            child.onRightMouseUpOutside(x, y);
        }

        rmbPressed = false;
    }

    /**
     * Called when a key is pressed while the {@link Widget} has focus.
     *
     * @param key the code of the key that was pressed
     */
    public void onKeyPress(int key) {
        for (Widget child : getChildren()) {
            child.onKeyPress(key);
        }
    }

    /**
     * Called when a key is released while the {@link Widget} has focus.
     *
     * @param key the code of the key that was released
     */
    public void onKeyRelease(int key) {
        for (Widget child : getChildren()) {
            child.onKeyRelease(key);
        }
    }

    /**
     * Called when a key is held down and repeatedly pressed while the {@link Widget} has focus.
     *
     * @param key the code of the key that is being repeatedly pressed
     */
    public void onKeyRepeat(int key) {
        for (Widget child : getChildren()) {
            child.onKeyRepeat(key);
        }
    }

    /**
     * Sets whether the window should always be on top of other windows.
     *
     * @param alwaysOnTop {@code true} if the window should always be on top; otherwise {@code false}
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    /**
     * Checks if the window is set to always be on top of other windows.
     *
     * @return {@code true} if the window is always on top; otherwise {@code false}
     */
    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    /**
     * Moves this {@link Widget} to the front of the rendering order, ensuring it is drawn above other widgets.
     */
    public void bringToTop() {
        WidgetManager.bringWidgetToTop(this);
    }

    /**
     * Checks if a point with coordinates (x, y) is within the bounds of the {@link Widget}.
     *
     * @param x the x-coordinate of the point to check
     * @param y the y-coordinate of the point to check
     * @return {@code true} if the point is within the bounds of the {@link Widget}, otherwise {@code false}
     */
    public boolean isPointOver(int x, int y) {
        if (!visible) return false;

        return x >= this.x && x <= this.x + width &&
                y >= this.y && y <= this.y + height;
    }

    /**
     * Checks if the mouse pointer is currently over this {@link Widget}.
     *
     * @return {@code true} if the mouse pointer is over the widget, otherwise {@code false}
     */
    public boolean isHovered() {
        return hovered;
    }

    /**
     * Sets the visibility of the {@link Widget}.
     *
     * @param visible {@code true} if the element should be visible, otherwise {@code false}
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns the current visibility of the {@link Widget}.
     *
     * @return {@code true} if the element is visible, otherwise {@code false}
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Checks if this {@link Widget} is at least partially visible within the boundaries of its absolute parent {@link Widget}.
     * If the {@link Widget} has no absolute parent (i.e., it is the root widget), it is considered fully visible by default.
     *
     * @return {@code true} if any part of the widget is within the visible bounds of its absolute parent, {@code false} otherwise.
     */
    public boolean isVisibleWithinParent() {
        if (getAbsoluteParent() == null) return true;

        return absoluteX < getAbsoluteParent().absoluteX + getAbsoluteParent().width &&
                absoluteY < getAbsoluteParent().absoluteY + getAbsoluteParent().height &&
                absoluteX + width > getAbsoluteParent().absoluteX &&
                absoluteY + height > getAbsoluteParent().absoluteY;
    }

    /**
     * Checks if the {@link Widget} is at least partially within the visible bounds of the window.
     *
     * @return {@code true} if any part of the {@link Widget} is within the window bounds, {@code false} otherwise.
     */
    public boolean isVisibleWithinWindow() {
        return absoluteX < WindowUtils.getWindowWidth() &&
                absoluteY < WindowUtils.getWindowHeight() &&
                absoluteX + width > 0 &&
                absoluteY + height > 0;
    }

    /**
     * Returns the relative x-coordinate of the top-left corner of the {@link Widget}.
     *
     * @return the relative x-coordinate of the top-left corner of the {@link Widget} (if not a child, coincides with absolute coordinates)
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the relative y-coordinate of the top-left corner of the {@link Widget}.
     *
     * @return the relative y-coordinate of the top-left corner of the {@link Widget} (if not a child, coincides with absolute coordinates)
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the absolute x-coordinate of the top-left corner of the {@link Widget}.
     *
     * @return the absolute x-coordinate of the top-left corner of the {@link Widget}
     */
    public int getXA() {
        return this.absoluteX;
    }

    /**
     * Returns the absolute y-coordinate of the top-left corner of the {@link Widget}.
     *
     * @return the absolute y-coordinate of the top-left corner of the {@link Widget}
     */
    public int getYA() {
        return this.absoluteY;
    }

    /**
     * Returns the width of the {@link Widget}.
     *
     * @return the width of the {@link Widget}
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the {@link Widget}.
     *
     * @return the height of the {@link Widget}
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the relative x-coordinate of the top-left corner of the {@link Widget}.
     *
     * @param x the new relative x-coordinate of the top-left corner of the {@link Widget}
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the relative y-coordinate of the top-left corner of the {@link Widget}.
     *
     * @param y the new relative y-coordinate of the top-left corner of the {@link Widget}
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Sets the absolute x-coordinate of the top-left corner of the {@link Widget}.
     *
     * @param x the new absolute x-coordinate of the top-left corner of the {@link Widget}
     */
    public void setXA(int x) {
        this.absoluteX = x;
    }

    /**
     * Sets the absolute y-coordinate of the top-left corner of the {@link Widget}.
     *
     * @param y the new absolute y-coordinate of the top-left corner of the {@link Widget}
     */
    public void setYA(int y) {
        this.absoluteY = y;
    }

    /**
     * Sets the width of the {@link Widget}.
     *
     * @param width the new width of the {@link Widget}
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height of the {@link Widget}.
     *
     * @param height the new height of the {@link Widget}
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Adds this {@link Widget} to the screen by registering it with the {@link WidgetManager}.
     * The {@link Widget} will be managed and rendered as part of the UI.
     */
    public void addToScreen() {
        WidgetManager.addWidget(this);
    }

    /**
     * Removes this {@link Widget} from the screen by unregistering it from the {@link WidgetManager}.
     * The {@link Widget} will no longer be managed or rendered as part of the UI.
     */
    public void removeFromScreen() {
        WidgetManager.removeWidget(this);
    }

    /**
     * Draws a rectangle with a stroke and no fill
     *
     * @param x         relative X coordinate of the top left corner of the rectangle
     * @param y         relative Y coordinate of the top left corner of the rectangle
     * @param width     the width of the rectangle
     * @param height    height of the rectangle
     * @param lineWidth the width of the stroke line in pixels
     * @param color     the color of the outline
     */
    public void drawRectOutline(int x, int y, int width, int height, float lineWidth, NanoColor color) {
        NanoDrawer.drawRectOutline(getX() + x, getY() + y, width, height, lineWidth, color);
    }

    /**
     * Draws a rectangle with rounded corners and a stroke without fill.
     *
     * @param x         relative X coordinate of the top left corner of the rectangle
     * @param y         relative Y coordinate of the top left corner of the rectangle
     * @param width     the width of the rectangle
     * @param height    height of the rectangle
     * @param radius    corner radius
     * @param lineWidth the width of the stroke line in pixels
     * @param color     the color of the outline
     */
    public void drawRoundedRectOutline(int x, int y, int width, int height, int radius, float lineWidth, NanoColor color) {
        NanoDrawer.drawRoundedRectOutline(getX() + x, getY() + y, width, height, radius, lineWidth, color);
    }

    /**
     * Draws a filled rectangle with the specified position, size, and color.
     *
     * @param x      relative x-coordinate of the top-left corner of the rectangle
     * @param y      relative y-coordinate of the top-left corner of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param color  the color to fill the rectangle with
     */
    public void drawRect(int x, int y, int width, int height, NanoColor color) {
        NanoDrawer.drawRect(getX() + x, getY() + y, width, height, color);
    }

    /**
     * Draws text on the screen using NanoVG.
     *
     * @param text     the text to be drawn
     * @param fontName the name of the font to be used
     * @param x        relative x-coordinate of the text's position
     * @param y        relative y-coordinate of the text's position
     * @param fontSize the size of the font
     * @param color    the color of the text
     */
    public void drawText(String text, String fontName, int x, int y, int fontSize, NanoColor color) {
        NanoDrawer.drawText(text, fontName, getX() + x, getY() + y, fontSize, color);
    }

    /**
     * Draws a filled rectangle with rounded corners with the specified position, size, radius, and color.
     *
     * @param x      relative x-coordinate of the top-left corner of the rectangle
     * @param y      relative y-coordinate of the top-left corner of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param radius the radius of the corners
     * @param color  the color to fill the rectangle with
     */
    public void drawRoundedRect(int x, int y, int width, int height, float radius, NanoColor color) {
        NanoDrawer.drawRoundedRect(getX() + x, getY() + y, width, height, radius, color);
    }

    /**
     * Draws an ellipse at the specified position with the given size and color.
     *
     * @param x      relative x-coordinate of the ellipse's center
     * @param y      relative y-coordinate of the ellipse's center
     * @param width  the width of the ellipse
     * @param height the height of the ellipse
     * @param color  the color of the ellipse
     */
    public void drawEllipse(int x, int y, int width, int height, NanoColor color) {
        NanoDrawer.drawEllipse(getX() + x, getY() + y, width, height, color);
    }

    /**
     * Draws a line from (x1, y1) to (x2, y2) with the specified color and thickness.
     *
     * @param x1    relative x-coordinate of the start point of the line
     * @param y1    relative y-coordinate of the start point of the line
     * @param x2    relative x-coordinate of the end point of the line
     * @param y2    relative y-coordinate of the end point of the line
     * @param width the thickness of the line
     * @param color the color of the line
     */
    public void drawLine(int x1, int y1, int x2, int y2, float width, NanoColor color) {
        NanoDrawer.drawLine(getX() + x1, getY() + y1, getX() + x2, getY() + y2, width, color);
    }

    /**
     * Draws a circle at the specified position with the given radius and color.
     *
     * @param x      relative x-coordinate of the circle's center
     * @param y      relative y-coordinate of the circle's center
     * @param radius the radius of the circle
     * @param color  the color of the circle
     */
    public void drawCircle(int x, int y, float radius, NanoColor color) {
        NanoDrawer.drawCircle(getX() + x, getY() + y, radius, color);
    }

    /**
     * Draws an arc segment with the specified center, radius, start angle, and end angle.
     *
     * @param x          relative x-coordinate of the center of the arc
     * @param y          relative y-coordinate of the center of the arc
     * @param radius     the radius of the arc
     * @param startAngle the starting angle of the arc (in radians)
     * @param endAngle   the ending angle of the arc (in radians)
     * @param color      the color of the arc segment
     */
    public void drawArc(int x, int y, float radius, float startAngle, float endAngle, NanoColor color) {
        NanoDrawer.drawArc(getX() + x, getY() + y, radius, startAngle, endAngle, color);
    }

    /**
     * Draws an arc segment with the specified center, radius, start angle, end angle, and thickness.
     *
     * @param x          relative x-coordinate of the center of the arc
     * @param y          relative y-coordinate of the center of the arc
     * @param radius     the radius of the arc
     * @param startAngle the starting angle of the arc (in radians)
     * @param endAngle   the ending angle of the arc (in radians)
     * @param thickness  the thickness of the arc segment
     * @param color      the color of the arc segment
     */
    public void drawArc(int x, int y, float radius, float thickness, float startAngle, float endAngle, NanoColor color) {
        NanoDrawer.drawArc(getX() + x, getY() + y, radius, thickness, startAngle, endAngle, color);
    }

    /**
     * Draws an image at the specified position with the given size.
     *
     * @param imageId the identifier of the image to draw
     * @param x       relative x-coordinate of the image's position
     * @param y       relative y-coordinate of the image's position
     * @param width   the width of the image
     * @param height  the height of the image
     * @param opacity image opacity (from 0 to 1)
     */
    public void drawImage(int imageId, int x, int y, int width, int height, float opacity) {
        NanoDrawer.drawImage(imageId, getX() + x, getY() + y, width, height, opacity);
    }

    /**
     * Intersects current scissor rectangle with the specified rectangle.
     * The scissor rectangle is transformed by the current transform.
     * <p>
     * Saves the current render state before starting.
     *
     * @param x      relative x-coordinate of the intersecting scissor region.
     * @param y      relative y-coordinate of the intersecting scissor region.
     * @param width  the width of the intersecting scissor region.
     * @param height the height of the intersecting scissor region.
     */
    public void intersectScissor(int x, int y, int width, int height) {
        NanoDrawer.saveRenderState();
        NanoDrawer.intersectScissor(x + absoluteX, y + absoluteY, width, height);
    }

    /**
     * Widget pre-rendering (before main rendering and updating)
     */
    public void preRender() {
    }

    /**
     * Updates the {@link Widget}
     */
    public void update() {
    }

    /**
     * Renders the {@link Widget}
     */
    abstract public void render();

    /**
     * Final rendering, after the main render and rendering of child elements
     */
    public void postRender() {
    }
}