package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGDrawer;
import org.joml.Vector2f;

import java.util.function.Consumer;

/**
 * A {@link Widget} that represents a checkbox with a title and an icon.
 * The checkbox can be checked or unchecked, and the title text can be truncated with an ellipsis if it exceeds the maximum width.
 */
public class CheckboxWidget extends Widget {
    /**
     * The title text displayed next to the checkbox.
     */
    protected String titleText;

    /**
     * The name of the font used for the title text.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * The name of the font used for the checkbox icon.
     */
    protected String iconFontName = "FontAwesome";

    /**
     * The icon used to represent a checked state.
     */
    protected String checkIcon = "\uf00c";

    /**
     * The size of the checkbox.
     */
    protected int checkBoxSize = 16;

    /**
     * The font size used for the title text.
     */
    protected int fontSize = 16;

    /**
     * The offset between the checkbox and the title text.
     */
    protected int titleOffset = 5;

    /**
     * The maximum width available for the title text before it gets truncated.
     */
    protected int maxTitleWidth;

    /**
     * The action to perform when the checkbox is checked or unchecked.
     */
    protected Consumer<Boolean> onCheckAction;

    /**
     * Indicates whether the checkbox is currently checked.
     */
    protected boolean checked = false;

    /**
     * Indicates whether the checkbox is enabled.
     */
    protected boolean enable = true;

    /**
     * The color of the title text.
     */
    protected NVGColor titleColor = NVGColor.WHITE;

    /**
     * The color of the checkbox icon when checked.
     */
    protected NVGColor checkColor = NVGColor.GREEN;

    /**
     * Constructs a new {@link Widget} with the specified position and size.
     *
     * @param title         the title text for the {@link Widget}
     * @param x             the x-coordinate of the {@link Widget}'s position
     * @param y             the y-coordinate of the {@link Widget}'s position
     * @param width         the width of the {@link Widget}
     * @param height        the height of the {@link Widget}
     * @param onCheckAction the action to perform when the checkbox is checked or unchecked
     */
    public CheckboxWidget(String title, int x, int y, int width, int height, Consumer<Boolean> onCheckAction) {
        super(x, y, width, height);
        this.titleText = title;
        this.onCheckAction = onCheckAction;

        this.width = Math.max(width, checkBoxSize);
        this.height = Math.max(height, Math.max(checkBoxSize, fontSize));
        this.maxTitleWidth = this.width - checkBoxSize - titleOffset; // Set the max width for title text
    }

    /**
     * Handles the left mouse button release event to toggle the checkbox state.
     *
     * @param x the x-coordinate of the mouse event
     * @param y the y-coordinate of the mouse event
     */
    @Override
    public void onLeftMouseUp(int x, int y) {
        super.onLeftMouseUp(x, y);

        if (x > 0 && x < width && y > 0 && y < height && enable) {
            checked = !checked;
            onCheckAction.accept(checked);
        }
    }

    /**
     * Returns the title text for the checkbox.
     *
     * @return the title text
     */
    public final String getTitleText() {
        return titleText;
    }

    /**
     * Sets the title text for the checkbox.
     *
     * @param titleText the title text to set
     */
    public final void setTitleText(String titleText) {
        this.titleText = titleText;
    }

    /**
     * Returns the font name used for the title text.
     *
     * @return the font name
     */
    public final String getFontName() {
        return fontName;
    }

    /**
     * Sets the font name used for the title text.
     *
     * @param fontName the font name to set
     */
    public final void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Returns the icon used to represent a checked state.
     *
     * @return the check icon
     */
    public final String getCheckIcon() {
        return checkIcon;
    }

    /**
     * Sets the icon used to represent a checked state.
     *
     * @param checkIcon the check icon to set
     */
    public final void setCheckIcon(String checkIcon) {
        this.checkIcon = checkIcon;
    }

    /**
     * Returns the font name used for the checkbox icon.
     *
     * @return the icon font name
     */
    public final String getIconFontName() {
        return iconFontName;
    }

    /**
     * Sets the font name used for the checkbox icon.
     *
     * @param iconFontName the icon font name to set
     */
    public final void setIconFontName(String iconFontName) {
        this.iconFontName = iconFontName;
    }

    /**
     * Returns the size of the checkbox.
     *
     * @return the checkbox size
     */
    public final int getCheckBoxSize() {
        return checkBoxSize;
    }

    /**
     * Sets the size of the checkbox.
     *
     * @param checkBoxSize the checkbox size to set
     */
    public final void setCheckBoxSize(int checkBoxSize) {
        this.checkBoxSize = checkBoxSize;
    }

    /**
     * Returns the font size used for the title text.
     *
     * @return the font size
     */
    public final int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size used for the title text.
     *
     * @param fontSize the font size to set
     */
    public final void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Returns the offset between the checkbox and the title text.
     *
     * @return the title offset
     */
    public final int getTitleOffset() {
        return titleOffset;
    }

    /**
     * Sets the offset between the checkbox and the title text.
     *
     * @param titleOffset the title offset to set
     */
    public final void setTitleOffset(int titleOffset) {
        this.titleOffset = titleOffset;
    }

    /**
     * Returns whether the checkbox is currently checked.
     *
     * @return {@code true} if the checkbox is checked, {@code false} otherwise
     */
    public final boolean isChecked() {
        return checked;
    }

    /**
     * Sets whether the checkbox is currently checked.
     *
     * @param checked {@code true} to check the checkbox, {@code false} to uncheck it
     */
    public final void setChecked(boolean checked) {
        this.checked = checked;
    }

    /**
     * Returns whether the checkbox is enabled.
     *
     * @return {@code true} if the checkbox is enabled, {@code false} otherwise
     */
    public final boolean isEnable() {
        return enable;
    }

    /**
     * Sets whether the checkbox is enabled.
     *
     * @param enable {@code true} to enable the checkbox, {@code false} to disable it
     */
    public final void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * Returns the color of the title text.
     *
     * @return the title color
     */
    public final NVGColor getTitleColor() {
        return titleColor;
    }

    /**
     * Sets the color of the title text.
     *
     * @param titleColor the title color to set
     */
    public final void setTitleColor(NVGColor titleColor) {
        this.titleColor = titleColor;
    }

    /**
     * Returns the color of the checkbox icon when checked.
     *
     * @return the check color
     */
    public final NVGColor getCheckColor() {
        return checkColor;
    }

    /**
     * Sets the color of the checkbox icon when checked.
     *
     * @param checkColor the check color to set
     */
    public final void setCheckColor(NVGColor checkColor) {
        this.checkColor = checkColor;
    }

    /**
     * Sets the action to perform when the checkbox is checked or unchecked.
     *
     * @param onCheckAction the action to perform
     */
    public void setOnCheckAction(Consumer<Boolean> onCheckAction) {
        this.onCheckAction = onCheckAction;
    }

    /**
     * Truncates the given text with an ellipsis if it exceeds the specified width.
     *
     * @param text     the text to be truncated
     * @param maxWidth the maximum width available for the text
     * @return the truncated text with an ellipsis if it exceeds the maximum width
     */
    private String truncateText(String text, int maxWidth) {
        Vector2f textSize = NVGDrawer.getTextSize(text, fontName, fontSize);
        if (textSize.x <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        Vector2f ellipsisSize = NVGDrawer.getTextSize(ellipsis, fontName, fontSize);
        int availableWidth = maxWidth - (int) ellipsisSize.x;

        String truncatedText = text;
        while (textSize.x > availableWidth && !truncatedText.isEmpty()) {
            truncatedText = truncatedText.substring(0, truncatedText.length() - 1);
            textSize = NVGDrawer.getTextSize(truncatedText + ellipsis, fontName, fontSize);
        }

        return truncatedText + ellipsis;
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        Vector2f textSize = NVGDrawer.getTextSize(titleText, fontName, fontSize);
        drawRect(0, 0, checkBoxSize, checkBoxSize, NVGColor.BLACK);
        drawRectOutline(0, 0, checkBoxSize, checkBoxSize, 1, NVGColor.WHITE);

        // Truncate the text if it exceeds the maximum width
        String displayText = truncateText(titleText, maxTitleWidth);

        int textY = (int) ((height - textSize.y) / 2) - titleOffset / 2;
        drawText(displayText, fontName, checkBoxSize + titleOffset, textY, fontSize, titleColor);

        if (checked) {
            int checkBoxOffset = 4;
            Vector2f checkSize = NVGDrawer.getTextSize(checkIcon, iconFontName, checkBoxSize - checkBoxOffset);
            drawText(checkIcon, iconFontName, (int) (checkBoxSize - checkSize.x) / 2, (int) (checkBoxSize - checkSize.y) / 2 - titleOffset / 2, checkBoxSize - checkBoxOffset, checkColor);
        }
    }
}