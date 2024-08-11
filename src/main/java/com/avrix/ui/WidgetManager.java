package com.avrix.ui;

import com.avrix.events.EventManager;
import com.avrix.ui.notify.Notify;
import com.avrix.ui.widgets.Widget;
import com.avrix.utils.WindowUtils;
import zombie.core.opengl.RenderThread;
import zombie.input.Mouse;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages a collection of {@link Widget} instances, handling rendering and updates.
 */
public class WidgetManager {
    private static NanoContext NanoContext;
    private static final List<Widget> widgetList = new CopyOnWriteArrayList<>();

    /**
     * Initializing the {@link WidgetManager} (creating NanoVG contexts)
     */
    public static void init() {
        RenderThread.queueInvokeOnRenderContext(() -> {
            if (NanoContext == null) {
                NanoContext = new NanoContext();
            }

            NanoFont.loadDefaultFonts();

            EventManager.invokeEvent("onWidgetManagerInitialized", NanoContext);
        });
    }

    /**
     * Renders all {@link Widget}s and processes input events.
     * Initializes the UI context if it is not already set.
     * Updates mouse events and renders each visible {@link Widget}.
     */
    public static void onRender() {
        List<Widget> widgetList = getWidgetList();
        widgetList.sort(Comparator.comparing(Widget::isAlwaysOnTop));

        InputWidgetHandler.updateMouseEvent();

        NanoContext.beginFrame(WindowUtils.getWindowWidth(), WindowUtils.getWindowHeight(), 1);

        EventManager.invokeEvent("onPreWidgetRender", NanoContext);

        for (Widget widget : widgetList) {
            if (!widget.isVisible()) continue;

            if (!widget.isVisibleWithinWindow()) continue;

            NanoDrawer.saveRenderState();
            NanoDrawer.intersectScissor(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());

            widget.preRender();

            widget.update();
            widget.render();
            widget.renderChildren();

            widget.postRender();

            NanoDrawer.restoreRenderState();
        }

        EventManager.invokeEvent("onPostWidgetRender", NanoContext);

        Notify.render();

        NanoContext.endFrame();
    }

    /**
     * Getting the NanoVG rendering context
     *
     * @return NanoVG context, if it does not exist (not initialized) returns {@code null}
     */
    public static NanoContext getContext() {
        return NanoContext;
    }

    /**
     * Getting a list of {@link Widget}s registered in the manager
     *
     * @return list of {@link Widget}s
     */
    public static List<Widget> getWidgetList() {
        return widgetList;
    }

    /**
     * Checks if the mouse pointer is over a custom UI element.
     *
     * @return true if the mouse pointer is over a custom UI element, false otherwise.
     */
    public static boolean isOverCustomUI() {
        int x = Mouse.getXA();
        int y = Mouse.getYA();

        for (Widget widget : getWidgetList()) {
            if (widget.isPointOver(x, y)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the specified {@link Widget} to the front of the rendering order, ensuring it is drawn above other {@link Widget}s.
     *
     * @param widget the {@link Widget} to bring to the front
     */
    public static void bringWidgetToTop(Widget widget) {
        if (widgetList.remove(widget)) {
            widgetList.add(widget);
        }
    }

    /**
     * Adds a widget to the list of {@link Widget}s to be managed and displayed.
     * The {@link Widget} is added only if it is not already present in the list.
     *
     * @param widget the {@link Widget} to be added
     */
    public static void addWidget(Widget widget) {
        if (!widgetList.contains(widget)) {
            widgetList.add(widget);
        }
    }

    /**
     * Removes a {@link Widget} from the list of {@link Widget}s to be managed and displayed.
     *
     * @param widget the {@link Widget} to be removed
     */
    public static void removeWidget(Widget widget) {
        widgetList.remove(widget);
    }
}