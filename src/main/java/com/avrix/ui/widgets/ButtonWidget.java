package com.avrix.ui.widgets;

import com.avrix.ui.UIColor;
import org.joml.Vector2f;
import zombie.SoundManager;

/**
 * A class representing a {@link ButtonWidget}, which is a type of {@link PanelWidget}.
 */
public class ButtonWidget extends PanelWidget {
    /**
     * The font name used for the button text.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * Names of in-game click sounds
     */
    protected String clickSound = "UIActivateButton";

    /**
     * The text displayed on the button.
     */
    protected String text;

    /**
     * The font size of the button text.
     */
    protected int fontSize = 14;

    /**
     * The color of the button text.
     */
    protected UIColor textColor = UIColor.WHITE;

    /**
     * The method to be executed when the button is clicked.
     */
    protected Runnable onClickMethod;

    /**
     * Indicates whether the button is enabled.
     */
    protected boolean enable = true;

    /**
     * Indicates whether the left mouse button is currently pressed down on the button.
     */
    private boolean LMBDown = false;

    /**
     * Constructs a new {@link ButtonWidget} with the specified text, position, size, border radius,
     * and background color.
     *
     * @param text            text displayed on the button
     * @param x               the X-coordinate of the {@link Widget}'s position
     * @param y               the Y-coordinate of the {@link Widget}'s position
     * @param width           the width of the widget
     * @param height          the height of the widget
     * @param borderRadius    the radius of the corner rounding in pixels
     * @param backgroundColor the background color of the widget, specified in {@link UIColor}
     * @param onClickMethod   method that is called when the button is clicked
     */
    public ButtonWidget(String text, int x, int y, int width, int height, int borderRadius, UIColor backgroundColor, Runnable onClickMethod) {
        super(x, y, width, height, borderRadius, backgroundColor);

        this.text = text;
        this.onClickMethod = onClickMethod;
    }

    /**
     * Returns the text color of the {@link ButtonWidget}.
     *
     * @return the text color
     */
    public final UIColor getTextColor() {
        return this.textColor;
    }

    /**
     * Sets the text color of the {@link ButtonWidget}.
     *
     * @param textColor the text color to set
     */
    public final void setTextColor(UIColor textColor) {
        this.textColor = textColor;
    }

    /**
     * Returns the text of the {@link ButtonWidget}.
     *
     * @return the text
     */
    public final String getText() {
        return this.text;
    }

    /**
     * Sets the text of the {@link ButtonWidget}.
     *
     * @param text the text to set
     */
    public final void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the font size of the {@link ButtonWidget}.
     *
     * @return the font size
     */
    public final int getFontSize() {
        return this.fontSize;
    }

    /**
     * Sets the font size of the {@link ButtonWidget}.
     *
     * @param fontSize the font size to set
     */
    public final void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Returns the font name of the {@link ButtonWidget}.
     *
     * @return the font name
     */
    public final String getFontName() {
        return fontName;
    }

    /**
     * Sets the font name of the {@link ButtonWidget}.
     *
     * @param fontName the font name to set
     */
    public final void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Sets the method to be called when the {@link ButtonWidget} is clicked.
     *
     * @param onClickMethod the method to set
     */
    public final void setOnClickMethod(Runnable onClickMethod) {
        this.onClickMethod = onClickMethod;
    }

    /**
     * Returns whether the {@link ButtonWidget} is enabled.
     *
     * @return true if the button is enabled, false otherwise
     */
    public final boolean isEnable() {
        return enable;
    }

    /**
     * Sets whether the {@link ButtonWidget} is enabled.
     *
     * @param enable true to enable the button, false to disable it
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

        this.LMBDown = true;
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

        if (this.enable && this.visible && this.LMBDown) {
            this.onClickMethod.run();
            SoundManager.instance.playUISound(this.clickSound);
        }

        this.LMBDown = false;
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

        this.LMBDown = false;
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        UIColor bgColor = this.backgroundColor.copy();

        if (isEnable()) {
            if (isHovered()) bgColor = bgColor.multiply(1.25f);
            if (LMBDown) bgColor = bgColor.multiply(0.75f);
        } else {
            bgColor = bgColor.multiply(0.3f);
        }

        if (this.borderRadius != 0) {
            drawRoundedRect(0, 0, getWidth(), getHeight(), this.borderRadius, bgColor);
            if (this.drawBorder) {
                drawRoundedRectOutline(0, 0, getWidth(), getHeight(), this.borderRadius, this.borderWidth, this.borderColor);
            }
        } else {
            drawRect(0, 0, getWidth(), getHeight(), bgColor);
            if (this.drawBorder) {
                drawRectOutline(0, 0, getWidth(), getHeight(), this.borderWidth, this.borderColor);
            }
        }

        Vector2f titleSize = getTextSize(this.text, this.fontName, this.fontSize);
        drawText(this.text, this.fontName, (getWidth() - (int) titleSize.x) / 2, (getHeight() - (int) titleSize.y - this.fontSize / 4) / 2, this.fontSize, this.textColor);
    }
}