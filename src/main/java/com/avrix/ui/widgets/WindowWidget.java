package com.avrix.ui.widgets;

import com.avrix.ui.UIColor;
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
    protected UIColor headerColor = UIColor.LIGHT_BLACK.multiply(1.7f);

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

        closeButton = new ButtonWidget("\uf00d", getWidth() - 16 - 5, 5, 16, 16, 16, new UIColor("#e74c3c"), this::closeWindow);
        closeButton.setDrawBorder(false);
        closeButton.setFontSize(12);
        closeButton.setTextColor(UIColor.WHITE);
        closeButton.setFontName("FontAwesome");
        closeButton.setScrollLock(true);
        addChild(closeButton);

        this.originalVerticalScrollBarY = this.verticalScrollbar.getY();
        this.originalVerticalScrollBarHeight = this.verticalScrollbar.getHeight();
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
        return this.fontName;
    }

    /**
     * Gets the current title of the {@link Widget}'s header.
     *
     * @return the title text currently set for the header
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Sets the color of the {@link Widget}'s header.
     *
     * @param headerColor the color to use for the header
     */
    public void setHeaderColor(UIColor headerColor) {
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

        this.verticalScrollbar.setY(this.originalVerticalScrollBarY + this.headerHeight);
        this.verticalScrollbar.setHeight(this.originalVerticalScrollBarHeight - this.headerHeight);
    }

    /**
     * Updates and renders all child widgets of this {@link Widget}.
     * This method recursively calls the update and render methods on each child widget,
     * ensuring that the rendering order respects the hierarchy of widgets.
     */
    @Override
    public void renderChildren() {
        for (Widget child : this.children) {
            if (!child.isVisible()) continue;

            if (child.getContext() == null) {
                child.setContext(this.context);
            }

            // Limitation for scroll bars
            if (!child.equals(this.horizontalScrollbar) && !child.equals(this.verticalScrollbar) && !child.equals(this.closeButton)) {
                saveRenderState();
                intersectScissor(getX(), getY() + this.headerHeight,
                        this.verticalScrollbar.isVisible() ? this.getWidth() - this.verticalScrollbar.width - this.verticalScrollbar.borderOffset * 2 : this.getWidth(),
                        this.horizontalScrollbar.isVisible() ? this.getHeight() - this.horizontalScrollbar.height - this.horizontalScrollbar.borderOffset * 2 - this.headerHeight : this.getHeight() - this.headerHeight);
            }

            // Calculate absolute positions considering scrolling
            int absoluteX = child.isScrollLock() ? getX() + child.getX() : getX() + child.getX() - this.scrollX;
            int absoluteY = child.isScrollLock() ? getY() + child.getY() : getY() + child.getY() - this.scrollY;

            // Save original positions to restore later
            int originalX = child.getX();
            int originalY = child.getY();

            // Set the child's position to the absolute position
            child.setX(absoluteX);
            child.setY(absoluteY);

            child.saveRenderState();
            child.intersectScissor(absoluteX, absoluteY, child.getWidth(), child.getHeight());

            // Render child and its children
            child.preRender();
            child.update();
            child.render();
            child.renderChildren();
            child.postRender();

            // Restore the original positions
            child.restoreRenderState();
            child.setX(originalX);
            child.setY(originalY);

            // Limitation for scroll bars
            if (!child.equals(this.horizontalScrollbar) && !child.equals(this.verticalScrollbar) && !child.equals(this.closeButton)) {
                restoreRenderState();
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
        if (this.borderRadius != 0) {
            drawRoundedRect(0, 0, getWidth(), this.headerHeight, this.borderRadius, this.headerColor);
        } else {
            drawRect(0, 0, getWidth(), this.headerHeight, this.headerColor);
        }
    }

    /**
     * Final rendering, after the main render and rendering of child elements
     */
    @Override
    public void postRender() {
        Vector2f titleSize = getTextSize(this.title, this.fontName, this.titleFontSize);
        drawText(this.title, this.fontName, (int) ((getWidth() - titleSize.x) / 2), (int) (this.headerHeight - titleSize.y) / 2, this.titleFontSize, UIColor.WHITE);

        // Border
        if (this.drawBorder) {
            if (this.borderRadius != 0) {
                drawRoundedRectOutline(0, 0, getWidth(), getHeight(), this.borderRadius, this.borderWidth, this.borderColor);
            } else {
                drawRectOutline(0, 0, getWidth(), getHeight(), this.borderWidth, this.borderColor);
            }
        }
    }
}