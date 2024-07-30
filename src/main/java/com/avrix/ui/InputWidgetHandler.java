package com.avrix.ui;

import com.avrix.enums.KeyEventType;
import com.avrix.ui.widgets.Widget;
import zombie.input.Mouse;

/**
 * Handling input events for widgets
 */
public class InputWidgetHandler {
    private static int lastMouseX, lastMouseY = 0;

    /**
     * Handles key press events for all visible widgets.
     *
     * @param key the code of the key that was pressed
     */
    public static void onKeyPress(int key) {
        handleKeyEvent(key, KeyEventType.PRESS);
    }

    /**
     * Handles key repeat events for all visible widgets.
     *
     * @param key the code of the key that is being repeatedly pressed
     */
    public static void onKeyRepeat(int key) {
        handleKeyEvent(key, KeyEventType.REPEAT);
    }

    /**
     * Handles key release events for all visible widgets.
     *
     * @param key the code of the key that was released
     */
    public static void onKeyRelease(int key) {
        handleKeyEvent(key, KeyEventType.RELEASE);
    }

    /**
     * Handles keyboard events for all visible widgets.
     *
     * @param key       the code of the key
     * @param eventType the type of the keyboard event (press, repeat, release)
     */
    private static void handleKeyEvent(int key, KeyEventType eventType) {
        for (Widget widget : WidgetManager.getWidgetList()) {
            if (!widget.isVisible()) continue;

            switch (eventType) {
                case PRESS:
                    widget.onKeyPress(key);
                    break;
                case REPEAT:
                    widget.onKeyRepeat(key);
                    break;
                case RELEASE:
                    widget.onKeyRelease(key);
                    break;
            }
        }
    }

    /**
     * Updates mouse events and dispatches them to the appropriate widgets.
     * Processes mouse movement, button presses/releases, and wheel scrolling.
     */
    public static void updateMouseEvent() {
        int mouseX = Mouse.getXA();
        int mouseY = Mouse.getYA();
        int currentWheel = Mouse.getWheelState();

        boolean mouseMoved = mouseX != lastMouseX || mouseY != lastMouseY;

        Widget topWidget = null;

        for (int i = WidgetManager.getWidgetList().size() - 1; i >= 0; i--) {
            Widget widget = WidgetManager.getWidgetList().get(i);

            if (!widget.isVisible()) continue;

            boolean isPointOver = widget.isPointOver(mouseX, mouseY);

            if (topWidget == null && isPointOver) {
                topWidget = widget;
            }

            int relativeX = mouseX - widget.getX();
            int relativeY = mouseY - widget.getY();

            boolean isPointOverTop = isPointOver && widget.equals(topWidget);

            if (mouseMoved) {
                if (isPointOverTop) {
                    widget.onMouseMove(relativeX, relativeY);

                    if (!widget.hovered) {
                        widget.onMouseEnter(mouseX, mouseY);
                        widget.hovered = true;
                    }
                } else {
                    widget.onMouseMoveOutside(mouseX, mouseY);

                    if (widget.hovered) {
                        widget.onMouseExit(mouseX, mouseY);
                        widget.hovered = false;
                    }
                }
            }

            if (Mouse.isLeftPressed()) {
                if (isPointOverTop) {
                    widget.onLeftMouseDown(relativeX, relativeY);
                } else {
                    widget.onLeftMouseDownOutside(mouseX, mouseY);
                }
            }

            if (Mouse.isLeftReleased()) {
                if (isPointOverTop) {
                    widget.onLeftMouseUp(relativeX, relativeY);
                } else {
                    widget.onLeftMouseUpOutside(mouseX, mouseY);
                }
            }

            if (Mouse.isRightPressed()) {
                if (isPointOverTop) {
                    widget.onRightMouseDown(relativeX, relativeY);
                } else {
                    widget.onRightMouseDownOutside(mouseX, mouseY);
                }
            }

            if (Mouse.isRightReleased()) {
                if (isPointOverTop) {
                    widget.onRightMouseUp(relativeX, relativeY);
                } else {
                    widget.onRightMouseUpOutside(mouseX, mouseY);
                }
            }

            if (currentWheel != 0) {
                if (isPointOverTop) {
                    widget.onMouseWheel(relativeX, relativeY, currentWheel);
                } else {
                    widget.onMouseWheelOutside(mouseX, mouseY, currentWheel);
                }
            }
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }
}