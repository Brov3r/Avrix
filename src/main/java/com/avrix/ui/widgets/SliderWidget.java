package com.avrix.ui.widgets;

import com.avrix.ui.NanoDrawer;
import com.avrix.ui.NanoColor;
import org.joml.Vector2f;
import zombie.input.Mouse;

import java.util.function.Consumer;

/**
 * Represents a slider widget that allows the user to select a value by dragging a thumb along a bar.
 * The widget can be customized in terms of appearance and behavior.
 */
public class SliderWidget extends Widget {
    /**
     * The font used for text rendering.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * The maximum value of the slider.
     */
    protected int maxValue = 100;

    /**
     * The minimum value of the slider.
     */
    protected int minValue = 0;

    /**
     * The current value of the slider.
     */
    protected int value = 100;

    /**
     * The font size for rendering text.
     */
    protected int fontSize = 12;

    /**
     * The height of the slider's bar.
     */
    protected int barHeight = 10;

    /**
     * The offset for rendering text relative to the bar.
     */
    protected int textOffset = 5;

    /**
     * The radius for rounded corners of the bar and thumb.
     */
    protected int borderRadius = 5;

    /**
     * The offset between the filled bar and its border.
     */
    protected int barFillOffset = 2;

    /**
     * The width of the slider's thumb.
     */
    protected int thumbWidth = 5;

    /**
     * The previous value of the slider, used to detect changes.
     */
    private int prevValue;

    /**
     * Indicates whether the slider is enabled.
     */
    protected boolean enable = true;

    /**
     * The color of the slider's thumb and filled bar.
     */
    protected NanoColor accentColor = new NanoColor("#2ecc71");

    /**
     * The color of the slider's background bar.
     */
    protected NanoColor barColor = new NanoColor("#2c3e50");

    /**
     * A callback action to be executed when the slider value changes.
     */
    protected Consumer<Integer> onSlideAction;

    /**
     * Constructs a new SliderWidget with the specified position, width, and slide action callback.
     *
     * @param x             the x-coordinate of the widget's position
     * @param y             the y-coordinate of the widget's position
     * @param width         the width of the widget
     * @param onSlideAction a callback function to be executed when the slider value changes
     */
    public SliderWidget(int x, int y, int width, Consumer<Integer> onSlideAction) {
        super(x, y, width, 0);

        this.onSlideAction = onSlideAction;
        this.height = barHeight + fontSize + textOffset * 3;
    }

    /**
     * Sets the callback function to be executed when the slider value changes.
     *
     * @param onSlideAction a callback function to be executed when the slider value changes
     */
    public final void setOnSlideAction(Consumer<Integer> onSlideAction) {
        this.onSlideAction = onSlideAction;
    }

    /**
     * Returns the name of the font used for text rendering.
     *
     * @return the font name
     */
    public final String getFontName() {
        return fontName;
    }

    /**
     * Sets the name of the font used for text rendering.
     *
     * @param fontName the font name
     */
    public final void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Returns the maximum value of the slider.
     *
     * @return the maximum value
     */
    public final int getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the maximum value of the slider.
     *
     * @param maxValue the maximum value
     */
    public final void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Returns the minimum value of the slider.
     *
     * @return the minimum value
     */
    public final int getMinValue() {
        return minValue;
    }

    /**
     * Sets the minimum value of the slider.
     *
     * @param minValue the minimum value
     */
    public final void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the current value of the widget.
     *
     * @return the current value
     */
    public final int getValue() {
        return value;
    }

    /**
     * Sets the value of the widget.
     *
     * @param value the new value to be set
     */
    public final void setValue(int value) {
        this.value = value;
    }

    /**
     * Gets the font size used in the widget.
     *
     * @return the font size
     */
    public final int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size for the widget.
     *
     * @param fontSize the new font size
     */
    public final void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Gets the height of the bar in the widget.
     *
     * @return the bar height
     */
    public final int getBarHeight() {
        return barHeight;
    }

    /**
     * Sets the height of the bar in the widget.
     *
     * @param barHeight the new bar height
     */
    public final void setBarHeight(int barHeight) {
        this.barHeight = barHeight;
    }

    /**
     * Gets the text offset used in rendering the widget.
     *
     * @return the text offset
     */
    public final int getTextOffset() {
        return textOffset;
    }

    /**
     * Sets the text offset for the widget.
     *
     * @param textOffset the new text offset
     */
    public final void setTextOffset(int textOffset) {
        this.textOffset = textOffset;
    }

    /**
     * Gets the border radius of the widget.
     *
     * @return the border radius
     */
    public final int getBorderRadius() {
        return borderRadius;
    }

    /**
     * Sets the border radius for the widget.
     *
     * @param borderRadius the new border radius
     */
    public final void setBorderRadius(int borderRadius) {
        this.borderRadius = borderRadius;
    }

    /**
     * Gets the offset for the bar fill in the widget.
     *
     * @return the bar fill offset
     */
    public final int getBarFillOffset() {
        return barFillOffset;
    }

    /**
     * Sets the offset for the bar fill in the widget.
     *
     * @param barFillOffset the new bar fill offset
     */
    public final void setBarFillOffset(int barFillOffset) {
        this.barFillOffset = barFillOffset;
    }

    /**
     * Gets the width of the thumb in the widget.
     *
     * @return the thumb width
     */
    public final int getThumbWidth() {
        return thumbWidth;
    }

    /**
     * Sets the width of the thumb in the widget.
     *
     * @param thumbWidth the new thumb width
     */
    public final void setThumbWidth(int thumbWidth) {
        this.thumbWidth = thumbWidth;
    }

    /**
     * Gets the accent color used in the widget.
     *
     * @return the accent color
     */
    public final NanoColor getAccentColor() {
        return accentColor;
    }

    /**
     * Sets the accent color for the widget.
     *
     * @param accentColor the new accent color
     */
    public final void setAccentColor(NanoColor accentColor) {
        this.accentColor = accentColor;
    }

    /**
     * Gets the bar color used in the widget.
     *
     * @return the bar color
     */
    public final NanoColor getBarColor() {
        return barColor;
    }

    /**
     * Sets the bar color for the widget.
     *
     * @param barColor the new bar color
     */
    public final void setBarColor(NanoColor barColor) {
        this.barColor = barColor;
    }

    /**
     * Checks if the widget is enabled.
     *
     * @return true if the widget is enabled, false otherwise
     */
    public final boolean isEnable() {
        return enable;
    }

    /**
     * Sets the enabled state of the widget.
     *
     * @param enable true to enable the widget, false to disable it
     */
    public final void setEnable(boolean enable) {
        this.enable = enable;
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

        prevValue = value;
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        if (lmbPressed && enable) {
            int mouseX = Mouse.getXA();
            int mouseY = Mouse.getYA();
            updateValue(mouseX, mouseY);
        }

        super.update();
    }

    /**
     * Updates the slider value based on the mouse position.
     *
     * @param mouseX absolute x-coordinate of the mouse position
     * @param mouseY absolute y-coordinate of the mouse position
     */
    private void updateValue(int mouseX, int mouseY) {
        int relativeMouseX = mouseX - x;

        int barWidth = width - thumbWidth;
        int thumbX = Math.max(0, Math.min(barWidth, relativeMouseX - thumbWidth / 2));

        float normalizedValue = (float) thumbX / (barWidth - thumbWidth);

        value = Math.max(minValue, Math.min(maxValue, minValue + (int) (normalizedValue * (maxValue - minValue))));

        if (prevValue == value) return;

        if (onSlideAction != null) {
            onSlideAction.accept(value);
        }

        prevValue = value;
    }


    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        Vector2f minSize = NanoDrawer.getTextSize(String.valueOf(minValue), fontName, fontSize);
        Vector2f maxSize = NanoDrawer.getTextSize(String.valueOf(maxValue), fontName, fontSize);
        Vector2f valueSize = NanoDrawer.getTextSize(String.valueOf(value), fontName, fontSize);

        int leftOffset = (int) minSize.x + textOffset;
        int rightOffset = (int) (leftOffset + maxSize.x + textOffset);
        int thumbHeight = textOffset * 2 + barHeight;

        int barWidth = width - rightOffset;

        float valueRange = maxValue - minValue;
        float normalizedValue = (value - minValue) / valueRange;
        int thumbX = (int) (leftOffset + barWidth * normalizedValue);

        int valueFill = Math.max(0, Math.min(barWidth - barFillOffset * 2, (int) (barWidth * normalizedValue) - barFillOffset * 2 + thumbWidth));
        thumbX = Math.max(leftOffset, Math.min(leftOffset + barWidth - thumbWidth, thumbX));

        if (borderRadius != 0) {
            drawRoundedRect(leftOffset, textOffset, barWidth, barHeight, borderRadius, barColor);
            drawRoundedRect(leftOffset + barFillOffset, textOffset + barFillOffset, valueFill, barHeight - barFillOffset * 2, borderRadius - barFillOffset, accentColor);

            drawRoundedRect(thumbX, 0, thumbWidth, thumbHeight, (float) thumbWidth / 2, accentColor);
        } else {
            drawRect(leftOffset, textOffset, barWidth, barHeight, barColor);
            drawRect(leftOffset + barFillOffset, textOffset + barFillOffset, valueFill, barHeight - barFillOffset * 2, accentColor);

            drawRect(thumbX, 0, thumbWidth, thumbHeight, accentColor);
        }

        drawText(String.valueOf(minValue), fontName, 0, textOffset / 2, fontSize, accentColor);
        drawText(String.valueOf(maxValue), fontName, (int) (width - maxSize.x), textOffset / 2, fontSize, accentColor);

        int valueX = thumbX + (thumbWidth - (int) valueSize.x) / 2;
        drawText(String.valueOf(value), fontName, valueX, thumbHeight, fontSize, accentColor);
    }
}