package com.avrix.ui.widgets;

import com.avrix.ui.NanoDrawer;
import com.avrix.ui.NanoColor;
import org.joml.Vector2f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A {@link Widget} that represents a set of radio buttons.
 * The radio buttons can be arranged either horizontally or vertically.
 */
public class RadioButtonWidget extends Widget {
    /**
     * The font name used to render the text in the radio buttons.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * The font size used to render the text in the radio buttons.
     */
    protected int fontSize = 12;

    /**
     * The index of the currently selected radio button.
     */
    protected int valueIndex = -1;

    /**
     * The offset between radio buttons.
     */
    protected int offset = 5;

    /**
     * The radius of the circles representing the radio buttons.
     */
    protected int circleRadius = 8;

    /**
     * The height of each radio button.
     */
    protected int radioHeight = 20;

    /**
     * The width of each radio button.
     */
    protected int radioWidth = 100;

    /**
     * Whether the radio buttons are arranged horizontally.
     */
    protected boolean horizontal;

    /**
     * The list of {@link RadioValue} objects representing the radio button options.
     */
    protected List<RadioValue> values = new CopyOnWriteArrayList<>();

    /**
     * The action to perform when the selected radio button changes.
     */
    protected BiConsumer<Integer, String> onChangeAction;

    /**
     * The color of the text in the radio buttons.
     */
    protected NanoColor textColor = NanoColor.WHITE;

    /**
     * The color used to accent the selected radio button.
     */
    protected NanoColor accentColor = new NanoColor("#2ecc71");

    /**
     * The color of the circles representing the radio buttons.
     */
    protected NanoColor circleColor = new NanoColor("#2c3e50");

    /**
     * Constructs a {@link RadioButtonWidget} with the specified position, size, and arrangement.
     *
     * @param x              the x-coordinate of the {@link Widget}
     * @param y              the y-coordinate of the {@link Widget}
     * @param width          the width of the {@link Widget}
     * @param height         the height of the {@link Widget}
     * @param horizontal     whether the radio buttons are arranged horizontally
     * @param onChangeAction the action to perform when the selected radio button changes
     */
    public RadioButtonWidget(int x, int y, int width, int height, boolean horizontal, BiConsumer<Integer, String> onChangeAction) {
        super(x, y, width, height);
        this.horizontal = horizontal;
        this.onChangeAction = onChangeAction;
    }

    /**
     * Resizes the {@link RadioButtonWidget} to fit its content based on the arrangement.
     * Adjusts the width and height according to the number of radio buttons and their dimensions.
     */
    public void resizeToContent() {
        width = horizontal ? getChildren().size() * (radioWidth + offset) - offset : radioWidth;
        height = horizontal ? radioHeight : getChildren().size() * (radioHeight + offset) - offset;
    }

    /**
     * Adds a new radio button to the {@link RadioButtonWidget}.
     *
     * @param titleText the text to display on the new radio button
     */
    public void addRadio(String titleText) {
        int radioX = horizontal ? getChildren().size() * (radioWidth + offset) : 0;
        int radioY = horizontal ? 0 : getChildren().size() * (radioHeight + offset);

        RadioValue radioValue = new RadioValue(titleText, radioX, radioY, (value) -> {
            for (int i = 0; i < values.size(); i++) {
                RadioValue radio = values.get(i);
                boolean selected = radio == value;
                radio.selected = selected;
                if (selected) {
                    valueIndex = i;
                }
            }
            onChangeAction.accept(valueIndex, value.value);
        });

        values.add(radioValue);
        addChild(radioValue);
    }

    /**
     * Returns the list of {@link RadioValue} objects representing the radio button options.
     *
     * @return the list of {@link RadioValue} objects
     */
    public final List<RadioValue> getValues() {
        return values;
    }

    /**
     * Retrieves the text of the currently selected radio button.
     *
     * @return the text of the currently selected radio button, or {@code null} if no radio button is selected
     */
    public final String getValueText() {
        return getValueText(valueIndex);
    }

    /**
     * Retrieves the text of the radio button at the specified index.
     *
     * @param index the index of the radio button whose text is to be retrieved
     * @return the text of the radio button at the specified index, or {@code null} if the index is out of bounds
     */
    public final String getValueText(int index) {
        return values.size() <= index ? null : values.get(index).value;
    }

    /**
     * Retrieves the index of the currently selected radio button.
     *
     * @return the index of the currently selected radio button, or {@code -1} if no radio button is selected
     */
    public final int getValueIndex() {
        return valueIndex;
    }

    /**
     * Returns the font name used to render the text in the radio buttons.
     *
     * @return the font name
     */
    public final String getFontName() {
        return fontName;
    }

    /**
     * Sets the font name used to render the text in the radio buttons.
     *
     * @param fontName the font name
     */
    public final void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Returns the font size used to render the text in the radio buttons.
     *
     * @return the font size
     */
    public final int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size used to render the text in the radio buttons.
     *
     * @param fontSize the font size
     */
    public final void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Returns the offset between radio buttons.
     *
     * @return the offset
     */
    public final int getOffset() {
        return offset;
    }

    /**
     * Sets the offset between radio buttons.
     *
     * @param offset the offset
     */
    public final void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Returns the radius of the circles representing the radio buttons.
     *
     * @return the circle radius
     */
    public final int getCircleRadius() {
        return circleRadius;
    }

    /**
     * Sets the radius of the circles representing the radio buttons.
     *
     * @param circleRadius the circle radius
     */
    public final void setCircleRadius(int circleRadius) {
        this.circleRadius = circleRadius;
    }

    /**
     * Returns the height of each radio button.
     *
     * @return the radio button height
     */
    public final int getRadioHeight() {
        return radioHeight;
    }

    /**
     * Sets the height of each radio button.
     *
     * @param radioHeight the radio button height
     */
    public final void setRadioHeight(int radioHeight) {
        this.radioHeight = radioHeight;
    }

    /**
     * Returns the width of each radio button.
     *
     * @return the radio button width
     */
    public final int getRadioWidth() {
        return radioWidth;
    }

    /**
     * Sets the width of each radio button.
     *
     * @param radioWidth the radio button width
     */
    public final void setRadioWidth(int radioWidth) {
        this.radioWidth = radioWidth;
    }

    /**
     * Returns the color of the text in the radio buttons.
     *
     * @return the text color
     */
    public final NanoColor getTextColor() {
        return textColor;
    }

    /**
     * Sets the color of the text in the radio buttons.
     *
     * @param textColor the text color
     */
    public final void setTextColor(NanoColor textColor) {
        this.textColor = textColor;
    }

    /**
     * Returns the color used to accent the selected radio button.
     *
     * @return the accent color
     */
    public final NanoColor getAccentColor() {
        return accentColor;
    }

    /**
     * Sets the color used to accent the selected radio button.
     *
     * @param accentColor the accent color
     */
    public final void setAccentColor(NanoColor accentColor) {
        this.accentColor = accentColor;
    }

    /**
     * Returns the color of the circles representing the radio buttons.
     *
     * @return the circle color
     */
    public final NanoColor getCircleColor() {
        return circleColor;
    }

    /**
     * Sets the color of the circles representing the radio buttons.
     *
     * @param circleColor the circle color
     */
    public final void setCircleColor(NanoColor circleColor) {
        this.circleColor = circleColor;
    }

    /**
     * Renders the {@link Widget}. This implementation does not perform rendering;
     * rendering is handled by the nested {@link RadioValue} class.
     */
    @Override
    public void render() {
    }

    /**
     * Represents a single radio button within the {@link RadioButtonWidget}.
     */
    public class RadioValue extends PanelWidget {
        /**
         * The text displayed on the radio button.
         */
        protected String value;

        /**
         * The action to perform when the radio button is clicked.
         */
        protected Consumer<RadioValue> onClickAction;

        /**
         * Whether the radio button is currently selected.
         */
        protected boolean selected = false;

        /**
         * Constructs a {@link RadioValue} with the specified text, position, and click action.
         * This class represents a single radio button within the {@link RadioButtonWidget}.
         *
         * @param value         the text to be displayed on the radio button
         * @param x             the x-coordinate of the {@link RadioValue} within its parent {@link Widget}
         * @param y             the y-coordinate of the {@link RadioValue} within its parent {@link Widget}
         * @param onClickAction the action to perform when the radio button is clicked
         */
        public RadioValue(String value, int x, int y, Consumer<RadioValue> onClickAction) {
            super(x, y, radioWidth, radioHeight);

            this.value = value;
            this.onClickAction = onClickAction;
            setDrawBorder(false);
            setBackgroundColor(NanoColor.TRANSPARENT);
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
                onClickAction.accept(this);
            }
            super.onLeftMouseUp(x, y);
        }

        /**
         * Renders the {@link Widget}
         */
        @Override
        public void render() {
            super.render();

            NanoColor newTextColor = textColor;
            Vector2f textSize = NanoDrawer.getTextSize(value, fontName, fontSize);

            drawCircle(circleRadius, height / 2, circleRadius, circleColor);

            if (selected) {
                drawCircle(circleRadius, height / 2, (float) circleRadius / 2, accentColor);
            }

            if (hovered) {
                newTextColor = accentColor.multiply(0.8f);

                if (lmbPressed) {
                    newTextColor = accentColor.multiply(0.5f);
                }
            }

            drawText(value, fontName, circleRadius * 2 + offset, (int) ((height - textSize.y) / 2 - fontSize / 4), fontSize, newTextColor);
        }
    }
}