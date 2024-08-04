package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGDrawer;
import org.joml.Vector2f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A customizable {@link ComboBoxWidget}.
 * This widget allows users to select a value from a dropdown list.
 */
public class ComboBoxWidget extends PanelWidget {
    /**
     * The name of the font used for the icons in the dropdown list.
     */
    protected String iconFontName = "FontAwesome";

    /**
     * The name of the font used for the text in the dropdown list.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * The Unicode character for the drop icon.
     */
    protected String dropIcon = "\uf078";

    /**
     * The width of the dropdown button in pixels.
     */
    protected int dropButtonWidth = 32;

    /**
     * The size of the icon in pixels.
     */
    protected int iconSize = 16;

    /**
     * The offset of the border in pixels.
     */
    protected int borderOffset = 5;

    /**
     * The index of the currently selected value in the dropdown list.
     */
    protected int valueIndex = 0;

    /**
     * The height of the popup menu in pixels.
     */
    protected int popupHeight = 100;

    /**
     * The height of each button in the popup menu in pixels.
     */
    protected int popupButtonHeight = 32;

    /**
     * The size of the font in pixels.
     */
    protected int fontSize;

    /**
     * Indicates whether the dropdown button is currently pressed.
     */
    protected boolean pressed = false;

    /**
     * The list of values to be displayed in the dropdown menu.
     * Uses a thread-safe list implementation.
     */
    protected List<String> valueList = new CopyOnWriteArrayList<>();

    /**
     * The color of the icons in the dropdown button.
     */
    protected NVGColor iconColor = NVGColor.WHITE;

    /**
     * The color of the text in the dropdown button.
     */
    protected NVGColor textColor = NVGColor.WHITE;

    /**
     * The accent color used for highlighting in the dropdown button.
     */
    protected NVGColor accentColor = new NVGColor("#2ecc71");

    /**
     * Constructs a new {@link Widget} with the specified position and size.
     *
     * @param x      the x-coordinate of the {@link Widget}'s position
     * @param y      the y-coordinate of the {@link Widget}'s position
     * @param width  the width of the {@link Widget}
     * @param height the height of the {@link Widget}
     */
    public ComboBoxWidget(int x, int y, int width, int height) {
        super(x, y, width, height);

        this.fontSize = height / 2;
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

        if (pressed) {
            openPopup();
        }

        pressed = false;
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

        pressed = false;
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

        pressed = true;
    }

    /**
     * Opens the popup to display the list of values.
     */
    private void openPopup() {
        PopupWidget popup = new PopupWidget(absoluteX, absoluteY + height, width, popupHeight);
        for (int i = 0; i < valueList.size(); i++) {
            int index = i;
            String value = valueList.get(i);
            ComboPopupButton valueButton = new ComboPopupButton(value, borderOffset, borderOffset + i * popupButtonHeight, popup.width - borderOffset * 2, popupButtonHeight, 0, NVGColor.LIGHT_BLACK, () -> {
                valueIndex = index;
                popup.close();
            });
            if (i == valueIndex) {
                valueButton.setSelected(true);
            }
            popup.addChild(valueButton);
        }
        popup.show();
    }

    /**
     * Returns the current value of the ComboBox.
     *
     * @return the currently selected value
     */
    public String getValue() {
        return valueList.get(valueIndex);
    }

    /**
     * Returns the value at the specified index.
     *
     * @param index the index of the value to return
     * @return the value at the specified index
     */
    public String getValue(int index) {
        return valueList.get(index);
    }

    /**
     * Adds a new value to the ComboBox.
     *
     * @param value the value to add
     */
    public void addValue(String value) {
        valueList.add(value);
    }

    /**
     * Remove a value from a ComboBox.
     *
     * @param value the value to remove
     */
    public void removeValue(String value) {
        valueList.remove(value);
    }

    /**
     * Remove a value from a ComboBox.
     *
     * @param index index of the value to be deleted
     */
    public void removeValue(int index) {
        valueList.remove(index);
    }

    /**
     * Gets the name of the icon font.
     *
     * @return the name of the icon font
     */
    public String getIconFontName() {
        return iconFontName;
    }

    /**
     * Sets the name of the icon font.
     *
     * @param iconFontName the new name of the icon font
     */
    public void setIconFontName(String iconFontName) {
        this.iconFontName = iconFontName;
    }

    /**
     * Gets the name of the font.
     *
     * @return the name of the font
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * Sets the name of the font.
     *
     * @param fontName the new name of the font
     */
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Gets the drop icon.
     *
     * @return the drop icon
     */
    public String getDropIcon() {
        return dropIcon;
    }

    /**
     * Sets the drop icon.
     *
     * @param dropIcon the new drop icon
     */
    public void setDropIcon(String dropIcon) {
        this.dropIcon = dropIcon;
    }

    /**
     * Gets the width of the drop button.
     *
     * @return the width of the drop button
     */
    public int getDropButtonWidth() {
        return dropButtonWidth;
    }

    /**
     * Sets the width of the drop button.
     *
     * @param dropButtonWidth the new width of the drop button
     */
    public void setDropButtonWidth(int dropButtonWidth) {
        this.dropButtonWidth = dropButtonWidth;
    }

    /**
     * Gets the value index.
     *
     * @return the value index
     */
    public int getValueIndex() {
        return valueIndex;
    }

    /**
     * Sets the value index.
     *
     * @param valueIndex the new value index
     */
    public void setValueIndex(int valueIndex) {
        this.valueIndex = valueIndex;
    }

    /**
     * Gets the popup height.
     *
     * @return the popup height
     */
    public int getPopupHeight() {
        return popupHeight;
    }

    /**
     * Sets the popup height.
     *
     * @param popupHeight the new popup height
     */
    public void setPopupHeight(int popupHeight) {
        this.popupHeight = popupHeight;
    }

    /**
     * Gets the popup button height.
     *
     * @return the popup button height
     */
    public int getPopupButtonHeight() {
        return popupButtonHeight;
    }

    /**
     * Sets the popup button height.
     *
     * @param popupButtonHeight the new popup button height
     */
    public void setPopupButtonHeight(int popupButtonHeight) {
        this.popupButtonHeight = popupButtonHeight;
    }

    /**
     * Gets the font size.
     *
     * @return the font size
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size.
     *
     * @param fontSize the new font size
     */
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Gets the value list.
     *
     * @return the value list
     */
    public List<String> getValueList() {
        return valueList;
    }

    /**
     * Sets the value list.
     *
     * @param valueList the new value list
     */
    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    /**
     * Gets the icon color.
     *
     * @return the icon color
     */
    public NVGColor getIconColor() {
        return iconColor;
    }

    /**
     * Sets the icon color.
     *
     * @param iconColor the new icon color
     */
    public void setIconColor(NVGColor iconColor) {
        this.iconColor = iconColor;
    }

    /**
     * Gets the text color.
     *
     * @return the text color
     */
    public NVGColor getTextColor() {
        return textColor;
    }

    /**
     * Sets the text color.
     *
     * @param textColor the new text color
     */
    public void setTextColor(NVGColor textColor) {
        this.textColor = textColor;
    }

    /**
     * Gets the accent color.
     *
     * @return the accent color
     */
    public NVGColor getAccentColor() {
        return accentColor;
    }

    /**
     * Sets the accent color.
     *
     * @param accentColor the new accent color
     */
    public void setAccentColor(NVGColor accentColor) {
        this.accentColor = accentColor;
    }

    /**
     * Gets the drop icon size.
     *
     * @return the icon size
     */
    public int getIconSize() {
        return iconSize;
    }

    /**
     * Sets the drop icon size.
     *
     * @param iconSize the new icon size
     */
    public void setIconSize(int iconSize) {
        this.iconSize = iconSize;
    }

    /**
     * Gets the border offset.
     *
     * @return the border offset
     */
    public int getBorderOffset() {
        return borderOffset;
    }

    /**
     * Sets the border offset.
     *
     * @param borderOffset the new border offset
     */
    public void setBorderOffset(int borderOffset) {
        this.borderOffset = borderOffset;
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        NVGColor newTextColor = textColor;
        NVGColor newIconColor = iconColor;

        NVGDrawer.saveRenderState();
        NVGDrawer.intersectScissor(absoluteX + borderOffset, absoluteY, width - dropButtonWidth - borderOffset * 2, height);

        if (hovered) {
            newTextColor = accentColor;
            newIconColor = accentColor;

            if (pressed) {
                newTextColor = accentColor.multiply(0.85f);
                newIconColor = accentColor.multiply(0.85f);
            }
        }

        super.render();

        if (!valueList.isEmpty() && valueList.get(valueIndex) != null) {
            String value = valueList.get(valueIndex);
            Vector2f textSize = NVGDrawer.getTextSize(value, fontName, fontSize);
            drawText(value, fontName, borderOffset, (int) ((height - textSize.y) / 2 - fontSize / 4), fontSize, newTextColor);
        }

        NVGDrawer.restoreRenderState();

        Vector2f iconTextSize = NVGDrawer.getTextSize(dropIcon, iconFontName, iconSize);

        drawLine(width - dropButtonWidth, borderOffset, width - dropButtonWidth, height - borderOffset, 2, iconColor);
        drawText(dropIcon, iconFontName, (int) (width - (dropButtonWidth + iconTextSize.x) / 2), (int) ((height - iconTextSize.y) / 2) - iconSize / 4, iconSize, newIconColor);
    }

    /**
     * The ComboPopupButton class represents a button within the dropdown popup menu of a ComboBoxWidget.
     * It extends the ButtonWidget class and includes additional state information such as whether the button is selected.
     */
    protected class ComboPopupButton extends ButtonWidget {
        /**
         * Indicates whether this popup button is currently selected.
         */
        private boolean selected = false;

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
         * @param backgroundColor the background color of the widget, specified in {@link NVGColor}
         * @param onClickMethod   method that is called when the button is clicked
         */
        public ComboPopupButton(String text, int x, int y, int width, int height, int borderRadius, NVGColor backgroundColor, Runnable onClickMethod) {
            super(text, x, y, width, height, borderRadius, backgroundColor, onClickMethod);
            setDrawBorder(false);
        }

        /**
         * Checks if the popup button is currently selected.
         *
         * @return true if the button is selected, false otherwise
         */
        public final boolean isSelected() {
            return selected;
        }

        /**
         * Sets the selected state of the popup button.
         *
         * @param selected the new selected state of the button
         */
        public final void setSelected(boolean selected) {
            this.selected = selected;
        }

        /**
         * Renders the {@link Widget}
         */
        @Override
        public void render() {
            super.render();

            NVGColor color = textColor;

            if (isEnable()) {
                if (isHovered() || selected) color = accentColor;
                if (LMBDown) color = accentColor.multiply(0.75f);
            } else {
                color = textColor.multiply(0.5f);
            }

            Vector2f titleSize = NVGDrawer.getTextSize(text, fontName, fontSize);
            drawText(text, fontName, (getWidth() - (int) titleSize.x) / 2, (getHeight() - (int) titleSize.y - fontSize / 4) / 2, fontSize, color);
        }
    }
}