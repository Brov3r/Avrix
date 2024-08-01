package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGDrawer;
import org.joml.Vector2f;
import org.lwjglx.input.Keyboard;
import zombie.core.Clipboard;

import java.util.function.Consumer;

/**
 * A widget for input text that supports text editing, selection, and clipboard operations.
 */
public class InputTextWidget extends PanelWidget {
    /**
     * The current text value of the input widget.
     */
    protected String value = "";

    /**
     * The placeholder text shown when the value is empty.
     */
    protected String placeholder = "";

    /**
     * The font used for displaying text.
     */
    protected String textFont = "Montserrat-Regular";

    /**
     * The color of the text.
     */
    protected NVGColor textColor = NVGColor.WHITE;

    /**
     * The color of the placeholder text.
     */
    protected NVGColor placeholderColor = NVGColor.WHITE.multiply(0.3f);

    /**
     * The color of the selected text highlight.
     */
    protected NVGColor selectionColor = new NVGColor(0.3f, 0.3f, 0.9f, 0.5f);

    /**
     * Action to be executed when the text changes.
     */
    protected Consumer<String> onTextChangeAction;

    /**
     * The maximum width of the text area.
     */
    private int maxTextWidth;

    /**
     * The font size used for displaying text.
     */
    protected int fontSize;

    /**
     * The offset from the border for the text.
     */
    protected int borderOffset = 5;

    /**
     * The delay between key repeats in milliseconds.
     */
    protected int repeatDelay = 80;

    /**
     * The current position of the cursor in the text.
     */
    private int cursorPosition = 0;

    /**
     * The offset of the text within the widget.
     */
    private int textOffset = 0;

    /**
     * The starting position of the selection.
     */
    private int selectionStart = -1;

    /**
     * The ending position of the selection.
     */
    private int selectionEnd = -1;

    /**
     * The current key value being processed.
     */
    private char currentKeyValue;

    /**
     * The last time the cursor blinked.
     */
    private long lastBlinkTime = 0;

    /**
     * The last time a key repeat event was processed.
     */
    private long lastRepeatTime = 0;

    /**
     * Indicates if the text can be edited.
     */
    protected boolean editable = true;

    /**
     * Indicates if the widget is currently active.
     */
    protected boolean active = false;

    /**
     * Indicates if the left mouse button is currently pressed.
     */
    protected boolean LMBDown = false;

    /**
     * Indicates if the cursor is currently visible.
     */
    protected boolean cursorVisible = true;

    /**
     * Indicates if text selection is currently active.
     */
    protected boolean selecting = false;

    /**
     * Constructs an InputTextWidget with the specified position and size.
     *
     * @param x      The x-coordinate of the widget.
     * @param y      The y-coordinate of the widget.
     * @param width  The width of the widget.
     * @param height The height of the widget.
     */
    public InputTextWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.fontSize = Math.max(12, height / 2);
        this.maxTextWidth = width - this.borderOffset * 2;
        Keyboard.enableRepeatEvents(true);
    }

    /**
     * Sets the action to be executed when the text changes.
     *
     * @param onTextChangeAction The action to be executed on text change.
     */
    public final void setOnTextChangeAction(Consumer<String> onTextChangeAction) {
        this.onTextChangeAction = onTextChangeAction;
    }

    /**
     * Gets the current value of the text widget.
     *
     * @return The current text value.
     */
    public final String getValue() {
        return value;
    }

    /**
     * Sets the current value of the text widget.
     *
     * @param value The new text value.
     */
    public final void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the placeholder text shown when the text value is empty.
     *
     * @return The placeholder text.
     */
    public final String getPlaceholder() {
        return placeholder;
    }

    /**
     * Sets the placeholder text to be shown when the text value is empty.
     *
     * @param placeholder The new placeholder text.
     */
    public final void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    /**
     * Gets the font used for displaying text.
     *
     * @return The text font.
     */
    public final String getTextFont() {
        return textFont;
    }

    /**
     * Sets the font to be used for displaying text.
     *
     * @param textFont The new text font.
     */
    public final void setTextFont(String textFont) {
        this.textFont = textFont;
    }

    /**
     * Gets the color of the text.
     *
     * @return The text color.
     */
    public final NVGColor getTextColor() {
        return textColor;
    }

    /**
     * Sets the color of the text.
     *
     * @param textColor The new text color.
     */
    public final void setTextColor(NVGColor textColor) {
        this.textColor = textColor;
    }

    /**
     * Gets the color of the placeholder text.
     *
     * @return The placeholder text color.
     */
    public final NVGColor getPlaceholderColor() {
        return placeholderColor;
    }

    /**
     * Sets the color of the placeholder text.
     *
     * @param placeholderColor The new placeholder text color.
     */
    public final void setPlaceholderColor(NVGColor placeholderColor) {
        this.placeholderColor = placeholderColor;
    }

    /**
     * Gets the color of the selected text highlight.
     *
     * @return The selection color.
     */
    public final NVGColor getSelectionColor() {
        return selectionColor;
    }

    /**
     * Sets the color of the selected text highlight.
     *
     * @param selectionColor The new selection color.
     */
    public final void setSelectionColor(NVGColor selectionColor) {
        this.selectionColor = selectionColor;
    }

    /**
     * Gets the font size used for displaying text.
     *
     * @return The font size.
     */
    public final int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size used for displaying text.
     *
     * @param fontSize The new font size.
     */
    public final void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Gets the offset from the border for the text.
     *
     * @return The border offset.
     */
    public final int getBorderOffset() {
        return borderOffset;
    }

    /**
     * Sets the offset from the border for the text.
     *
     * @param borderOffset The new border offset.
     */
    public final void setBorderOffset(int borderOffset) {
        this.borderOffset = borderOffset;
    }

    /**
     * Gets the delay between key repeat events in milliseconds.
     *
     * @return The repeat delay.
     */
    public final int getRepeatDelay() {
        return repeatDelay;
    }

    /**
     * Sets the delay between key repeat events in milliseconds.
     *
     * @param repeatDelay The new repeat delay.
     */
    public final void setRepeatDelay(int repeatDelay) {
        this.repeatDelay = repeatDelay;
    }

    /**
     * Checks if the text widget is editable.
     *
     * @return {@code true} if the text widget is editable, {@code false} otherwise.
     */
    public final boolean isEditable() {
        return editable;
    }

    /**
     * Sets whether the text widget is editable.
     *
     * @param editable {@code true} to make the widget editable, {@code false} otherwise.
     */
    public final void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Checks if the text widget is currently active.
     *
     * @return {@code true} if the widget is active, {@code false} otherwise.
     */
    public final boolean isActive() {
        return active;
    }

    /**
     * Sets the active state of the text widget.
     *
     * @param active {@code true} to activate the widget, {@code false} to deactivate it.
     */
    public final void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Checks if the cursor is currently visible.
     *
     * @return {@code true} if the cursor is visible, {@code false} otherwise.
     */
    public final boolean isCursorVisible() {
        return cursorVisible;
    }

    /**
     * Sets the visibility of the cursor.
     *
     * @param cursorVisible {@code true} to make the cursor visible, {@code false} to hide it.
     */
    public final void setCursorVisible(boolean cursorVisible) {
        this.cursorVisible = cursorVisible;
    }

    /**
     * Handles the event when the left mouse button is released.
     * This method updates the widget's state based on the mouse release and finalizes the selection if needed.
     *
     * @param x The x-coordinate of the mouse position at the time of release.
     * @param y The y-coordinate of the mouse position at the time of release.
     */
    @Override
    public void onLeftMouseUp(int x, int y) {
        super.onLeftMouseUp(x, y);

        if (LMBDown) {
            active = true;
            if (selectionStart == -1) {
                selectionStart = cursorPosition;
            }
            selectionEnd = cursorPosition;
        }

        LMBDown = false;
        selecting = false;
    }

    /**
     * Handles the event when the left mouse button is released outside the widget.
     * This method clears any active selection and deactivates the widget.
     *
     * @param x The x-coordinate of the mouse position at the time of release.
     * @param y The y-coordinate of the mouse position at the time of release.
     */
    @Override
    public void onLeftMouseDownOutside(int x, int y) {
        super.onLeftMouseDownOutside(x, y);
        clearSelection();
        selecting = false;
        active = false;
    }

    /**
     * Handles the event when the left mouse button is pressed down within the widget.
     * This method sets up the widget's state for selection and cursor positioning.
     *
     * @param x The x-coordinate of the mouse position at the time of press.
     * @param y The y-coordinate of the mouse position at the time of press.
     */
    @Override
    public void onLeftMouseDown(int x, int y) {
        super.onLeftMouseDown(x, y);

        LMBDown = true;
        active = true;
        selecting = true;
        cursorPosition = getCursorPositionFromMouse(x);
        selectionStart = cursorPosition;
        selectionEnd = cursorPosition;
    }

    /**
     * Handles the event when the left mouse button is released outside the widget.
     * This method deactivates the widget if the mouse was dragging, but reactivates it if it was being selected.
     *
     * @param x The x-coordinate of the mouse position at the time of release.
     * @param y The y-coordinate of the mouse position at the time of release.
     */
    @Override
    public void onLeftMouseUpOutside(int x, int y) {
        super.onLeftMouseUpOutside(x, y);

        active = false;

        if (selecting && LMBDown) {
            selecting = false;
            LMBDown = false;
            active = true;
        }
    }

    /**
     * Handles the event when the mouse is moved within the widget.
     * This method updates the cursor position and adjusts text offset based on the mouse movement during selection.
     *
     * @param x The x-coordinate of the mouse position at the time of movement.
     * @param y The y-coordinate of the mouse position at the time of movement.
     */
    @Override
    public void onMouseMove(int x, int y) {
        super.onMouseMove(x, y);

        if (LMBDown && selecting) {
            cursorPosition = getCursorPositionFromMouse(x);
            selectionEnd = cursorPosition;

            if (selectionEnd != selectionStart) {
                int selectionLength = Math.abs(selectionEnd - selectionStart);
                if (selectionLength > 0) {
                    int start = Math.min(selectionStart, selectionEnd);
                    int end = Math.max(selectionStart, selectionEnd);

                    if (start >= 0 && end <= value.length()) {
                        float endTextWidth = NVGDrawer.getTextSize(value.substring(0, end), textFont, fontSize).x;
                        float startTextWidth = NVGDrawer.getTextSize(value.substring(0, start), textFont, fontSize).x;

                        if (endTextWidth > maxTextWidth + textOffset) {
                            textOffset += (int) NVGDrawer.getTextSize(value.substring(end - 1, end), textFont, fontSize).x;
                        } else if (startTextWidth < textOffset) {
                            textOffset -= (int) NVGDrawer.getTextSize(value.substring(start, start + 1), textFont, fontSize).x;
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles the event when a key is repeatedly pressed.
     * This method manages key repeat actions based on the configured repeat delay.
     *
     * @param key The key code of the key being repeated.
     */
    @Override
    public void onKeyRepeat(int key) {
        super.onKeyRepeat(key);

        if (!active || !editable) return;

        if (System.currentTimeMillis() - lastRepeatTime < repeatDelay) {
            return;
        }

        lastRepeatTime = System.currentTimeMillis();
        handleKeyInput(key);
    }

    /**
     * Handles the event when a key is pressed down.
     * This method processes key inputs for text manipulation and updates the widget accordingly.
     *
     * @param key The key code of the key being pressed.
     */
    @Override
    public void onKeyPress(int key) {
        super.onKeyPress(key);

        if (!active || !editable) return;

        currentKeyValue = Keyboard.getEventCharacter();
        lastRepeatTime = System.currentTimeMillis() + repeatDelay * 3L;
        handleKeyInput(key);
    }

    /**
     * Executes the action specified for text change, if any.
     * This method triggers the `onTextChangeAction` callback.
     */
    private void onTextChange() {
        if (onTextChangeAction == null) return;

        onTextChangeAction.accept(value);
    }

    /**
     * Handles key input for text manipulation, including character insertion, deletion, and selection actions.
     *
     * @param key The key code of the key being processed.
     */
    private void handleKeyInput(int key) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            switch (key) {
                case Keyboard.KEY_V -> {
                    if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
                        deleteSelection();
                    }
                    String clipboardText = Clipboard.getClipboard();
                    value = new StringBuilder(value).insert(cursorPosition, clipboardText).toString();
                    cursorPosition += clipboardText.length();
                    clearSelection();
                    onTextChange();
                }
                case Keyboard.KEY_A -> {
                    selectionStart = 0;
                    selectionEnd = value.length();
                }
                case Keyboard.KEY_C -> {
                    if (selectionStart != -1 && selectionEnd != -1) {
                        Clipboard.setClipboard(value.substring(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd)));
                    }
                }
            }
        }

        switch (key) {
            case Keyboard.KEY_BACK -> {
                if (!value.isEmpty() && cursorPosition > 0) {
                    if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
                        deleteSelection();
                    } else {
                        value = new StringBuilder(value).deleteCharAt(cursorPosition - 1).toString();
                        cursorPosition--;
                    }
                    clearSelection();
                    onTextChange();
                }
            }
            case Keyboard.KEY_LEFT -> {
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                updateSelectionOnArrow(1);
            }
            case Keyboard.KEY_RIGHT -> {
                if (cursorPosition < value.length()) {
                    cursorPosition++;
                }
                updateSelectionOnArrow(-1);
            }
            default -> {
                if (!Character.isISOControl(currentKeyValue)) {
                    if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
                        deleteSelection();
                    }
                    value = new StringBuilder(value).insert(cursorPosition, currentKeyValue).toString();
                    cursorPosition++;
                    clearSelection();
                    onTextChange();
                }
            }

        }
    }

    /**
     * Updates the selection range based on arrow key input while the Shift key is pressed.
     *
     * @param direction The direction of the arrow key: 1 for left, -1 for right.
     */
    private void updateSelectionOnArrow(int direction) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            if (selectionStart == -1)
                selectionStart = Math.min(value.length(), Math.max(0, cursorPosition + direction));
            selectionEnd = cursorPosition;
            selecting = true;
        } else {
            clearSelection();
        }
    }

    /**
     * Clears any active text selection.
     */
    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        selecting = false;
    }

    /**
     * Deletes the selected text range from the widget's value.
     */
    private void deleteSelection() {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        value = new StringBuilder(value).delete(start, end).toString();
        cursorPosition = start;
        clearSelection();
    }

    /**
     * Determines the cursor position based on the given mouse x-coordinate.
     *
     * @param mouseX The x-coordinate of the mouse position.
     * @return The calculated cursor position.
     */
    private int getCursorPositionFromMouse(int mouseX) {
        int position = 0;
        float offsetX = borderOffset - textOffset;
        float totalTextWidth = NVGDrawer.getTextSize(value, textFont, fontSize).x;

        if (mouseX < offsetX) {
            return 0;
        } else if (mouseX > offsetX + totalTextWidth) {
            return value.length();
        }

        for (int i = 1; i <= value.length(); i++) {
            float charWidth = NVGDrawer.getTextSize(value.substring(0, i), textFont, fontSize).x;
            if (mouseX <= offsetX + charWidth) {
                position = i - 1;
                break;
            }
        }
        return position;
    }

    /**
     * Renders the text widget, including text, selection highlight, and cursor.
     * This method also handles text offset adjustments and cursor blinking.
     */
    @Override
    public void render() {
        super.render();

        NVGDrawer.saveRenderState();
        NVGDrawer.intersectScissor(getX() + borderOffset, getY() + borderOffset, getWidth() - borderOffset * 2, getHeight() - borderOffset * 2);

        Vector2f textSize = NVGDrawer.getTextSize(value, textFont, fontSize);

        int textY = (int) ((height - textSize.y) / 2) - borderOffset / 2;
        int textX = borderOffset;

        if (textSize.x > maxTextWidth) {
            Vector2f cursorTextSize = NVGDrawer.getTextSize(value.substring(0, cursorPosition), textFont, fontSize);
            if (cursorTextSize.x - textOffset > maxTextWidth) {
                textOffset = (int) (cursorTextSize.x - maxTextWidth) + borderOffset;
            } else if (cursorTextSize.x - textOffset < 0) {
                textOffset = (int) cursorTextSize.x;
            }
            textX -= textOffset;
        } else {
            textOffset = 0;
        }

        if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            float startX = borderOffset + NVGDrawer.getTextSize(value.substring(0, start), textFont, fontSize).x - textOffset;
            float endX = borderOffset + NVGDrawer.getTextSize(value.substring(0, end), textFont, fontSize).x - textOffset;
            drawRect((int) startX, borderOffset, (int) (endX - startX), y - borderOffset * 2, selectionColor);
        }

        if (value.isEmpty() && !active) {
            drawText(placeholder, textFont, borderOffset, textY, fontSize, placeholderColor);
        } else {
            drawText(value, textFont, textX, textY, fontSize, textColor);
        }

        if (System.currentTimeMillis() - lastBlinkTime > 500) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = System.currentTimeMillis();
        }

        if (active && cursorVisible) {
            Vector2f cursorSize = NVGDrawer.getTextSize(value.substring(0, cursorPosition), textFont, fontSize);
            int cursorX = borderOffset + (int) cursorSize.x - textOffset;
            if (cursorX >= borderOffset && cursorX <= getWidth() - borderOffset) {
                drawLine(cursorX, borderOffset, cursorX, y - borderOffset * 2, 1.0f, textColor);
            }
        }

        NVGDrawer.restoreRenderState();
    }
}