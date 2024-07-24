package com.avrix.ui.widgets;

import com.avrix.ui.UIColor;
import com.avrix.ui.UIContext;
import com.avrix.ui.WidgetManager;
import org.joml.Vector2f;
import org.lwjgl.nanovg.NVGPaint;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.*;

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
     * The x-coordinate of the {@link Widget}'s position.
     */
    protected int x;

    /**
     * The y-coordinate of the {@link Widget}'s position.
     */
    protected int y;

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
     * The rendering context used by the {@link Widget}.
     */
    protected UIContext context;

    /**
     * List of all child elements of the {@link Widget}.
     */
    protected List<Widget> children = new ArrayList<>();

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
    public final boolean isScrollable() {
        return this.scrollable;
    }

    /**
     * Sets the scrollable property of the {@link Widget}.
     *
     * @param scrollable the new scrollable value to set
     */
    public final void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    /**
     * Checks if scrolling is currently locked for this {@link Widget}.
     *
     * @return {@code true} if scrolling is locked, {@code false} otherwise.
     */
    public final boolean isScrollLock() {
        return this.scrollLock;
    }

    /**
     * Sets the scrolling lock state for this {@link Widget}.
     *
     * @param scrollLock {@code true} to lock scrolling, {@code false} to unlock it.
     */
    public final void setScrollLock(boolean scrollLock) {
        this.scrollLock = scrollLock;
    }

    /**
     * Returns the parent {@link Widget} of this {@link Widget}.
     *
     * @return the parent {@link Widget}, or {@code null} if this {@link Widget} does not have a parent
     */
    public final Widget getParent() {
        return this.parent;
    }

    /**
     * Gets the current horizontal scroll offset of the widget.
     *
     * @return the current horizontal scroll offset
     */
    public final int getScrollX() {
        return this.scrollX;
    }

    /**
     * Sets the current horizontal scroll offset of the widget.
     *
     * @param scrollX the new horizontal scroll offset
     */
    public final void setScrollX(int scrollX) {
        this.scrollX = scrollX;
    }

    /**
     * Gets the maximum horizontal scroll offset of the widget.
     * This value represents the farthest point that the content can be scrolled horizontally.
     *
     * @return the maximum horizontal scroll offset
     */
    public final int getMaxScrollX() {
        return this.maxScrollX;
    }

    /**
     * Sets the maximum horizontal scroll offset of the widget.
     * This value determines the limit for horizontal scrolling based on the content width.
     *
     * @param maxScrollX the new maximum horizontal scroll offset
     */
    public final void setMaxScrollX(int maxScrollX) {
        this.maxScrollX = maxScrollX;
    }

    /**
     * Gets the current vertical scroll offset of the widget.
     *
     * @return the current vertical scroll offset
     */
    public final int getScrollY() {
        return this.scrollY;
    }

    /**
     * Sets the current vertical scroll offset of the widget.
     *
     * @param scrollY the new vertical scroll offset
     */
    public final void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }

    /**
     * Gets the maximum vertical scroll offset of the widget.
     * This value represents the farthest point that the content can be scrolled vertically.
     *
     * @return the maximum vertical scroll offset
     */
    public final int getMaxScrollY() {
        return this.maxScrollY;
    }

    /**
     * Sets the maximum vertical scroll offset of the widget.
     * This value determines the limit for vertical scrolling based on the content height.
     *
     * @param maxScrollY the new maximum vertical scroll offset
     */
    public final void setMaxScrollY(int maxScrollY) {
        this.maxScrollY = maxScrollY;
    }

    /**
     * Gets the speed at which the widget scrolls in response to mouse wheel movements.
     * This value determines how many pixels the scroll offset changes per wheel tick.
     *
     * @return the scroll speed
     */
    public final int getScrollSpeed() {
        return this.scrollSpeed;
    }

    /**
     * Sets the speed at which the widget scrolls in response to mouse wheel movements.
     * This value determines how many pixels the scroll offset changes per wheel tick.
     *
     * @param scrollSpeed the new scroll speed
     */
    public final void setScrollSpeed(int scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
    }

    /**
     * Updates and renders all child widgets of this {@link Widget}.
     * This method recursively calls the update and render methods on each child widget,
     * ensuring that the rendering order respects the hierarchy of widgets.
     */
    public void renderChildren() {
        for (Widget child : this.children) {
            if (!child.isVisible()) continue;

            if (child.getContext() == null) {
                child.setContext(this.context);
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
        }
    }

    /**
     * Adds a child widget to this widget's list of children.
     *
     * @param widget the widget to add as a child
     */
    public synchronized void addChild(Widget widget) {
        widget.setContext(this.context);
        widget.parent = this;
        this.children.add(widget);
        updateMaxScrollOffset();
    }

    /**
     * Removes a child widget from this widget's list of children.
     *
     * @param widget the widget to remove from the list of children
     */
    public synchronized void removeChild(Widget widget) {
        widget.parent = null;
        this.children.remove(widget);
        updateMaxScrollOffset();
    }

    /**
     * Gets an unmodifiable view of the list of child widgets.
     *
     * @return an unmodifiable list of child widgets
     */
    public final synchronized List<Widget> getChildren() {
        return Collections.unmodifiableList(this.children);
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
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        // Calculate the bounding box of all children
        for (Widget child : this.children) {
            int childMinX = child.getX();
            int childMinY = child.getY();
            int childMaxX = child.getX() + child.getWidth();
            int childMaxY = child.getY() + child.getHeight();

            if (childMinX < minX) {
                minX = childMinX;
            }
            if (childMinY < minY) {
                minY = childMinY;
            }
            if (childMaxX > maxX) {
                maxX = childMaxX;
            }
            if (childMaxY > maxY) {
                maxY = childMaxY;
            }
        }

        // Calculate the maximum scroll offsets based on the parent's dimensions
        int newMaxScrollX = Math.max(0, maxX - getWidth());
        int newMaxScrollY = Math.max(0, maxY - getHeight());

        // Update the class fields with the calculated values
        this.maxScrollX = newMaxScrollX;
        this.maxScrollY = newMaxScrollY;
    }

    /**
     * Called when the mouse is moved over the {@link Widget}.
     *
     * @param x relative x-coordinate of mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onMouseMove(int x, int y) {
        boolean topWidgetHovered = false;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            Widget child = this.children.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : this.scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : this.scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : this.scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : this.scrollY);

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
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Widget child = this.children.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : this.scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : this.scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : this.scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : this.scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY)) {
                child.onLeftMouseDown(childRelativeX, childRelativeY);
                break;
            } else {
                child.onLeftMouseDownOutside(x, y);
            }
        }
    }

    /**
     * Called when the left mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onLeftMouseUp(int x, int y) {
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Widget child = this.children.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : this.scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : this.scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : this.scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : this.scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY)) {
                child.onLeftMouseUp(childRelativeX, childRelativeY);
                break;
            } else {
                child.onLeftMouseUpOutside(x, y);
            }
        }
    }

    /**
     * Called when the right mouse button is pressed down over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onRightMouseDown(int x, int y) {
        bringToTop();
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Widget child = this.children.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : this.scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : this.scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : this.scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : this.scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY)) {
                child.onRightMouseDown(childRelativeX, childRelativeY);
                break;
            } else {
                child.onRightMouseDownOutside(x, y);
            }
        }
    }

    /**
     * Called when the right mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onRightMouseUp(int x, int y) {
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Widget child = this.children.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : this.scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : this.scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : this.scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : this.scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY)) {
                child.onRightMouseUp(childRelativeX, childRelativeY);
                break;
            } else {
                child.onRightMouseUpOutside(x, y);
            }
        }
    }

    /**
     * Called when the mouse wheel is scrolled over the {@link Widget}.
     *
     * @param x     relative x-coordinate of the mouse position
     * @param y     relative y-coordinate of the mouse position
     * @param delta direction of mouse wheel movement - (1 - up, -1 - down)
     */
    public void onMouseWheel(int x, int y, int delta) {
        // Update scroll position
        if (this.scrollable) {
            this.scrollY -= delta * this.scrollSpeed;
        }

        // Limit the scroll values
        if (this.scrollY < 0) {
            this.scrollY = 0;
        } else if (this.scrollY > this.maxScrollY) {
            this.scrollY = this.maxScrollY;
        }

        // Update child widgets based on the new scroll values
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Widget child = this.children.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : this.scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : this.scrollY);

            int scrollAbsoluteX = x + (child.isScrollLock() ? 0 : this.scrollX);
            int scrollAbsoluteY = y + (child.isScrollLock() ? 0 : this.scrollY);

            if (child.isPointOver(scrollAbsoluteX, scrollAbsoluteY)) {
                child.onMouseWheel(childRelativeX, childRelativeY, delta);
                break;
            } else {
                child.onMouseWheelOutside(x, y, delta);
            }
        }
    }

    /**
     * Called when the mouse cursor enters the bounds of this {@link Widget}.
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onMouseEnter(int x, int y) {
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Widget child = this.children.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : this.scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : this.scrollY);
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
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Widget child = this.children.get(i);
            int childRelativeX = x - child.getX() + (child.isScrollLock() ? 0 : this.scrollX);
            int childRelativeY = y - child.getY() + (child.isScrollLock() ? 0 : this.scrollY);

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
        for (Widget child : this.children) {
            child.onMouseMoveOutside(x, y);
        }
    }

    /**
     * Handles the left mouse button down event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onLeftMouseDownOutside(int x, int y) {
        for (Widget child : this.children) {
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
        for (Widget child : this.children) {
            child.onLeftMouseUpOutside(x, y);
        }
    }

    /**
     * Handles the right mouse button down event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onRightMouseDownOutside(int x, int y) {
        for (Widget child : this.children) {
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
        for (Widget child : this.children) {
            child.onRightMouseUpOutside(x, y);
        }
    }

    /**
     * Handles the mouse wheel event outside any visible widget
     *
     * @param x     absolute x-coordinate of the mouse position
     * @param y     absolute y-coordinate of the mouse position
     * @param delta the amount of wheel movement
     */
    public void onMouseWheelOutside(int x, int y, int delta) {
        for (Widget child : this.children) {
            child.onMouseWheelOutside(x, y, delta);
        }
    }

    /**
     * Called when a key is pressed while the {@link Widget} has focus.
     *
     * @param key the code of the key that was pressed
     */
    public void onKeyPress(int key) {
        for (Widget child : this.children) {
            child.onKeyPress(key);
        }
    }

    /**
     * Called when a key is released while the {@link Widget} has focus.
     *
     * @param key the code of the key that was released
     */
    public void onKeyRelease(int key) {
        for (Widget child : this.children) {
            child.onKeyRelease(key);
        }
    }

    /**
     * Called when a key is held down and repeatedly pressed while the {@link Widget} has focus.
     *
     * @param key the code of the key that is being repeatedly pressed
     */
    public void onKeyRepeat(int key) {
        for (Widget child : this.children) {
            child.onKeyRepeat(key);
        }
    }

    /**
     * Sets whether the window should always be on top of other windows.
     *
     * @param alwaysOnTop {@code true} if the window should always be on top; otherwise {@code false}
     */
    public final void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    /**
     * Checks if the window is set to always be on top of other windows.
     *
     * @return {@code true} if the window is always on top; otherwise {@code false}
     */
    public final boolean isAlwaysOnTop() {
        return this.alwaysOnTop;
    }

    /**
     * Moves this {@link Widget} to the front of the rendering order, ensuring it is drawn above other widgets.
     */
    public final void bringToTop() {
        WidgetManager.bringWidgetToTop(this);
    }

    /**
     * Checks if a point with coordinates (x, y) is within the bounds of the {@link Widget}.
     *
     * @param x the x-coordinate of the point to check
     * @param y the y-coordinate of the point to check
     * @return {@code true} if the point is within the bounds of the {@link Widget}, otherwise {@code false}
     */
    public final boolean isPointOver(int x, int y) {
        if (!this.visible) return false;

        return x >= this.x && x <= this.x + this.width &&
                y >= this.y && y <= this.y + this.height;
    }

    /**
     * Checks if the mouse pointer is currently over this {@link Widget}.
     *
     * @return {@code true} if the mouse pointer is over the widget, otherwise {@code false}
     */
    public final boolean isHovered() {
        return this.hovered;
    }

    /**
     * Sets the visibility of the {@link Widget}.
     *
     * @param visible {@code true} if the element should be visible, otherwise {@code false}
     */
    public final void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns the current visibility of the {@link Widget}.
     *
     * @return {@code true} if the element is visible, otherwise {@code false}
     */
    public final boolean isVisible() {
        return this.visible;
    }

    /**
     * Returns the x-coordinate of the top-left corner of the {@link Widget}.
     *
     * @return the x-coordinate of the top-left corner of the {@link Widget}
     */
    public final int getX() {
        return this.x;
    }

    /**
     * Returns the y-coordinate of the top-left corner of the {@link Widget}.
     *
     * @return the y-coordinate of the top-left corner of the {@link Widget}
     */
    public final int getY() {
        return this.y;
    }

    /**
     * Returns the width of the {@link Widget}.
     *
     * @return the width of the {@link Widget}
     */
    public final int getWidth() {
        return this.width;
    }

    /**
     * Returns the height of the {@link Widget}.
     *
     * @return the height of the {@link Widget}
     */
    public final int getHeight() {
        return this.height;
    }

    /**
     * Sets the x-coordinate of the top-left corner of the {@link Widget}.
     *
     * @param x the new x-coordinate of the top-left corner of the {@link Widget}
     */
    public final void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the y-coordinate of the top-left corner of the {@link Widget}.
     *
     * @param y the new y-coordinate of the top-left corner of the {@link Widget}
     */
    public final void setY(int y) {
        this.y = y;
    }

    /**
     * Sets the width of the {@link Widget}.
     *
     * @param width the new width of the {@link Widget}
     */
    public final void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height of the {@link Widget}.
     *
     * @param height the new height of the {@link Widget}
     */
    public final void setHeight(int height) {
        this.height = height;
    }

    /**
     * Adds this {@link Widget} to the screen by registering it with the {@link WidgetManager}.
     * The {@link Widget} will be managed and rendered as part of the UI.
     */
    public final void addToScreen() {
        WidgetManager.addWidget(this);
    }

    /**
     * Removes this {@link Widget} from the screen by unregistering it from the {@link WidgetManager}.
     * The {@link Widget} will no longer be managed or rendered as part of the UI.
     */
    public final void removeFromScreen() {
        WidgetManager.removeWidget(this);
    }

    /**
     * Sets the {@link UIContext} for this {@link Widget}.
     *
     * @param context the {@link UIContext} to be set
     */
    public final void setContext(UIContext context) {
        this.context = context;
    }

    /**
     * Returns the current {@link UIContext} for this {@link Widget}.
     *
     * @return the {@link UIContext} currently associated with this {@link Widget}
     */
    public final UIContext getContext() {
        return this.context;
    }

    /**
     * Returns the current {@link UIContext} ID for this {@link Widget}.
     *
     * @return the {@link UIContext} ID currently associated with this {@link Widget}
     */
    public final long getContextID() {
        return this.context.get();
    }

    /**
     * Sets the current scissor rectangle.
     * The scissor rectangle is transformed by the current transform.
     *
     * @param x      absolute x-coordinate of the scissor region.
     * @param y      absolute y-coordinate of the scissor region.
     * @param width  the width of the scissor region.
     * @param height the height of the scissor region.
     */
    public final void scissor(int x, int y, int width, int height) {
        nvgScissor(getContextID(), x, y, width, height);
    }

    /**
     * Intersects current scissor rectangle with the specified rectangle.
     * The scissor rectangle is transformed by the current transform.
     * Note: in case the rotation of previous scissor rect differs from the current one, the intersection will be done
     * between the specified rectangle and the previous scissor rectangle transformed in the current transform space. The resulting shape is always rectangle.
     *
     * @param x      absolute x-coordinate of the intersecting scissor region.
     * @param y      absolute y-coordinate of the intersecting scissor region.
     * @param width  the width of the intersecting scissor region.
     * @param height the height of the intersecting scissor region.
     */
    public final void intersectScissor(int x, int y, int width, int height) {
        nvgIntersectScissor(getContextID(), x, y, width, height);
    }

    /**
     * Resets and disables scissoring.
     */
    public final void resetScissor() {
        nvgResetScissor(getContextID());
    }

    /**
     * Pushes and saves the current render state into a state stack. A matching {@link Widget#restoreRenderState()} must be used to restore the state.
     */
    public final void saveRenderState() {
        nvgSave(getContextID());
    }

    /**
     * Pops and restores current render state.
     */
    public final void restoreRenderState() {
        nvgRestore(getContextID());
    }

    /**
     * Resets current render state to default values. Does not affect the render state stack.
     */
    public final void resetRenderState() {
        nvgReset(getContextID());
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
    public final void drawRectOutline(int x, int y, int width, int height, float lineWidth, UIColor color) {
        nvgBeginPath(getContextID());
        nvgRect(getContextID(), getX() + x, getY() + y, width, height);
        color.tallocNVG(nvgColor -> nvgStrokeColor(getContextID(), nvgColor));
        nvgStrokeWidth(getContextID(), lineWidth);
        nvgStroke(getContextID());
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
    public final void drawRoundedRectOutline(int x, int y, int width, int height, int radius, float lineWidth, UIColor color) {
        nvgBeginPath(getContextID());
        nvgRoundedRect(getContextID(), getX() + x, getY() + y, width, height, radius);
        color.tallocNVG(nvgColor -> nvgStrokeColor(getContextID(), nvgColor));
        nvgStrokeWidth(getContextID(), lineWidth);
        nvgStroke(getContextID());
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
    public final void drawRect(int x, int y, int width, int height, UIColor color) {
        nvgBeginPath(getContextID());
        nvgRect(getContextID(), getX() + x, getY() + y, width, height);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
    public final void drawText(String text, String fontName, int x, int y, int fontSize, UIColor color) {
        Vector2f textSize = getTextSize(text, fontName, fontSize);

        int horizontalOffset = 1;

        nvgFontFace(getContextID(), fontName);
        nvgFontSize(getContextID(), fontSize);
        nvgBeginPath(getContextID());
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgText(getContextID(), getX() + x + horizontalOffset, getY() + y + textSize.y, text);
        nvgFill(getContextID());
        nvgClosePath(getContextID());
    }

    /**
     * Calculates the width and height of the given text when rendered with the specified font and size.
     *
     * @param text     the text whose dimensions are to be calculated
     * @param fontName the name of the font to use
     * @param fontSize the size of the font
     * @return an array containing two elements: the width (index 0) and the height (index 1) of the text
     */
    public final Vector2f getTextSize(String text, String fontName, int fontSize) {
        nvgFontFace(getContextID(), fontName);
        nvgFontSize(getContextID(), fontSize);

        float[] bounds = new float[4];

        nvgTextBounds(getContextID(), 0, 0, text, bounds);

        float width = bounds[2] - bounds[0];
        float height = bounds[3] - bounds[1];

        return new Vector2f(width, height);
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
    public final void drawRoundedRect(int x, int y, int width, int height, float radius, UIColor color) {
        nvgBeginPath(getContextID());
        nvgRoundedRect(getContextID(), getX() + x, getY() + y, width, height, radius);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
    public final void drawEllipse(int x, int y, int width, int height, UIColor color) {
        nvgBeginPath(getContextID());
        nvgEllipse(getContextID(), getX() + x, getY() + y, (float) width / 2, (float) height / 2);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
    public final void drawLine(int x1, int y1, int x2, int y2, float width, UIColor color) {
        nvgBeginPath(getContextID());
        nvgMoveTo(getContextID(), getX() + x1, getY() + y1);
        nvgLineTo(getContextID(), getX() + x2, getY() + y2);
        nvgStrokeWidth(getContextID(), width);
        color.tallocNVG(nvgColor -> nvgStrokeColor(getContextID(), nvgColor));
        nvgStroke(getContextID());
    }

    /**
     * Draws a circle at the specified position with the given radius and color.
     *
     * @param x      relative x-coordinate of the circle's center
     * @param y      relative y-coordinate of the circle's center
     * @param radius the radius of the circle
     * @param color  the color of the circle
     */
    public final void drawCircle(int x, int y, float radius, UIColor color) {
        nvgBeginPath(getContextID());
        nvgCircle(getContextID(), getX() + x, getY() + y, radius);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
    public final void drawArc(int x, int y, float radius, float startAngle, float endAngle, UIColor color) {
        nvgBeginPath(getContextID());
        nvgArc(getContextID(), getX() + x, getY() + y, radius, startAngle, endAngle, NVG_CW);
        nvgLineTo(getContextID(), getX() + x, getY() + y);
        nvgClosePath(getContextID());
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
    public final void drawArc(int x, int y, float radius, float thickness, float startAngle, float endAngle, UIColor color) {
        nvgBeginPath(getContextID());
        nvgArc(getContextID(), getX() + x, getY() + y, radius, startAngle, endAngle, NVG_CW);
        nvgLineTo(getContextID(), getX() + x + (float) Math.cos(endAngle) * radius, getY() + y + (float) Math.sin(endAngle) * radius);
        nvgArc(getContextID(), getX() + x, getY() + y, radius - thickness, endAngle, startAngle, NVG_CCW);
        nvgLineTo(getContextID(), getX() + x + (float) Math.cos(startAngle) * radius, getY() + y + (float) Math.sin(startAngle) * radius);
        nvgClosePath(getContextID());
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
    }

    /**
     * Draws an image at the specified position with the given size.
     *
     * @param imageId the identifier of the image to draw
     * @param x       relative x-coordinate of the image's position
     * @param y       relative y-coordinate of the image's position
     * @param width   the width of the image
     * @param height  the height of the image
     */
    public final void drawImage(int imageId, int x, int y, int width, int height) {
        NVGPaint paint = nvgImagePattern(getContextID(), getX() + x, getY() + y, width, height, 0, imageId, 1, NVGPaint.create());
        nvgBeginPath(getContextID());
        nvgRect(getContextID(), getX() + x, getY() + y, width, height);
        nvgFillPaint(getContextID(), paint);
        nvgFill(getContextID());
    }

    /**
     * Draws an image at the specified position with the given size.
     *
     * @param imagePath path to the image
     * @param x         relative x-coordinate of the image's position
     * @param y         relative y-coordinate of the image's position
     * @param width     the width of the image
     * @param height    the height of the image
     */
    public final void drawImage(Path imagePath, int x, int y, int width, int height) {
        int imageID = nvgCreateImage(getContextID(), imagePath.toString(), NVG_IMAGE_NEAREST);
        drawImage(imageID, getX() + x, getY() + y, width, height);
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