package com.avrix.api;

import zombie.core.Color;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

/**
 * A set of tools for manipulating text and rendering it
 */
public class TextUtils {
    /**
     * Drawing colored text at specified coordinates
     *
     * @param text  the text to be drawn. Must not be null
     * @param font  text font corresponding to enum UIFont
     * @param posX  horizontal text position (X-coordinate), measured in pixels
     * @param posY  vertical position of the text (Y-coordinate), measured in pixels
     * @param color color implementation object
     */
    public static void drawText(String text, UIFont font, int posX, int posY, Color color) {
        drawText(text, font, posX, posY, color.r, color.g, color.b, color.a);
    }

    /**
     * Drawing colored text at specified coordinates
     *
     * @param text the text to be drawn. Must not be null
     * @param font text font corresponding to enum UIFont
     * @param posX horizontal text position (X-coordinate), measured in pixels
     * @param posY vertical position of the text (Y-coordinate), measured in pixels
     * @param r    red component of the text color, value from 0.0 to 1.0
     * @param g    green component of the text color, value from 0.0 to 1.0
     * @param b    blue component of the text color, value from 0.0 to 1.0
     * @param a    alpha component of the text color, value from 0.0 to 1.0
     */
    public static void drawText(String text, UIFont font, int posX, int posY, float r, float g, float b, float a) {
        TextManager.instance.DrawString(font, posX, posY, text, r, g, b, a);
    }

    /**
     * Getting line height from a given font
     *
     * @param font text font corresponding to enum UIFont
     * @return line height
     */
    public static int getFontHeight(UIFont font) {
        return TextManager.instance.getFontHeight(font);
    }

    /**
     * Returns the width of the given text in the specified font
     *
     * @param text The text for the dimension
     * @param font The font used for the text
     * @return The width of the text in pixels
     */
    public static int getTextWidth(String text, UIFont font) {
        return TextManager.instance.MeasureStringX(font, text);
    }

    /**
     * Returns the height of the given text in the specified font
     *
     * @param text The text for the dimension
     * @param font The font used for the text
     * @return The height of the text in pixels
     */
    public static int getTextHeight(String text, UIFont font) {
        return TextManager.instance.MeasureStringY(font, text);
    }
}