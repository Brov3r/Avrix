package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGDrawer;
import org.joml.Vector2f;

/**
 * A {@link Widget} for displaying text labels with customizable font and color.
 */
public class LabelWidget extends Widget {
    /**
     * The color of the text.
     */
    protected NVGColor textColor;

    /**
     * The text to display.
     */
    protected String text;

    /**
     * The name of the font to use.
     */
    protected String fontName;

    /**
     * The size of the font.
     */
    protected int fontSize;

    /**
     * Constructs a new {@link LabelWidget} with the specified text, font, position, size, and color.
     *
     * @param text      the text to display
     * @param fontName  the name of the font to use
     * @param x         the x-coordinate of the {@link Widget}'s position
     * @param y         the y-coordinate of the {@link Widget}'s position
     * @param fontSize  the size of the font
     * @param textColor the color of the text
     */
    public LabelWidget(String text, String fontName, int x, int y, int fontSize, NVGColor textColor) {
        super(x, y, 0, 0);

        this.textColor = textColor;
        this.text = text;
        this.fontName = fontName;
        this.fontSize = fontSize;
    }

    /**
     * Gets the font size of the text.
     *
     * @return the font size
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size of the text.
     *
     * @param fontSize the new font size
     */
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Gets the color of the text.
     *
     * @return the text color
     */
    public final NVGColor getTextColor() {
        return this.textColor;
    }

    /**
     * Sets the color of the text.
     *
     * @param textColor the new text color
     */
    public final void setTextColor(NVGColor textColor) {
        this.textColor = textColor;
    }

    /**
     * Gets the text displayed by the widget.
     *
     * @return the text
     */
    public final String getText() {
        return this.text;
    }

    /**
     * Sets the text to be displayed by the {@link Widget}.
     *
     * @param text the new text
     */
    public final void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the name of the font used by the {@link Widget}.
     *
     * @return the font name
     */
    public final String getFontName() {
        return fontName;
    }

    /**
     * Sets the name of the font to be used by the {@link Widget}.
     *
     * @param fontName the new font name
     */
    public final void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();

        Vector2f textSize = NVGDrawer.getTextSize(this.text, this.fontName, this.fontSize);

        this.width = (int) textSize.x;
        this.height = (int) textSize.y;
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        drawText(getText(), getFontName(), 0, 0, getFontSize(), getTextColor());
    }
}