package com.avrix.ui;

import org.joml.Vector2f;
import org.lwjgl.nanovg.NVGPaint;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * A set of tools for rendering NanoVG elements
 */
public class NVGDrawer {
    /**
     * Obtaining NanoVG context ID
     *
     * @return context identifier
     */
    private static long getContextID() {
        return WidgetManager.getContext().get();
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
    public static void scissor(int x, int y, int width, int height) {
        if (WidgetManager.getContext() == null) return;

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
    public static void intersectScissor(int x, int y, int width, int height) {
        if (WidgetManager.getContext() == null) return;

        nvgIntersectScissor(getContextID(), x, y, width, height);
    }

    /**
     * Resets and disables scissoring.
     */
    public static void resetScissor() {
        if (WidgetManager.getContext() == null) return;

        nvgResetScissor(getContextID());
    }

    /**
     * Pushes and saves the current render state into a state stack. A matching {@link #restoreRenderState()} must be used to restore the state.
     */
    public static void saveRenderState() {
        if (WidgetManager.getContext() == null) return;

        nvgSave(getContextID());
    }

    /**
     * Pops and restores current render state.
     */
    public static void restoreRenderState() {
        if (WidgetManager.getContext() == null) return;

        nvgRestore(getContextID());
    }

    /**
     * Resets current render state to default values. Does not affect the render state stack.
     */
    public static void resetRenderState() {
        if (WidgetManager.getContext() == null) return;

        nvgReset(getContextID());
    }

    /**
     * Draws a rectangle with a stroke and no fill
     *
     * @param x         absolute X coordinate of the top left corner of the rectangle
     * @param y         absolute Y coordinate of the top left corner of the rectangle
     * @param width     the width of the rectangle
     * @param height    height of the rectangle
     * @param lineWidth the width of the stroke line in pixels
     * @param color     the color of the outline
     */
    public static void drawRectOutline(int x, int y, int width, int height, float lineWidth, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

        nvgBeginPath(getContextID());
        nvgRect(getContextID(), x, y, width, height);
        color.tallocNVG(nvgColor -> nvgStrokeColor(getContextID(), nvgColor));
        nvgStrokeWidth(getContextID(), lineWidth);
        nvgStroke(getContextID());
    }

    /**
     * Draws a rectangle with rounded corners and a stroke without fill.
     *
     * @param x         absolute X coordinate of the top left corner of the rectangle
     * @param y         absolute Y coordinate of the top left corner of the rectangle
     * @param width     the width of the rectangle
     * @param height    height of the rectangle
     * @param radius    corner radius
     * @param lineWidth the width of the stroke line in pixels
     * @param color     the color of the outline
     */
    public static void drawRoundedRectOutline(int x, int y, int width, int height, int radius, float lineWidth, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

        nvgBeginPath(getContextID());
        nvgRoundedRect(getContextID(), x, y, width, height, radius);
        color.tallocNVG(nvgColor -> nvgStrokeColor(getContextID(), nvgColor));
        nvgStrokeWidth(getContextID(), lineWidth);
        nvgStroke(getContextID());
    }

    /**
     * Draws a filled rectangle with the specified position, size, and color.
     *
     * @param x      absolute x-coordinate of the top-left corner of the rectangle
     * @param y      absolute y-coordinate of the top-left corner of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param color  the color to fill the rectangle with
     */
    public static void drawRect(int x, int y, int width, int height, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

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
     * @param x        absolute x-coordinate of the text's position
     * @param y        absolute y-coordinate of the text's position
     * @param fontSize the size of the font
     * @param color    the color of the text
     */
    public static void drawText(String text, String fontName, int x, int y, int fontSize, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

        Vector2f textSize = getTextSize(text, fontName, fontSize);

        int horizontalOffset = 1;

        nvgFontFace(getContextID(), fontName);
        nvgFontSize(getContextID(), fontSize);
        nvgBeginPath(getContextID());
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgText(getContextID(), x + horizontalOffset, y + textSize.y, text);
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
    public static Vector2f getTextSize(String text, String fontName, int fontSize) {
        if (WidgetManager.getContext() == null) return new Vector2f(0, 0);

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
     * @param x      absolute x-coordinate of the top-left corner of the rectangle
     * @param y      absolute y-coordinate of the top-left corner of the rectangle
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param radius the radius of the corners
     * @param color  the color to fill the rectangle with
     */
    public static void drawRoundedRect(int x, int y, int width, int height, float radius, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

        nvgBeginPath(getContextID());
        nvgRoundedRect(getContextID(), x, y, width, height, radius);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
    }

    /**
     * Draws an ellipse at the specified position with the given size and color.
     *
     * @param x      absolute x-coordinate of the ellipse's center
     * @param y      absolute y-coordinate of the ellipse's center
     * @param width  the width of the ellipse
     * @param height the height of the ellipse
     * @param color  the color of the ellipse
     */
    public static void drawEllipse(int x, int y, int width, int height, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

        nvgBeginPath(getContextID());
        nvgEllipse(getContextID(), x, y, (float) width / 2, (float) height / 2);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
    }

    /**
     * Draws a line from (x1, y1) to (x2, y2) with the specified color and thickness.
     *
     * @param x1    absolute x-coordinate of the start point of the line
     * @param y1    absolute y-coordinate of the start point of the line
     * @param x2    absolute x-coordinate of the end point of the line
     * @param y2    absolute y-coordinate of the end point of the line
     * @param width the thickness of the line
     * @param color the color of the line
     */
    public static void drawLine(int x1, int y1, int x2, int y2, float width, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

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
     * @param x      absolute x-coordinate of the circle's center
     * @param y      absolute y-coordinate of the circle's center
     * @param radius the radius of the circle
     * @param color  the color of the circle
     */
    public static void drawCircle(int x, int y, float radius, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

        nvgBeginPath(getContextID());
        nvgCircle(getContextID(), x, y, radius);
        color.tallocNVG(nvgColor -> nvgFillColor(getContextID(), nvgColor));
        nvgFill(getContextID());
    }

    /**
     * Draws an arc segment with the specified center, radius, start angle, and end angle.
     *
     * @param x          absolute x-coordinate of the center of the arc
     * @param y          absolute y-coordinate of the center of the arc
     * @param radius     the radius of the arc
     * @param startAngle the starting angle of the arc (in radians)
     * @param endAngle   the ending angle of the arc (in radians)
     * @param color      the color of the arc segment
     */
    public static void drawArc(int x, int y, float radius, float startAngle, float endAngle, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

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
     * @param x          absolute x-coordinate of the center of the arc
     * @param y          absolute y-coordinate of the center of the arc
     * @param radius     the radius of the arc
     * @param startAngle the starting angle of the arc (in radians)
     * @param endAngle   the ending angle of the arc (in radians)
     * @param thickness  the thickness of the arc segment
     * @param color      the color of the arc segment
     */
    public static void drawArc(int x, int y, float radius, float thickness, float startAngle, float endAngle, NVGColor color) {
        if (WidgetManager.getContext() == null) return;

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
     * @param x       absolute x-coordinate of the image's position
     * @param y       absolute y-coordinate of the image's position
     * @param width   the width of the image
     * @param height  the height of the image
     * @param opacity image opacity (from 0 to 1)
     */
    public static void drawImage(int imageId, int x, int y, int width, int height, float opacity) {
        if (WidgetManager.getContext() == null) return;

        if (imageId == -1) {
            drawRect(x, y, width / 2, height / 2, NVGColor.VIOLET);
            drawRect(x + width / 2, y, width / 2, height / 2, NVGColor.BLACK);
            drawRect(x, y + height / 2, width / 2, height / 2, NVGColor.BLACK);
            drawRect(x + width / 2, y + height / 2, width / 2, height / 2, NVGColor.VIOLET);
            return;
        }

        NVGPaint paint = nvgImagePattern(getContextID(), x, y, width, height, 0, imageId, opacity, NVGPaint.create());
        nvgBeginPath(getContextID());
        nvgRect(getContextID(), x, y, width, height);
        nvgFillPaint(getContextID(), paint);
        nvgFill(getContextID());
    }
}