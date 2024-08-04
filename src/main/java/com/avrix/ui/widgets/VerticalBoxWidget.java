package com.avrix.ui.widgets;

import java.util.List;

/**
 * A {@link Widget} that arranges its children vertically with a specified margin between them.
 * It can also resize itself to fit its children if the autoresize option is enabled.
 */
public class VerticalBoxWidget extends BoxLayoutWidget {
    /**
     * Constructs a {@link VerticalBoxWidget} with the specified position, size, and autoresize option.
     *
     * @param x          the x-coordinate of the {@link Widget}
     * @param y          the y-coordinate of the {@link Widget}
     * @param width      the width of the {@link Widget}
     * @param height     the height of the {@link Widget}
     * @param autoresize whether the {@link Widget} should automatically resize to fit its children
     */
    public VerticalBoxWidget(int x, int y, int width, int height, boolean autoresize) {
        super(x, y, width, height, autoresize);
    }

    /**
     * Updates the layout of the {@link Widget}, positioning its children vertically
     * with the specified margin between them. Adjusts the size of the widget if autoresize is enabled.
     */
    public void updateLayout() {
        lastWidth = width;
        lastHeight = height;

        int maxWidth = 0;
        int currentY = 0;

        List<Widget> children = getChildren();
        for (Widget child : children) {
            child.setX(0);
            child.setY(currentY);
            currentY += child.getHeight() + margin;

            if (child.getWidth() > maxWidth) {
                maxWidth = child.getWidth();
            }
        }

        if (autoresize) {
            width = maxWidth;
            height = currentY - margin;

            if (parent != null && (width != lastWidth || height != lastHeight)) {
                parent.updateMaxScrollOffset();

                if (parent instanceof BoxLayoutWidget && !((BoxLayoutWidget) parent).isUpdatingLayout()) {
                    ((BoxLayoutWidget) parent).setUpdatingLayout(true);
                    ((BoxLayoutWidget) parent).updateLayout();
                    ((BoxLayoutWidget) parent).setUpdatingLayout(false);
                }
            }
        }
    }
}