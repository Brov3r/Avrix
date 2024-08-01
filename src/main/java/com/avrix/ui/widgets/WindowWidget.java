package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGDrawer;
import org.joml.Vector2f;

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
     * Window close button object
     */
    private ButtonWidget closeButton;

    /**
     * The original vertical position of the scrollbar before any transformation.
     */
    private int originalVerticalScrollBarY;

    /**
     * The original height of the vertical scrollbar before any transformation.
     */
    private int originalVerticalScrollBarHeight;

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

        closeButton = new ButtonWidget("\uf00d", getWidth() - 16 - 5, 5, 16, 16, 16, new NVGColor("#e74c3c"), this::closeWindow);
        closeButton.setDrawBorder(false);
        closeButton.setFontSize(12);
        closeButton.setTextColor(NVGColor.WHITE);
        closeButton.setFontName("FontAwesome");
        closeButton.setScrollLock(true);
        addChild(closeButton);

        originalVerticalScrollBarY = verticalScrollbar.getY();
        originalVerticalScrollBarHeight = verticalScrollbar.getHeight();
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
    public void setFont(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Sets the title of the {@link Widget}'s header.
     *
     * @param title the title text to display in the header
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the current font name used by the {@link Widget}.
     *
     * @return the name of the font being used
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * Gets the current title of the {@link Widget}'s header.
     *
     * @return the title text currently set for the header
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the color of the {@link Widget}'s header.
     *
     * @param headerColor the color to use for the header
     */
    public void setHeaderColor(NVGColor headerColor) {
        this.headerColor = headerColor;
    }

    /**
     * Sets the height of the {@link Widget}'s header.
     *
     * @param headerHeight the height of the header in pixels
     */
    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();

        verticalScrollbar.setY(originalVerticalScrollBarY + headerHeight);
        verticalScrollbar.setHeight(originalVerticalScrollBarHeight - headerHeight);
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
    }
}