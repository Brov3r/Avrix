package com.avrix.ui.widgets;

import com.avrix.ui.NanoColor;
import zombie.input.Mouse;

/**
 * Represents a panel {@link Widget} with optional rounded corners, borders, and drag functionality.
 */
public class PanelWidget extends Widget {
    /**
     * The background color of the panel.
     */
    protected NanoColor backgroundColor = NanoColor.LIGHT_BLACK;

    /**
     * The border color of the panel.
     */
    protected NanoColor borderColor = NanoColor.WHITE_SMOKE;

    /**
     * The radius of the panel's border corners.
     */
    protected int borderRadius = 0;

    /**
     * The width of the panel's border.
     */
    protected int borderWidth = 2;

    /**
     * The x-coordinate offset for dragging the panel.
     */
    protected int dragOffsetX = 0;

    /**
     * The y-coordinate offset for dragging the panel.
     */
    protected int dragOffsetY = 0;

    /**
     * Indicates whether the panel is currently being dragged.
     */
    protected boolean dragging = false;

    /**
     * Indicates whether the panel's border should be drawn.
     */
    protected boolean drawBorder = true;

    /**
     * Indicates whether the panel is draggable.
     */
    protected boolean draggable = false;

    /**
     * Constructs a new {@link Widget} with the specified position and size.
     *
     * @param x      the x-coordinate of the {@link Widget}'s position
     * @param y      the y-coordinate of the {@link Widget}'s position
     * @param width  the width of the {@link Widget}
     * @param height the height of the {@link Widget}
     */
    public PanelWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Constructs a new {@link PanelWidget} with the specified position, size, border radius,
     * and background color.
     *
     * @param x               the X-coordinate of the {@link Widget}'s position
     * @param y               the Y-coordinate of the {@link Widget}'s position
     * @param width           the width of the widget
     * @param height          the height of the widget
     * @param borderRadius    the radius of the corner rounding in pixels
     * @param backgroundColor the background color of the widget, specified in {@link NanoColor}
     */
    public PanelWidget(int x, int y, int width, int height, int borderRadius, NanoColor backgroundColor) {
        super(x, y, width, height);

        this.backgroundColor = backgroundColor;
        this.borderRadius = borderRadius;
    }

    /**
     * Sets whether the {@link Widget} is draggable.
     *
     * @param draggable true if the {@link Widget} is draggable; otherwise false
     */
    public final void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    /**
     * Checks if the {@link Widget} is draggable.
     *
     * @return true if the {@link Widget} is draggable; otherwise false
     */
    public final boolean isDraggable() {
        return this.draggable;
    }

    /**
     * Sets the background color of the {@link Widget}.
     *
     * @param color the background color, specified in {@link NanoColor}
     */
    public final void setBackgroundColor(NanoColor color) {
        this.backgroundColor = color;
    }

    /**
     * Sets the border color of the widget.
     *
     * @param color the border color, specified in {@link NanoColor}
     */
    public final void setBorderColor(NanoColor color) {
        this.borderColor = color;
    }

    /**
     * Sets the border width of the widget.
     *
     * @param borderWidth the border width in pixels
     */
    public final void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    /**
     * Sets whether to draw the {@link Widget} border.
     *
     * @param drawBorder true if the border should be drawn; otherwise false
     */
    public final void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }

    /**
     * Sets the radius of the {@link Widget}'s corner rounding.
     *
     * @param radius the radius of the corner rounding in pixels
     */
    public final void setBorderRadius(int radius) {
        this.borderRadius = radius;
    }

    /**
     * Called when the left mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    @Override
    public void onLeftMouseUp(int x, int y) {
        super.onLeftMouseUp(x, y);

        dragging = false;
    }

    /**
     * Handles the left mouse button up event outside any visible {@link Widget}
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    @Override
    public void onLeftMouseUpOutside(int x, int y) {
        super.onLeftMouseUpOutside(x, y);

        dragging = false;
    }

    /**
     * Called when the left mouse button is pressed down over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    @Override
    public void onLeftMouseDown(int x, int y) {
        super.onLeftMouseDown(x, y);

        boolean canDrag = true;

        for (Widget child : getChildren()) {
            int childRelativeX = x + (child.isScrollLock() ? 0 : scrollX);
            int childRelativeY = y + (child.isScrollLock() ? 0 : scrollY);

            if (child.isPointOver(childRelativeX, childRelativeY)) {
                canDrag = false;
                break;
            }
        }

        if (draggable) {
            dragging = canDrag;
            dragOffsetX = x;
            dragOffsetY = y;
        }
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();
        if (dragging) {
            int mouseX = Mouse.getXA();
            int mouseY = Mouse.getYA();

            setX(mouseX - dragOffsetX);
            setY(mouseY - dragOffsetY);
            
            setXA(parent == null ? x : parent.absoluteX + x);
            setYA(parent == null ? y : parent.absoluteY + y);
        }
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        if (this.borderRadius != 0) {
            drawRoundedRect(0, 0, getWidth(), getHeight(), this.borderRadius, this.backgroundColor);
        } else {
            drawRect(0, 0, getWidth(), getHeight(), this.backgroundColor);
        }
    }

    /**
     * Final rendering, after the main render and rendering of child elements
     */
    @Override
    public void postRender() {
        if (this.drawBorder) {
            if (this.borderRadius != 0) {
                drawRoundedRectOutline(0, 0, getWidth(), getHeight(), this.borderRadius, this.borderWidth, this.borderColor);
            } else {
                drawRectOutline(0, 0, getWidth(), getHeight(), this.borderWidth, this.borderColor);
            }
        }
    }
}