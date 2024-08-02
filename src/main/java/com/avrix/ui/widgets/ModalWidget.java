package com.avrix.ui.widgets;

import com.avrix.ui.NVGColor;
import com.avrix.utils.WindowUtils;

/**
 * The {@code ModalWidget} class represents a modal window widget that can display
 * a content panel in the center of the screen.
 */
public class ModalWidget extends Widget {
    /**
     * The background color of the modal widget.
     */
    protected NVGColor backgroundColor = NVGColor.BLACK.alpha(0.85f);

    /**
     * The content panel displayed within the modal.
     */
    protected Widget contentPanel;

    /**
     * Constructs a new {@link ModalWidget} with the specified content width, height, and content panel.
     *
     * @param contentWidth  the width of the content panel
     * @param contentHeight the height of the content panel
     * @param contentPanel  the content panel to be displayed inside the modal
     */
    public ModalWidget(int contentWidth, int contentHeight, Widget contentPanel) {
        super(0, 0, WindowUtils.getWindowWidth(), WindowUtils.getWindowHeight());

        setAlwaysOnTop(true);

        this.contentPanel = contentPanel;
        this.contentPanel.setX((width - contentWidth) / 2);
        this.contentPanel.setY((height - contentHeight) / 2);

        addChild(this.contentPanel);
    }

    /**
     * Constructs a new {@link ModalWidget} with the specified content width and height.
     * A default {@link ScrollPanelWidget} is created as the content panel.
     *
     * @param contentWidth  the width of the content panel
     * @param contentHeight the height of the content panel
     */
    public ModalWidget(int contentWidth, int contentHeight) {
        super(0, 0, WindowUtils.getWindowWidth(), WindowUtils.getWindowHeight());

        setAlwaysOnTop(true);

        this.contentPanel = new ScrollPanelWidget((width - contentWidth) / 2, (height - contentHeight) / 2, contentWidth, contentHeight);
        addChild(this.contentPanel);
    }

    /**
     * Gets the background color of the modal widget.
     *
     * @return the background color
     */
    public NVGColor getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Sets the background color of the modal widget.
     *
     * @param backgroundColor the new background color
     */
    public void setBackgroundColor(NVGColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Gets the content panel of the modal widget.
     *
     * @return the content panel
     */
    public Widget getContentPanel() {
        return contentPanel;
    }

    /**
     * Sets a new content panel for the modal widget.
     * The previous content panel is removed and replaced with the new one.
     *
     * @param contentPanel the new content panel
     */
    public void setContentPanel(Widget contentPanel) {
        removeChild(this.contentPanel);

        this.contentPanel = contentPanel;

        this.contentPanel.setX((width - contentPanel.getWidth()) / 2);
        this.contentPanel.setY((height - contentPanel.getHeight()) / 2);

        addChild(this.contentPanel);
    }

    /**
     * Open current modal window
     */
    public void show() {
        addToScreen();
    }

    /**
     * Closes the current modal window
     */
    public void close() {
        removeFromScreen();
    }

    /**
     * Updates the {@link Widget}
     */
    @Override
    public void update() {
        super.update();

        int newWidth = WindowUtils.getWindowWidth();
        int newHeight = WindowUtils.getWindowHeight();
        if (width != newWidth || height != newHeight) {
            width = newWidth;
            height = newHeight;
        }
    }

    /**
     * Renders the {@link Widget}
     */
    @Override
    public void render() {
        drawRect(0, 0, width, height, backgroundColor);
    }
}