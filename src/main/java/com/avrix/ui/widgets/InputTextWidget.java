package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGDrawer;
import org.joml.Vector2f;
import org.lwjglx.input.Keyboard;
import org.tinylog.Logger;
import zombie.core.Clipboard;

public class InputTextWidget extends PanelWidget {
    protected String value = "";
    protected String placeholder = "";
    protected String textFont = "Montserrat-Regular";

    protected NVGColor textColor = NVGColor.WHITE;
    protected NVGColor placeholderColor = NVGColor.WHITE.multiply(0.3f);
    protected NVGColor selectionColor = new NVGColor(0.3f, 0.3f, 0.9f, 0.5f);

    protected int fontSize;
    protected int borderOffset = 5;
    protected int cursorOffset = 2;
    private int cursorPosition = 0;
    private int textX = 0;
    private int textY = 0;
    private int textOffset = 0;
    private int maxTextWidth;

    private char currentKeyValue;

    private long lastBlinkTime = 0;
    private long lastRepeatTime = 0;
    private final long repeatDelay = 100;

    protected boolean editable = true;
    protected boolean active = false;
    protected boolean LMBDown = false;
    protected boolean cursorVisible = true;

    public InputTextWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.fontSize = Math.max(12, height / 2);
        this.maxTextWidth = width - this.borderOffset * 2;
    }

    public final void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public void onLeftMouseUp(int x, int y) {
        super.onLeftMouseUp(x, y);

        if (this.LMBDown) {
            this.active = true;
        }

        this.LMBDown = false;
    }

    @Override
    public void onLeftMouseDown(int x, int y) {
        super.onLeftMouseDown(x, y);

        this.LMBDown = true;

        if (this.active) {
            // Рассчитываем позицию курсора на основе клика
            int clickX = x - getX() - this.borderOffset;

            // Смещение текста при прокрутке
            Vector2f textSize = NVGDrawer.getTextSize(this.value, this.textFont, this.fontSize);
            int textWidth = (int) textSize.x;
            this.textOffset = 0;

            if (textWidth > this.maxTextWidth) {
                this.textOffset = textWidth - this.maxTextWidth;
            }

            clickX += this.textOffset;
            int currentX = 0;
            this.cursorPosition = 0;

            for (int i = 0; i < this.value.length(); i++) {
                String substring = this.value.substring(0, i + 1);
                Vector2f substringSize = NVGDrawer.getTextSize(substring, this.textFont, this.fontSize);
                int charWidth = (int) substringSize.x - currentX;

                if (currentX + charWidth / 2 >= clickX) {
                    break;
                }

                currentX = (int) substringSize.x;
                this.cursorPosition++;
            }

            if (clickX >= currentX) {
                this.cursorPosition = this.value.length();
            }
        }
    }

    @Override
    public void onLeftMouseUpOutside(int x, int y) {
        super.onLeftMouseDownOutside(x, y);

        this.active = false;
    }

    @Override
    public void onKeyPress(int key) {
        super.onKeyPress(key);

        if (!this.active) return;

        char keyValue = Keyboard.getEventCharacter();
        this.currentKeyValue = keyValue;
        this.lastRepeatTime = System.currentTimeMillis();

        Logger.info("KEY {}, VALUE {}", key, keyValue);

        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && key == Keyboard.KEY_V) {
            String clipboardText = Clipboard.getClipboard();
            this.value = new StringBuilder(this.value).insert(this.cursorPosition, clipboardText).toString();
            this.cursorPosition += clipboardText.length();
            return;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && key == Keyboard.KEY_C) {
            Clipboard.setClipboard(this.value);
            return;
        }

        if (key == Keyboard.KEY_BACK && !this.value.isEmpty() && this.cursorPosition > 0) {
            this.value = new StringBuilder(this.value).deleteCharAt(this.cursorPosition - 1).toString();
            this.cursorPosition--;
            return;
        }

        if (keyValue >= 32 && keyValue <= 126) {
            this.value = new StringBuilder(this.value).insert(this.cursorPosition, keyValue).toString();
            this.cursorPosition++;
        }
    }

    @Override
    public void onKeyRelease(int key) {
        super.onKeyRelease(key);
    }

    @Override
    public void onKeyRepeat(int key) {
        super.onKeyRepeat(key);
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();
    }

    @Override
    public void render() {
        super.render();

        NVGDrawer.saveRenderState();
        NVGDrawer.intersectScissor(getX() + this.borderOffset, getY() + this.borderOffset, getWidth() - this.borderOffset * 2, getHeight() - this.borderOffset * 2);

        Vector2f textSize = NVGDrawer.getTextSize(this.value, this.textFont, this.fontSize);
        Vector2f placeholderSize = NVGDrawer.getTextSize(this.placeholder, this.textFont, this.fontSize);

        this.textY = (int) ((this.height - textSize.y) / 2);
        int placeholderY = (int) ((this.height - placeholderSize.y) / 2);

        this.textX = this.borderOffset;
        this.textOffset = 0;

        if (textSize.x > this.maxTextWidth) {
            this.textOffset = (int) (textSize.x - this.maxTextWidth + this.borderOffset);
            this.textX -= this.textOffset;
        }

        if (this.value.isEmpty()) {
            drawText(this.placeholder, this.textFont, this.borderOffset, placeholderY, this.fontSize, this.placeholderColor);
        } else {
            drawText(this.value, this.textFont, this.textX, this.textY, this.fontSize, this.textColor);
        }

        if (System.currentTimeMillis() - lastBlinkTime > 500) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = System.currentTimeMillis();
        }

        if (this.active && cursorVisible) {
            Vector2f cursorSize = NVGDrawer.getTextSize(this.value.substring(0, this.cursorPosition), this.textFont, this.fontSize);
            int cursorX = this.textX + (int) cursorSize.x + cursorOffset;
            if (cursorX < this.width - this.borderOffset) {
                drawLine(cursorX, this.textY, cursorX, this.textY + (int) textSize.y, 1.0f, this.textColor);
            }
        }

        NVGDrawer.restoreRenderState();
    }
}
