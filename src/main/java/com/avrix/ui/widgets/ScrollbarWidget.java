package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import zombie.input.Mouse;

/**
 * The {@link ScrollbarWidget} class represents a scrollbar component that can be either horizontal or vertical.
 * It extends the {@link PanelWidget} class.
 */
public class ScrollbarWidget extends PanelWidget {
    /**
     * The default width for a vertical scrollbar.
     */
    private final int DEFAULT_VERTICAL_WIDTH = 6;

    /**
     * The default height for a horizontal scrollbar.
     */
    private final int DEFAULT_HORIZONTAL_HEIGHT = 6;

    /**
     * Number of pixels to set back from the edge of the widget
     */
    protected int borderOffset = 5;

    /**
     * Number of pixels of slider space from each scrollbar wall
     */
    protected int thumbOffset = 2;

    /**
     * Compressed width for horizontal scrollbar
     */
    protected int compressedWidth = this.width;

    /**
     * Default background color
     */
    private final NVGColor DEFAULT_BACKGROUND_COLOR = new NVGColor("#1e272e");

    /**
     * Thumb color
     */
    protected NVGColor thumbColor = new NVGColor("#808e9b");

    /**
     * Indicates whether the scrollbar is horizontal. If {@code false}, the scrollbar is vertical.
     */
    protected boolean horizontal;

    /**
     * Indicates whether the scrollbar is currently being dragged by the user.
     * This flag is used to track the dragging state of the scrollbar.
     */
    private boolean dragging = false;

    /**
     * The x-coordinate of the mouse position when the dragging of the scrollbar started.
     * This is used to calculate the amount of movement and update the scrollbar position accordingly.
     */
    private int dragStartX;

    /**
     * The y-coordinate of the mouse position when the dragging of the scrollbar started.
     * This is used to calculate the amount of movement and update the scrollbar position accordingly.
     */
    private int dragStartY;

    /**
     * The initial x-coordinate of the scrollbar's scroll position when dragging started.
     * This value helps in calculating the new scroll position based on the mouse movement.
     */
    private int scrollStartX;

    /**
     * The initial y-coordinate of the scrollbar's scroll position when dragging started.
     * This value helps in calculating the new scroll position based on the mouse movement.
     */
    private int scrollStartY;

    /**
     * Constructs a {@link ScrollbarWidget} with the specified orientation.
     *
     * @param horizontal {@code true} if the scrollbar should be horizontal, {@code false} if it should be vertical.
     */
    public ScrollbarWidget(boolean horizontal) {
        super(0, 0, 0, 0);
        this.horizontal = horizontal;
        this.backgroundColor = this.DEFAULT_BACKGROUND_COLOR;
        this.drawBorder = false;
        this.borderRadius = horizontal ? this.DEFAULT_HORIZONTAL_HEIGHT : this.DEFAULT_VERTICAL_WIDTH;
        this.borderRadius = this.borderRadius / 2;
        this.scrollable = false;
        this.scrollLock = true;
    }

    /**
     * Constructs a {@link ScrollbarWidget} with a default vertical orientation.
     */
    public ScrollbarWidget() {
        this(false);
    }

    /**
     * Gets the color of the scrollbar thumb (the draggable part of the scrollbar).
     *
     * @return the color of the scrollbar thumb as a {@link NVGColor} object
     */
    public final NVGColor getThumbColor() {
        return thumbColor;
    }

    /**
     * Sets the color of the scrollbar thumb (the draggable part of the scrollbar).
     *
     * @param thumbColor the color to set for the scrollbar thumb, specified as a {@link NVGColor} object
     */
    public final void setThumbColor(NVGColor thumbColor) {
        this.thumbColor = thumbColor;
    }

    /**
     * Gets the border offset of the scrollbar.
     *
     * @return the border offset in pixels as an {@code int}
     */
    public final int getBorderOffset() {
        return borderOffset;
    }

    /**
     * Sets the border offset of the scrollbar.
     *
     * @param borderOffset the border offset in pixels to set, specified as an {@code int}
     */
    public final void setBorderOffset(int borderOffset) {
        this.borderOffset = borderOffset;
    }

    /**
     * Gets the compressed width of the scrollbar.
     *
     * @return the compressed width in pixels as an {@code int}
     */
    public final int getCompressedWidth() {
        return compressedWidth;
    }

    /**
     * Sets the compressed width of the scrollbar.
     *
     * @param compressedWidth the compressed width in pixels to set, specified as an {@code int}
     */
    public final void setCompressedWidth(int compressedWidth) {
        this.compressedWidth = compressedWidth;
    }

    /**
     * Update the {@link Widget} position
     */
    public void updatePosition() {
        if (parent == null) {
            return;
        }

        if (!horizontal) {
            width = DEFAULT_VERTICAL_WIDTH;
            height = parent.height - borderOffset * 2;
            x = parent.width - width - borderOffset;
            y = borderOffset;
        } else {
            height = DEFAULT_HORIZONTAL_HEIGHT;
            compressedWidth = parent.width - borderOffset * 2 - DEFAULT_VERTICAL_WIDTH * 2;
            width = parent.width - borderOffset * 2;
            x = borderOffset;
            y = parent.height - height - borderOffset;
        }
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

        dragging = true;

        dragStartX = Mouse.getXA();
        dragStartY = Mouse.getYA();

        if (!horizontal) {
            scrollStartY = parent.scrollY;
        } else {
            scrollStartX = parent.scrollX;
        }
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
        dragging = false;
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
        dragging = false;
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();
        if (dragging) {
            int mouseX = Mouse.getXA();
            int mouseY = Mouse.getYA();
            updateScrollbarPosition(mouseX, mouseY);
        }
    }

    /**
     * Update the scrollbar position based on the mouse position.
     *
     * @param mouseX absolute x-coordinate of the mouse position
     * @param mouseY absolute y-coordinate of the mouse position
     */
    private void updateScrollbarPosition(int mouseX, int mouseY) {
        if (!horizontal) {
            int deltaY = mouseY - dragStartY;
            int contentHeight = height - 2 * thumbOffset;
            int thumbHeight = (int) ((float) height / (height + parent.maxScrollY) * contentHeight);
            int maxThumbY = contentHeight - thumbHeight;
            parent.scrollY = Math.max(0, Math.min(parent.maxScrollY, scrollStartY + (int) ((float) deltaY / maxThumbY * parent.maxScrollY)));
        } else {
            int deltaX = mouseX - dragStartX;
            int contentWidth = width - 2 * thumbOffset;
            int thumbWidth = (int) ((float) width / (width + parent.maxScrollX) * contentWidth);
            int maxThumbX = contentWidth - thumbWidth;
            parent.scrollX = Math.max(0, Math.min(parent.maxScrollX, scrollStartX + (int) ((float) deltaX / maxThumbX * parent.maxScrollX)));
        }
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        super.render();

        int scrollX = parent.scrollX;
        int scrollY = parent.scrollY;

        int maxScrollX = parent.maxScrollX;
        int maxScrollY = parent.maxScrollY;

        if (!horizontal) {
            // Vertical thumb
            int thumbHeight = (int) ((float) height / (height + maxScrollY) * (height - 2 * thumbOffset));
            int thumbY = (int) ((float) scrollY / maxScrollY * (height - thumbHeight - 2 * thumbOffset)) + thumbOffset;
            if (borderRadius != 0) {
                drawRoundedRect(thumbOffset, thumbY, width - 2 * thumbOffset, thumbHeight, (float) width / 4, thumbColor);
            } else {
                drawRect(thumbOffset, thumbY, width - 2 * thumbOffset, thumbHeight, thumbColor);
            }
        } else {
            // Horizontal thumb
            int thumbWidth = (int) ((float) width / (width + maxScrollX) * (width - 2 * thumbOffset));
            int thumbX = (int) ((float) scrollX / maxScrollX * (width - thumbWidth - 2 * thumbOffset)) + thumbOffset;
            if (borderRadius != 0) {
                drawRoundedRect(thumbX, thumbOffset, thumbWidth, height - 2 * thumbOffset, (float) height / 4, thumbColor);
            } else {
                drawRect(thumbX, thumbOffset, thumbWidth, height - 2 * thumbOffset, thumbColor);
            }
        }
    }
}