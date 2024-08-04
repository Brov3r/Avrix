package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGDrawer;
import org.joml.Vector2f;
import zombie.input.Mouse;

/**
 * Represents a {@link Widget} panel that can be customized with various properties such as font, title,
 * header color, and header height.
 */
public class WindowWidget extends ScrollPanelWidget {
    /**
     * The name of the font used for rendering text within the {@link Widget}.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * The name of the font used for rendering icon within the {@link Widget}.
     */
    protected String iconFontName = "FontAwesome";

    /**
     * Window resizing icon
     */
    protected String resizeIcon = "\uf00a";

    /**
     * The title text displayed in the {@link Widget}'s header.
     */
    protected String title;

    /**
     * The color of the {@link Widget}'s header.
     */
    protected NVGColor headerColor = NVGColor.LIGHT_BLACK.multiply(1.7f);

    /**
     * The height of the {@link Widget}'s header.
     */
    protected int headerHeight = 24;

    /**
     * Font size for drawing the title.
     */
    protected int titleFontSize = 12;

    /**
     * Window resize icon size
     */
    protected int resizeIconSize = 8;

    /**
     * Window resize icon offset
     */
    protected int resizeIconOffset = 5;

    /**
     * The x-coordinate offset for resize the window
     */
    protected int resizeOffsetX = 0;

    /**
     * The y-coordinate offset for resize the window
     */
    protected int resizeOffsetY = 0;

    /**
     * The size of the close button in pixels.
     */
    private final int closeButtonSize = 16;

    /**
     * The offset of the close button from the edge of the widget in pixels.
     */
    private final int closeButtonOffset = 5;

    /**
     * The original width of the widget before resizing.
     */
    protected int originalWidth = 0;

    /**
     * The original height of the widget before resizing.
     */
    protected int originalHeight = 0;

    /**
     * The minimum width of the widget in pixels.
     */
    protected int minSizeX = 100;

    /**
     * The minimum height of the widget in pixels.
     */
    protected int minSizeY = 100;

    /**
     * Window close button object
     */
    private ButtonWidget closeButton;

    /**
     * The original vertical position of the scrollbar before any transformation.
     */
    private int originalVerticalScrollBarY;

    /**
     * Indicates whether the widget can be resized.
     */
    protected boolean resizable = true;

    /**
     * Indicates whether the widget is currently being resized.
     */
    private boolean resizing = false;

    /**
     * Constructs a new {@link Widget} with the specified position and size.
     *
     * @param title  window title
     * @param x      the x-coordinate of the {@link Widget}'s position
     * @param y      the y-coordinate of the {@link Widget}'s position
     * @param width  the width of the {@link Widget}
     * @param height the height of the {@link Widget}
     */
    public WindowWidget(String title, int x, int y, int width, int height) {
        super(x, y, Math.max(width, 100), Math.max(height, 100));

        this.title = title;
    }

    /**
     * Closing a window (removes it from the screen)
     */
    public final void closeWindow() {
        this.removeFromScreen();
    }

    /**
     * Initializes the {@link Widget}
     */
    @Override
    public void onInitialize() {
        super.onInitialize();

        closeButton = new ButtonWidget("\uf00d", getWidth() - closeButtonSize - closeButtonOffset, closeButtonOffset, closeButtonSize, closeButtonSize, closeButtonSize, new NVGColor("#e74c3c"), this::closeWindow);
        closeButton.setDrawBorder(false);
        closeButton.setFontSize(12);
        closeButton.setTextColor(NVGColor.WHITE);
        closeButton.setFontName("FontAwesome");
        closeButton.setScrollLock(true);
        addChild(closeButton);

        originalVerticalScrollBarY = verticalScrollbar.getY();
    }

    /**
     * Returns the minimum width of the widget.
     *
     * @return the minimum width of the widget in pixels.
     */
    public final int getMinSizeX() {
        return minSizeX;
    }

    /**
     * Sets the minimum width of the widget.
     *
     * @param minSizeX the minimum width of the widget in pixels.
     */
    public final void setMinSizeX(int minSizeX) {
        this.minSizeX = minSizeX;
    }

    /**
     * Returns the minimum height of the widget.
     *
     * @return the minimum height of the widget in pixels.
     */
    public final int getMinSizeY() {
        return minSizeY;
    }

    /**
     * Sets the minimum height of the widget.
     *
     * @param minSizeY the minimum height of the widget in pixels.
     */
    public final void setMinSizeY(int minSizeY) {
        this.minSizeY = minSizeY;
    }

    /**
     * Checks if the widget is resizable.
     *
     * @return {@code true} if the widget is resizable, {@code false} otherwise.
     */
    public final boolean isResizable() {
        return resizable;
    }

    /**
     * Sets the ability to resize the widget.
     *
     * @param resizable {@code true} if the widget is resizable, {@code false} otherwise.
     */
    public final void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    /**
     * Adds a child widget to this widget's list of children.
     *
     * @param widget the widget to add as a child
     */
    @Override
    public synchronized void addChild(Widget widget) {
        super.addChild(widget);
        raiseCloseButton(widget != closeButton);
    }

    /**
     * Removes a child widget from this widget's list of children.
     *
     * @param widget the widget to remove from the list of children
     */
    @Override
    public synchronized void removeChild(Widget widget) {
        super.removeChild(widget);
        raiseCloseButton(widget != closeButton);
    }

    /**
     * Raising the close button to the top of rendering
     *
     * @param isAnotherWidget flag pointing to other widgets
     */
    private synchronized void raiseCloseButton(boolean isAnotherWidget) {
        if (isAnotherWidget && closeButton != null) {
            children.remove(closeButton);
            children.add(closeButton);
        }
    }

    /**
     * Sets the font name for the {@link Widget}.
     *
     * @param fontName the name of the font to use, specified as a string (e.g., "Arial", "Helvetica")
     */
    public final void setFont(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Sets the title of the {@link Widget}'s header.
     *
     * @param title the title text to display in the header
     */
    public final void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the current font name used by the {@link Widget}.
     *
     * @return the name of the font being used
     */
    public final String getFontName() {
        return fontName;
    }

    /**
     * Gets the current title of the {@link Widget}'s header.
     *
     * @return the title text currently set for the header
     */
    public final String getTitle() {
        return title;
    }

    /**
     * Sets the color of the {@link Widget}'s header.
     *
     * @param headerColor the color to use for the header
     */
    public final void setHeaderColor(NVGColor headerColor) {
        this.headerColor = headerColor;
    }

    /**
     * Sets the height of the {@link Widget}'s header.
     *
     * @param headerHeight the height of the header in pixels
     */
    public final void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }

    /**
     * Returns the height of the {@link Widget}'s header
     *
     * @return header height in pixels
     */
    public final int getHeaderHeight() {
        return headerHeight;
    }

    /**
     * Called when the left mouse button is pressed down over the {@link Widget}.
     *
     * @param x relative x-coordinate of the mouse position
     * @param y relative y-coordinate of the mouse position
     */
    @Override
    public void onLeftMouseDown(int x, int y) {
        if (x < width && y < height && x > width - resizeIconSize - resizeIconOffset && y > height - resizeIconSize - resizeIconOffset) {
            resizing = true;
            resizeOffsetX = x;
            resizeOffsetY = y;
            originalWidth = width;
            originalHeight = height;
            return;
        }

        super.onLeftMouseDown(x, y);
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
        resizing = false;
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
        resizing = false;
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();

        verticalScrollbar.setY(originalVerticalScrollBarY + headerHeight);

        if (resizing) {
            int mouseX = Mouse.getXA();
            int mouseY = Mouse.getYA();

            int deltaX = mouseX - getX() - resizeOffsetX;
            int deltaY = mouseY - getY() - resizeOffsetY;

            int newWidth = Math.max(minSizeX, originalWidth + deltaX);
            int newHeight = Math.max(minSizeY, originalHeight + deltaY);

            if (newWidth != getWidth() || newHeight != getHeight()) {
                setWidth(newWidth);
                setHeight(newHeight);

                updateMaxScrollOffset();
            }
        }

        if (resizable) {
            horizontalScrollbar.setWidth(width - horizontalScrollbar.borderOffset * 2 - resizeIconSize - resizeIconOffset);
            verticalScrollbar.setHeight((int) (height - verticalScrollbar.borderOffset * 1.5 - headerHeight - resizeIconSize - resizeIconOffset));
        } else {
            if (verticalScrollbar.isVisible()) {
                horizontalScrollbar.setWidth(width - horizontalScrollbar.borderOffset * 2 - verticalScrollbar.getWidth() * 2);
            } else {
                horizontalScrollbar.setWidth(width - horizontalScrollbar.borderOffset * 2);
            }
            verticalScrollbar.setHeight(height - verticalScrollbar.borderOffset * 2 - headerHeight);
        }

        closeButton.setX(width - closeButtonSize - closeButtonOffset);
    }

    /**
     * Updates and renders all child widgets of this {@link Widget}.
     * This method recursively calls the update and render methods on each child widget,
     * ensuring that the rendering order respects the hierarchy of widgets.
     */
    @Override
    public void renderChildren() {
        for (Widget child : getChildren()) {
            if (!child.isVisible()) continue;

            // Limitation for scroll bars
            if (!child.equals(horizontalScrollbar) && !child.equals(verticalScrollbar) && !child.equals(closeButton)) {
                NVGDrawer.saveRenderState();
                NVGDrawer.intersectScissor(getX(), getY() + headerHeight,
                        verticalScrollbar.isVisible() ? getWidth() - verticalScrollbar.width - verticalScrollbar.borderOffset * 2 : getWidth(),
                        horizontalScrollbar.isVisible() ? getHeight() - horizontalScrollbar.height - horizontalScrollbar.borderOffset * 2 - headerHeight : getHeight() - headerHeight);
            }

            // Calculate absolute positions considering scrolling
            int absoluteX = child.isScrollLock() ? getX() + child.getX() : getX() + child.getX() - scrollX;
            int absoluteY = child.isScrollLock() ? getY() + child.getY() : getY() + child.getY() - scrollY;

            // Save original positions to restore later
            int originalX = child.getX();
            int originalY = child.getY();

            // Set the child's absolute position
            child.setXA(absoluteX);
            child.setYA(absoluteY);

            // Set the child's position to the absolute position
            child.setX(absoluteX);
            child.setY(absoluteY);

            NVGDrawer.saveRenderState();
            NVGDrawer.intersectScissor(absoluteX, absoluteY, child.getWidth(), child.getHeight());

            // Render child and its children
            child.preRender();
            child.update();
            child.render();
            child.renderChildren();
            child.postRender();

            // Restore the original positions
            NVGDrawer.restoreRenderState();
            child.setX(originalX);
            child.setY(originalY);

            // Limitation for scroll bars
            if (!child.equals(horizontalScrollbar) && !child.equals(verticalScrollbar) && !child.equals(closeButton)) {
                NVGDrawer.restoreRenderState();
            }
        }
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        super.render();

        // Header
        if (borderRadius != 0) {
            drawRoundedRect(0, 0, getWidth(), headerHeight, borderRadius, headerColor);
        } else {
            drawRect(0, 0, getWidth(), headerHeight, headerColor);
        }
    }

    /**
     * Final rendering, after the main render and rendering of child elements
     */
    @Override
    public void postRender() {
        Vector2f titleSize = NVGDrawer.getTextSize(title, fontName, titleFontSize);
        drawText(title, fontName, (int) ((getWidth() - titleSize.x) / 2), (int) (headerHeight - titleSize.y) / 2, titleFontSize, NVGColor.WHITE);

        // Border
        if (drawBorder) {
            if (borderRadius != 0) {
                drawRoundedRectOutline(0, 0, getWidth(), getHeight(), borderRadius, borderWidth, borderColor);
            } else {
                drawRectOutline(0, 0, getWidth(), getHeight(), borderWidth, borderColor);
            }
        }

        if (resizable) {
            Vector2f iconSize = NVGDrawer.getTextSize(resizeIcon, iconFontName, resizeIconSize);
            drawText(resizeIcon, iconFontName, (int) (width - iconSize.x - resizeIconOffset), (int) (height - iconSize.y - resizeIconOffset), resizeIconSize, NVGColor.WHITE);
        }
    }
}