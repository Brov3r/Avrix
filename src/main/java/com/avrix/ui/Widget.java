package com.avrix.ui;

import org.lwjgl.nanovg.NVGPaint;
import zombie.input.Mouse;

import java.nio.file.Path;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Abstract base class for all UI {@link Widget}'s. This class provides the basic interface and functionality
 * for UI elements that can be rendered and interacted with.
 */
public abstract class Widget {
    /**
     * Indicates whether this {@link Widget} is visible or not.
     */
    private boolean visible = true;

    /**
     * The x-coordinate of the top-left corner of this {@link Widget}.
     */
    private int x;

    /**
     * The y-coordinate of the top-left corner of this {@link Widget}.
     */
    private int y;

    /**
     * The width of this {@link Widget}.
     */
    private int width;

    /**
     * The height of this {@link Widget}.
     */
    private int height;

    private UIContext context;

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
    }

    /**
     * Called when the mouse is moved over the {@link Widget}.
     *
     * @param context the UI context in which the event occurred
     * @param x       the x-coordinate of the mouse position
     * @param y       the y-coordinate of the mouse position
     */
    public void onMouseMove(UIContext context, float x, float y) {
    }

    /**
     * Called when the left mouse button is pressed down over the {@link Widget}.
     *
     * @param context the UI context in which the event occurred
     * @param x       the x-coordinate of the mouse position
     * @param y       the y-coordinate of the mouse position
     */
    public void onLeftMouseDown(UIContext context, float x, float y) {
    }

    /**
     * Called when the left mouse button is released over the {@link Widget}.
     *
     * @param context the UI context in which the event occurred
     * @param x       the x-coordinate of the mouse position
     * @param y       the y-coordinate of the mouse position
     */
    public void onLeftMouseUp(UIContext context, float x, float y) {
    }

    /**
     * Called when the right mouse button is pressed down over the {@link Widget}.
     *
     * @param context the UI context in which the event occurred
     * @param x       the x-coordinate of the mouse position
     * @param y       the y-coordinate of the mouse position
     */
    public void onRightMouseDown(UIContext context, float x, float y) {
    }

    /**
     * Called when the right mouse button is released over the {@link Widget}.
     *
     * @param context the UI context in which the event occurred
     * @param x       the x-coordinate of the mouse position
     * @param y       the y-coordinate of the mouse position
     */
    public void onRightMouseUp(UIContext context, float x, float y) {
    }

    /**
     * Called when the mouse wheel is scrolled over the {@link Widget}.
     *
     * @param context the UI context in which the event occurred
     * @param x       the x-coordinate of the mouse position
     * @param y       the y-coordinate of the mouse position
     * @param delta   the amount of scrolling; positive values for scrolling up, negative for scrolling down
     */
    public void onMouseWheel(UIContext context, float x, float y, float delta) {
    }

    /**
     * Called when a key is pressed while the {@link Widget} has focus.
     *
     * @param context the UI context in which the event occurred
     * @param key     the code of the key that was pressed
     */
    public void onKeyPress(UIContext context, int key) {
    }

    /**
     * Called when a key is released while the {@link Widget} has focus.
     *
     * @param context the UI context in which the event occurred
     * @param key     the code of the key that was released
     */
    public void onKeyRelease(UIContext context, int key) {
    }

    /**
     * Called when a key is held down and repeatedly pressed while the {@link Widget} has focus.
     *
     * @param context the UI context in which the event occurred
     * @param key     the code of the key that is being repeatedly pressed
     */
    public void onKeyRepeat(UIContext context, int key) {
    }

    /**
     * Checks if a point with coordinates (x, y) is within the bounds of the {@link Widget}.
     *
     * @param x the x-coordinate of the point to check
     * @param y the y-coordinate of the point to check
     * @return {@code true} if the point is within the bounds of the {@link Widget}, otherwise {@code false}
     */
    public boolean isPointOver(int x, int y) {
        if (!this.visible) return false;

        return x >= this.x && x <= this.x + this.width &&
                y >= this.y && y <= this.y + this.height;
    }

    /**
     * Checks if the mouse pointer is currently over this {@link Widget}.
     *
     * @return {@code true} if the mouse pointer is over the widget, otherwise {@code false}
     */
    public boolean isHovered() {
        return isPointOver(Mouse.getXA(), Mouse.getYA());
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
        return this.visible;
    }

    /**
     * Returns the x-coordinate of the top-left corner of the {@link Widget}.
     *
     * @return the x-coordinate of the top-left corner of the {@link Widget}
     */
    public int getX() {
        return this.x;
    }

    /**
     * Returns the y-coordinate of the top-left corner of the {@link Widget}.
     *
     * @return the y-coordinate of the top-left corner of the {@link Widget}
     */
    public int getY() {
        return this.y;
    }

    /**
     * Returns the width of the {@link Widget}.
     *
     * @return the width of the {@link Widget}
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Returns the height of the {@link Widget}.
     *
     * @return the height of the {@link Widget}
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Sets the x-coordinate of the top-left corner of the {@link Widget}.
     *
     * @param x the new x-coordinate of the top-left corner of the {@link Widget}
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the y-coordinate of the top-left corner of the {@link Widget}.
     *
     * @param y the new y-coordinate of the top-left corner of the {@link Widget}
     */
    public void setY(int y) {
        this.y = y;
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
     * Removes this {@link Widget} from the screen by deregistering it from the {@link WidgetManager}.
     * The {@link Widget} will no longer be managed or rendered as part of the UI.
     */
    public void removeFromScreen() {
        WidgetManager.removeWidget(this);
    }

    /**
     * Sets the {@link UIContext} for this {@link Widget}.
     *
     * @param context the {@link UIContext} to be set
     */
    public void setContext(UIContext context) {
        this.context = context;
    }

    /**
     * Returns the current {@link UIContext} for this {@link Widget}.
     *
     * @return the {@link UIContext} currently associated with this {@link Widget}
     */
    public UIContext getContext() {
        return this.context;
    }

    /**
     * Draws a filled rectangle with the specified position, size, and color.
     *
     * @param x      the x-coordinate of the top-left corner of the rectangle
     * @param y      the y-coordinate of the top-left corner of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param color  the color to fill the rectangle with
     */
    public void drawRect(int x, int y, int width, int height, UIColor color) {
        nvgBeginPath(context.get());
        nvgRect(context.get(), x, y, width, height);
        color.tallocNVG(nvgColor -> {
            nvgFillColor(context.get(), nvgColor);
        });
        nvgFill(context.get());
    }

    /**
     * Draws text on the screen using NanoVG.
     *
     * @param text     the text to be drawn
     * @param fontName the name of the font to be used
     * @param x        the x-coordinate of the text's position
     * @param y        the y-coordinate of the text's position
     * @param fontSize the size of the font
     * @param color    the color of the text
     */
    public void drawText(String text, String fontName, int x, int y, int fontSize, UIColor color) {
        nvgFontFace(context.get(), fontName);
        nvgFontSize(context.get(), fontSize);
        nvgBeginPath(context.get());
        color.tallocNVG(nvgColor -> nvgFillColor(context.get(), nvgColor));
        nvgText(context.get(), x, y, text);
        nvgFill(context.get());
        nvgClosePath(context.get());
    }

    /**
     * Draws a filled rectangle with rounded corners with the specified position, size, radius, and color.
     *
     * @param x      the x-coordinate of the top-left corner of the rectangle
     * @param y      the y-coordinate of the top-left corner of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param radius the radius of the corners
     * @param color  the color to fill the rectangle with
     */
    public void drawRoundedRect(int x, int y, int width, int height, float radius, UIColor color) {
        nvgBeginPath(context.get());
        nvgRoundedRect(context.get(), x, y, width, height, radius);
        color.tallocNVG(nvgColor -> {
            nvgFillColor(context.get(), nvgColor);
        });
        nvgFill(context.get());
    }

    /**
     * Draws an ellipse at the specified position with the given size and color.
     *
     * @param x      the x-coordinate of the ellipse's center
     * @param y      the y-coordinate of the ellipse's center
     * @param width  the width of the ellipse
     * @param height the height of the ellipse
     * @param color  the color of the ellipse
     */
    public void drawEllipse(int x, int y, int width, int height, UIColor color) {
        nvgBeginPath(context.get());
        nvgEllipse(context.get(), x, y, (float) width / 2, (float) height / 2);
        color.tallocNVG(nvgColor -> {
            nvgFillColor(context.get(), nvgColor);
        });
        nvgFill(context.get());
    }

    /**
     * Draws a line from (x1, y1) to (x2, y2) with the specified color and thickness.
     *
     * @param x1    the x-coordinate of the start point of the line
     * @param y1    the y-coordinate of the start point of the line
     * @param x2    the x-coordinate of the end point of the line
     * @param y2    the y-coordinate of the end point of the line
     * @param width the thickness of the line
     * @param color the color of the line
     */
    public void drawLine(int x1, int y1, int x2, int y2, float width, UIColor color) {
        nvgBeginPath(context.get());
        nvgMoveTo(context.get(), x1, y1);
        nvgLineTo(context.get(), x2, y2);
        nvgStrokeWidth(context.get(), width);
        color.tallocNVG(nvgColor -> {
            nvgStrokeColor(context.get(), nvgColor);
        });
        nvgStroke(context.get());
    }

    /**
     * Draws a circle at the specified position with the given radius and color.
     *
     * @param x      the x-coordinate of the circle's center
     * @param y      the y-coordinate of the circle's center
     * @param radius the radius of the circle
     * @param color  the color of the circle
     */
    public void drawCircle(int x, int y, float radius, UIColor color) {
        nvgBeginPath(context.get());
        nvgCircle(context.get(), x, y, radius);
        color.tallocNVG(nvgColor -> {
            nvgFillColor(context.get(), nvgColor);
        });
        nvgFill(context.get());
    }

    /**
     * Draws an arc segment with the specified center, radius, start angle, and end angle.
     *
     * @param x          the x-coordinate of the center of the arc
     * @param y          the y-coordinate of the center of the arc
     * @param radius     the radius of the arc
     * @param startAngle the starting angle of the arc (in radians)
     * @param endAngle   the ending angle of the arc (in radians)
     * @param color      the color of the arc segment
     */
    public void drawArc(int x, int y, float radius, float startAngle, float endAngle, UIColor color) {
        nvgBeginPath(context.get());
        nvgArc(context.get(), x, y, radius, startAngle, endAngle, NVG_CW);
        nvgLineTo(context.get(), x, y);
        nvgClosePath(context.get());
        color.tallocNVG(nvgColor -> {
            nvgFillColor(context.get(), nvgColor);
        });
        nvgFill(context.get());
    }

    /**
     * Draws an arc segment with the specified center, radius, start angle, end angle, and thickness.
     *
     * @param x          the x-coordinate of the center of the arc
     * @param y          the y-coordinate of the center of the arc
     * @param radius     the radius of the arc
     * @param startAngle the starting angle of the arc (in radians)
     * @param endAngle   the ending angle of the arc (in radians)
     * @param thickness  the thickness of the arc segment
     * @param color      the color of the arc segment
     */
    public void drawArc(int x, int y, float radius, float thickness, float startAngle, float endAngle, UIColor color) {
        nvgBeginPath(context.get());
        nvgArc(context.get(), x, y, radius, startAngle, endAngle, NVG_CW);
        nvgLineTo(context.get(), x + (float) Math.cos(endAngle) * radius, y + (float) Math.sin(endAngle) * radius);
        nvgArc(context.get(), x, y, radius - thickness, endAngle, startAngle, NVG_CCW);
        nvgLineTo(context.get(), x + (float) Math.cos(startAngle) * radius, y + (float) Math.sin(startAngle) * radius);
        nvgClosePath(context.get());
        color.tallocNVG(nvgColor -> {
            nvgFillColor(context.get(), nvgColor);
        });
        nvgFill(context.get());
    }

    /**
     * Draws an image at the specified position with the given size.
     *
     * @param imageId the identifier of the image to draw
     * @param x       the x-coordinate of the image's position
     * @param y       the y-coordinate of the image's position
     * @param width   the width of the image
     * @param height  the height of the image
     */
    public void drawImage(int imageId, int x, int y, int width, int height) {
        NVGPaint paint = nvgImagePattern(context.get(), x, y, width, height, 0, imageId, 1, NVGPaint.create());
        nvgBeginPath(context.get());
        nvgRect(context.get(), x, y, width, height);
        nvgFillPaint(context.get(), paint);
        nvgFill(context.get());
    }

    /**
     * Draws an image at the specified position with the given size.
     *
     * @param imagePath path to the image
     * @param x         the x-coordinate of the image's position
     * @param y         the y-coordinate of the image's position
     * @param width     the width of the image
     * @param height    the height of the image
     */
    public void drawImage(Path imagePath, int x, int y, int width, int height) {
        int imageID = nvgCreateImage(context.get(), imagePath.toString(), NVG_IMAGE_NEAREST);
        drawImage(imageID, x, y, width, height);
    }

    /**
     * Updates the {@link Widget} with the given context.
     */
    public void update() {
    }

    /**
     * Renders the {@link Widget} with the given context.
     */
    abstract public void render();
}