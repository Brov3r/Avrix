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
    private boolean visible = true;
    private boolean alwaysOnTop = false;

    private int x;
    private int y;
    private int width;
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
     * @param x relative x-coordinate of mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onMouseMove(int x, int y) {
    }

    /**
     * Called when the left mouse button is pressed down over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onLeftMouseDown(int x, int y) {
        bringToTop();
    }

    /**
     * Called when the left mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onLeftMouseUp(int x, int y) {
    }

    /**
     * Called when the right mouse button is pressed down over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onRightMouseDown(int x, int y) {
        bringToTop();
    }

    /**
     * Called when the right mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    public void onRightMouseUp(int x, int y) {
    }

    /**
     * Called when the mouse wheel is scrolled over the {@link Widget}.
     *
     * @param x     relative x-coordinate of the mouse position
     * @param y     relative y-coordinate of the mouse position
     * @param delta direction of mouse wheel movement - (1 - up, -1 - down)
     */
    public void onMouseWheel(int x, int y, int delta) {
    }

    /**
     * Handles the mouse move event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onMouseMoveOutside(int x, int y) {
    }

    /**
     * Handles the left mouse button down event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onLeftMouseDownOutside(int x, int y) {
    }

    /**
     * Handles the left mouse button up event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onLeftMouseUpOutside(int x, int y) {
    }

    /**
     * Handles the right mouse button down event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onRightMouseDownOutside(int x, int y) {
    }

    /**
     * Handles the right mouse button up event outside any visible widget
     *
     * @param x absolute x-coordinate of the mouse position
     * @param y absolute y-coordinate of the mouse position
     */
    public void onRightMouseUpOutside(int x, int y) {
    }

    /**
     * Handles the mouse wheel event outside any visible widget
     *
     * @param x     absolute x-coordinate of the mouse position
     * @param y     absolute y-coordinate of the mouse position
     * @param delta the amount of wheel movement
     */
    public void onMouseWheelOutside(int x, int y, int delta) {
    }

    /**
     * Called when a key is pressed while the {@link Widget} has focus.
     *
     * @param key the code of the key that was pressed
     */
    public void onKeyPress(int key) {
    }

    /**
     * Called when a key is released while the {@link Widget} has focus.
     *
     * @param key the code of the key that was released
     */
    public void onKeyRelease(int key) {
    }

    /**
     * Called when a key is held down and repeatedly pressed while the {@link Widget} has focus.
     *
     * @param key the code of the key that is being repeatedly pressed
     */
    public void onKeyRepeat(int key) {
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
        return this.alwaysOnTop;
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
    public final void addToScreen() {
        WidgetManager.addWidget(this);
    }

    /**
     * Removes this {@link Widget} from the screen by deregistering it from the {@link WidgetManager}.
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
     * Sets the scissor region for NanoVG. Only content inside this region will be rendered.
     *
     * @param x      the x-coordinate of the scissor region.
     * @param y      the y-coordinate of the scissor region.
     * @param width  the width of the scissor region.
     * @param height the height of the scissor region.
     */
    public void startScissor(int x, int y, int width, int height) {
        nvgScissor(getContextID(), x, y, width, height);
    }

    /**
     * Sets the intersecting scissor region for NanoVG. Only content inside this intersection of
     * this region and the current scissor region will be rendered.
     *
     * @param x      the x-coordinate of the intersecting scissor region.
     * @param y      the y-coordinate of the intersecting scissor region.
     * @param width  the width of the intersecting scissor region.
     * @param height the height of the intersecting scissor region.
     */
    public void startIntersectScissor(int x, int y, int width, int height) {
        nvgIntersectScissor(getContextID(), x, y, width, height);
    }

    /**
     * Resets the scissor region, allowing rendering to occur over the entire canvas.
     */
    public void endScissor() {
        nvgResetScissor(getContextID());
    }

    /**
     * Draws a rectangle with a stroke and no fill
     *
     * @param x         the X coordinate of the top left corner of the rectangle
     * @param y         the Y coordinate of the top left corner of the rectangle
     * @param width     the width of the rectangle
     * @param height    height of the rectangle
     * @param lineWidth the width of the stroke line in pixels
     * @param color     the color of the outline
     */
    public void drawRectOutline(int x, int y, int width, int height, float lineWidth, UIColor color) {
        nvgBeginPath(getContextID());
        nvgRect(getContextID(), x, y, width, height);
        color.tallocNVG(nvgColor -> nvgStrokeColor(getContextID(), nvgColor));
        nvgStrokeWidth(getContextID(), lineWidth);
        nvgStroke(getContextID());
    }

    /**
     * Draws a rectangle with rounded corners and a stroke without fill.
     *
     * @param x         the X coordinate of the top left corner of the rectangle
     * @param y         the Y coordinate of the top left corner of the rectangle
     * @param width     the width of the rectangle
     * @param height    height of the rectangle
     * @param radius    corner radius
     * @param lineWidth the width of the stroke line in pixels
     * @param color     the color of the outline
     */
    public void drawRoundedRectOutline(int x, int y, int width, int height, int radius, float lineWidth, UIColor color) {
        nvgBeginPath(getContextID());
        nvgRoundedRect(getContextID(), x, y, width, height, radius);
        color.tallocNVG(nvgColor -> nvgStrokeColor(getContextID(), nvgColor));
        nvgStrokeWidth(getContextID(), lineWidth);
        nvgStroke(getContextID());
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
        nvgBeginPath(getContextID());
        nvgRect(getContextID(), x, y, width, height);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
        nvgFontFace(getContextID(), fontName);
        nvgFontSize(getContextID(), fontSize);
        nvgBeginPath(getContextID());
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgText(getContextID(), x, y, text);
        nvgFill(getContextID());
        nvgClosePath(getContextID());
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
        nvgBeginPath(getContextID());
        nvgRoundedRect(getContextID(), x, y, width, height, radius);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
        nvgBeginPath(getContextID());
        nvgEllipse(getContextID(), x, y, (float) width / 2, (float) height / 2);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
        nvgBeginPath(getContextID());
        nvgMoveTo(getContextID(), x1, y1);
        nvgLineTo(getContextID(), x2, y2);
        nvgStrokeWidth(getContextID(), width);
        color.tallocNVG(nvgColor -> nvgStrokeColor(getContextID(), nvgColor));
        nvgStroke(getContextID());
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
        nvgBeginPath(getContextID());
        nvgCircle(getContextID(), x, y, radius);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
        nvgBeginPath(getContextID());
        nvgArc(getContextID(), x, y, radius, startAngle, endAngle, NVG_CW);
        nvgLineTo(getContextID(), x, y);
        nvgClosePath(getContextID());
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
        nvgBeginPath(getContextID());
        nvgArc(getContextID(), x, y, radius, startAngle, endAngle, NVG_CW);
        nvgLineTo(getContextID(), x + (float) Math.cos(endAngle) * radius, y + (float) Math.sin(endAngle) * radius);
        nvgArc(getContextID(), x, y, radius - thickness, endAngle, startAngle, NVG_CCW);
        nvgLineTo(getContextID(), x + (float) Math.cos(startAngle) * radius, y + (float) Math.sin(startAngle) * radius);
        nvgClosePath(getContextID());
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
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
        NVGPaint paint = nvgImagePattern(getContextID(), x, y, width, height, 0, imageId, 1, NVGPaint.create());
        nvgBeginPath(getContextID());
        nvgRect(getContextID(), x, y, width, height);
        nvgFillPaint(getContextID(), paint);
        nvgFill(getContextID());
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
        int imageID = nvgCreateImage(getContextID(), imagePath.toString(), NVG_IMAGE_NEAREST);
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