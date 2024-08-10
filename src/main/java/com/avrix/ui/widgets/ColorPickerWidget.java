package com.avrix.ui.widgets;

import com.avrix.ui.NanoColor;
import com.avrix.utils.WindowUtils;
import zombie.input.Mouse;

import java.util.function.Consumer;

/**
 * A {@link ColorPickerWidget} that allows the user to pick and adjust colors using sliders for red, green, blue, and alpha values.
 * This widget displays a popup for color selection and provides an input field for hexadecimal color values.
 */
public class ColorPickerWidget extends Widget {
    /**
     * The font size used for displaying text.
     */
    protected int fontSize = 14;

    /**
     * The offset used for the border of the widget.
     */
    protected int borderOffset = 2;

    /**
     * The name of the font used for displaying text.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * Action when color changes
     */
    protected Consumer<NanoColor> onChangeColorAction;

    /**
     * The color of the text displayed on the widget.
     */
    protected NanoColor textColor = NanoColor.WHITE;

    /**
     * The current color value being picked or adjusted.
     */
    protected NanoColor value;

    /**
     * Constructs a new {@link ColorPickerWidget} with the specified position and size.
     *
     * @param x      the x-coordinate of the {@link Widget}'s position
     * @param y      the y-coordinate of the {@link Widget}'s position
     * @param width  the width of the {@link Widget}
     * @param height the height of the {@link Widget}
     * @param value  the initial {@link NanoColor} value to be displayed and adjusted
     */
    public ColorPickerWidget(int x, int y, int width, int height, NanoColor value) {
        super(x, y, width, height);
        this.value = value;
    }

    /**
     * Called when the left mouse button is released over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    @Override
    public void onLeftMouseUp(int x, int y) {
        if (lmbPressed) {
            openPopup();
        }

        super.onLeftMouseUp(x, y);
    }

    /**
     * Returns the font size used for displaying text.
     *
     * @return the font size
     */
    public final int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size used for displaying text.
     *
     * @param fontSize the new font size
     */
    public final void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Returns the name of the font used for displaying text.
     *
     * @return the font name
     */
    public final String getFontName() {
        return fontName;
    }

    /**
     * Sets the name of the font used for displaying text.
     *
     * @param fontName the new font name
     */
    public final void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Returns the border offset of the widget.
     *
     * @return the border offset
     */
    public final int getBorderOffset() {
        return borderOffset;
    }

    /**
     * Sets the border offset of the widget.
     *
     * @param borderOffset the new border offset
     */
    public final void setBorderOffset(int borderOffset) {
        this.borderOffset = borderOffset;
    }

    /**
     * Returns the text color used for displaying text on the widget.
     *
     * @return the text color
     */
    public final NanoColor getTextColor() {
        return textColor;
    }

    /**
     * Sets the text color used for displaying text on the widget.
     *
     * @param textColor the new text color
     */
    public final void setTextColor(NanoColor textColor) {
        this.textColor = textColor;
    }

    /**
     * Returns the current {@link NanoColor} value being picked or adjusted.
     *
     * @return the current color value
     */
    public final NanoColor getValue() {
        return value;
    }

    /**
     * Sets the current {@link NanoColor} value to be displayed and adjusted.
     *
     * @param value the new color value
     */
    public final void setValue(NanoColor value) {
        this.value = value;
    }

    /**
     * Returns the action to be performed when the color value changes.
     *
     * @return a {@link Consumer} of {@link NanoColor} representing the action to be performed on color change
     */
    public final Consumer<NanoColor> getOnChangeColorAction() {
        return onChangeColorAction;
    }

    /**
     * Sets the action to be performed when the color value changes.
     *
     * @param onChangeColorAction a {@link Consumer} of {@link NanoColor} to be executed on color change
     */
    public final void setOnChangeColorAction(Consumer<NanoColor> onChangeColorAction) {
        this.onChangeColorAction = onChangeColorAction;
    }

    /**
     * Opens a popup window for color selection and adjustment.
     * The popup includes sliders for red, green, blue, and alpha values, a preview panel, and a hexadecimal input field.
     */
    protected void openPopup() {
        int popupWidth = 200;
        int popupHeight = 330;
        int margin = 10;
        int previewHeight = 30;

        int vboxWidth = popupWidth - margin * 2;
        int vboxHeight = popupHeight - margin * 2;

        int popupX = Mouse.getXA();
        int popupY = Mouse.getYA();

        if (popupX + popupWidth > WindowUtils.getWindowWidth()) {
            popupX = WindowUtils.getWindowWidth() - popupWidth;
        }

        if (popupY + popupHeight > WindowUtils.getWindowHeight()) {
            popupY = WindowUtils.getWindowHeight() - popupHeight;
        }

        PopupWidget popup = new PopupWidget(popupX, popupY, popupWidth, popupHeight);
        VerticalBoxWidget vbox = new VerticalBoxWidget(margin, margin, vboxWidth, vboxHeight, true);

        PanelWidget previewColor = new PanelWidget(0, 0, vboxWidth, previewHeight);
        previewColor.setBackgroundColor(value);
        vbox.addChild(previewColor);

        InputTextWidget hexInput = new InputTextWidget(0, 0, vboxWidth, 30);

        SliderWidget redSlider = new SliderWidget(0, 0, vboxWidth, (red) -> updateColor(value.red((float) red / 255), previewColor, hexInput));
        redSlider.setMaxValue(255);
        redSlider.setValue((int) (value.getRed() * 255));

        SliderWidget greenSlider = new SliderWidget(0, 0, vboxWidth, (green) -> updateColor(value.green((float) green / 255), previewColor, hexInput));
        greenSlider.setMaxValue(255);
        greenSlider.setValue((int) (value.getGreen() * 255));

        SliderWidget blueSlider = new SliderWidget(0, 0, vboxWidth, (blue) -> updateColor(value.blue((float) blue / 255), previewColor, hexInput));
        blueSlider.setMaxValue(255);
        blueSlider.setValue((int) (value.getBlue() * 255));

        SliderWidget alphaSlider = new SliderWidget(0, 0, vboxWidth, (alpha) -> updateColor(value.alpha((float) alpha / 255), previewColor, hexInput));
        alphaSlider.setMaxValue(255);
        alphaSlider.setValue((int) (value.getAlpha() * 255));

        vbox.addChild(new LabelWidget("Red:", fontName, 0, 0, 50, fontSize, textColor));
        vbox.addChild(redSlider);
        vbox.addChild(new LabelWidget("Green:", fontName, 0, 0, 50, fontSize, textColor));
        vbox.addChild(greenSlider);
        vbox.addChild(new LabelWidget("Blue:", fontName, 0, 0, 50, fontSize, textColor));
        vbox.addChild(blueSlider);
        vbox.addChild(new LabelWidget("Alpha:", fontName, 0, 0, 50, fontSize, textColor));
        vbox.addChild(alphaSlider);

        hexInput.setValue(value.toHEX());
        hexInput.setOnTextChangeAction((text) -> {
            if (!text.matches("^#[0-9A-Fa-f]{6}$")) return;

            updateColor(new NanoColor(text), previewColor, hexInput);

            redSlider.setValue((int) (value.getRed() * 255));
            greenSlider.setValue((int) (value.getGreen() * 255));
            blueSlider.setValue((int) (value.getBlue() * 255));
            alphaSlider.setValue((int) (value.getAlpha() * 255));
            previewColor.setBackgroundColor(value);

        });
        vbox.addChild(hexInput);

        popup.addChild(vbox);
        popup.show();
    }

    /**
     * Updates the color value, refreshes the preview, and updates the HEX input field.
     * Optionally triggers the color change action if it is set.
     *
     * @param newColor     the new {@link NanoColor} to set
     * @param previewColor the {@link PanelWidget} used to preview the color
     * @param hexInput     the {@link InputTextWidget} used to display the color in HEX format
     */
    protected void updateColor(NanoColor newColor, PanelWidget previewColor, InputTextWidget hexInput) {
        value = newColor;
        previewColor.setBackgroundColor(value);
        hexInput.setValue(value.toHEX());

        if (onChangeColorAction != null) onChangeColorAction.accept(newColor);
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        drawRect(0, 0, width, height, NanoColor.WHITE);
        drawRect(borderOffset, borderOffset, width - borderOffset * 2, height - borderOffset * 2, value);
        drawRectOutline(0, 0, width, height, 2, NanoColor.BLACK);
    }
}