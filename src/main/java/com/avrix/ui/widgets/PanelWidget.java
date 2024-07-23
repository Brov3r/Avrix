package com.avrix.ui.widgets;

import com.avrix.ui.UIColor;
import zombie.input.Mouse;

/**
 * Represents a panel {@link Widget} with optional rounded corners, borders, and drag functionality.
 */
public class PanelWidget extends Widget {
    private UIColor backgroundColor = UIColor.LIGHT_BLACK;
    private UIColor borderColor = UIColor.WHITE_SMOKE;

    private int borderRadius = 0;
    private int borderWidth = 2;
    private int dragOffsetX, dragOffsetY = 0;

    private boolean dragging = false;
    private boolean drawBorder = true;
    private boolean draggable = false;

    /**
     * Sets whether the {@link Widget} is draggable.
     *
     * @param draggable true if the {@link Widget} is draggable; otherwise false
     */
    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    /**
     * Checks if the {@link Widget} is draggable.
     *
     * @return true if the {@link Widget} is draggable; otherwise false
     */
    public boolean isDraggable() {
        return this.draggable;
    }

    /**
     * Sets the background color of the {@link Widget}.
     *
     * @param color the background color, specified in {@link UIColor}
     */
    public void setBackgroundColor(UIColor color) {
        this.backgroundColor = color;
    }

    /**
     * Sets the border color of the widget.
     *
     * @param color the border color, specified in {@link UIColor}
     */
    public void setBorderColor(UIColor color) {
        this.borderColor = color;
    }

    /**
     * Sets the border width of the widget.
     *
     * @param borderWidth the border width in pixels
     */
    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    /**
     * Sets whether to draw the {@link Widget} border.
     *
     * @param drawBorder true if the border should be drawn; otherwise false
     */
    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }

    /**
     * Sets the radius of the {@link Widget}'s corner rounding.
     *
     * @param radius the radius of the corner rounding in pixels
     */
    public void setBorderRadius(int radius) {
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

        this.dragging = false;
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

        this.dragging = false;
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

        if (draggable) {
            dragging = true;
            dragOffsetX = x;
            dragOffsetY = y;
        }
    }

    /**
     * Updates the {@link Widget} with the given context.
     */
    @Override
    public void update() {
        super.update();
        if (dragging) {
            int mouseX = Mouse.getXA();
            int mouseY = Mouse.getYA();

            setX(mouseX - dragOffsetX);
            setY(mouseY - dragOffsetY);
        }
    }

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
     * @param backgroundColor the background color of the widget, specified in {@link UIColor}
     */
    public PanelWidget(int x, int y, int width, int height, int borderRadius, UIColor backgroundColor) {
        super(x, y, width, height);

        this.backgroundColor = backgroundColor;
        this.borderRadius = borderRadius;
    }

    /**
     * Renders the {@link Widget} with the given context.
     */
    @Override
    public void render() {
        if (borderRadius != 0) {
            drawRoundedRect(getX(), getY(), getWidth(), getHeight(), this.borderRadius, this.backgroundColor);
            if (drawBorder) {
                drawRoundedRectOutline(getX(), getY(), getWidth(), getHeight(), borderRadius, borderWidth, borderColor);
            }
        } else {
            drawRect(getX(), getY(), getWidth(), getHeight(), this.backgroundColor);
            if (drawBorder) {
                drawRectOutline(getX(), getY(), getWidth(), getHeight(), borderWidth, borderColor);
            }
        }
    }
}