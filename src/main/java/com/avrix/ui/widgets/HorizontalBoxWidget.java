package com.avrix.ui.widgets;

import java.util.List;

/**
 * A {@link Widget} that places its children horizontally with a specified amount of space between them.
 * It can also resize itself to fit its children if the auto resizing option is enabled.
 */
public class HorizontalBoxWidget extends BoxLayoutWidget {
    /**
     * Constructs a {@link HorizontalBoxWidget} with the specified position, size, and autoresize option.
     *
     * @param x          the x-coordinate of the {@link Widget}
     * @param y          the y-coordinate of the {@link Widget}
     * @param width      the width of the {@link Widget}
     * @param height     the height of the {@link Widget}
     * @param autoresize whether the {@link Widget} should automatically resize to fit its children
     */
    public HorizontalBoxWidget(int x, int y, int width, int height, boolean autoresize) {
        super(x, y, width, height, autoresize);
    }

    /**
     * Updates the layout of the {@link Widget}, positioning its children horizontally
     * with the specified margin between them. Adjusts the size of the widget if autoresize is enabled.
     */
    @Override
    public void updateLayout() {
        lastWidth = width;
        lastHeight = height;

        int maxHeight = 0;
        int currentX = 0;

        List<Widget> children = getChildren();
        for (Widget child : children) {
            child.setX(currentX);
            child.setY(0);
            currentX += child.getWidth() + margin;

            if (child.getHeight() > maxHeight) {
                maxHeight = child.getHeight();
            }
        }

        if (autoresize) {
            width = currentX - margin;
            height = maxHeight;

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