package com.avrix.ui.widgets;

import com.avrix.ui.NanoDrawer;
import com.avrix.ui.NanoColor;
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
    protected NanoColor textColor = NanoColor.WHITE;

    /**
     * The method to be executed when the button is clicked.
     */
    protected Runnable onClickMethod;

    /**
     * Indicates whether the button is enabled.
     */
    protected boolean enable = true;

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
     * @param backgroundColor the background color of the widget, specified in {@link NanoColor}
     * @param onClickMethod   method that is called when the button is clicked
     */
    public ButtonWidget(String text, int x, int y, int width, int height, int borderRadius, NanoColor backgroundColor, Runnable onClickMethod) {
        super(x, y, width, height, borderRadius, backgroundColor);

        this.text = text;
        this.onClickMethod = onClickMethod;
    }

    /**
     * Returns the text color of the {@link ButtonWidget}.
     *
     * @return the text color
     */
    public final NanoColor getTextColor() {
        return textColor;
    }

    /**
     * Sets the text color of the {@link ButtonWidget}.
     *
     * @param textColor the text color to set
     */
    public final void setTextColor(NanoColor textColor) {
        this.textColor = textColor;
    }

    /**
     * Returns the text of the {@link ButtonWidget}.
     *
     * @return the text
     */
    public final String getText() {
        return text;
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
        return fontSize;
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
     * Called when the left mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    @Override
    public void onLeftMouseUp(int x, int y) {
        if (enable && visible && lmbPressed) {
            onClickMethod.run();
            SoundManager.instance.playUISound(clickSound);
        }

        super.onLeftMouseUp(x, y);
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        NanoColor bgColor = backgroundColor.copy();

        if (isEnable()) {
            if (isHovered()) bgColor = bgColor.multiply(1.25f);
            if (lmbPressed) bgColor = bgColor.multiply(0.75f);
        } else {
            bgColor = bgColor.multiply(0.3f);
        }

        if (borderRadius != 0) {
            drawRoundedRect(0, 0, getWidth(), getHeight(), borderRadius, bgColor);
            if (drawBorder) {
                drawRoundedRectOutline(0, 0, getWidth(), getHeight(), borderRadius, borderWidth, borderColor);
            }
        } else {
            drawRect(0, 0, getWidth(), getHeight(), bgColor);
            if (drawBorder) {
                drawRectOutline(0, 0, getWidth(), getHeight(), borderWidth, borderColor);
            }
        }

        Vector2f titleSize = NanoDrawer.getTextSize(text, fontName, fontSize);
        drawText(text, fontName, (getWidth() - (int) titleSize.x) / 2, (getHeight() - (int) titleSize.y - fontSize / 4) / 2, fontSize, textColor);
    }
}