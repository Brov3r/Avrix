package com.avrix.ui;

import com.avrix.Launcher;
import com.avrix.enums.KeyEventType;
import com.avrix.events.EventManager;
import com.avrix.ui.widgets.Widget;
import com.avrix.utils.WindowUtils;
import zombie.input.Mouse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.nvgCreateFont;

/**
 * Manages a collection of {@link Widget} instances, handling rendering, input events, and updates.
 * Provides static methods to interact with the widget list and process user input.
 */
public class WidgetManager {
    private static UIContext uiContext;
    private static final List<Widget> widgetList = new ArrayList<>();
    private static final List<Widget> renderWidgetList = new ArrayList<>();

    private static boolean isInitialized = false;
    private static int lastMouseX, lastMouseY = 0;

    /**
     * Initializing the widget manager (installing NanoVG contexts)
     */
    private static void init() {
        if (uiContext == null) {
            uiContext = new UIContext();
        }

        loadDefaultFonts();

        EventManager.invokeEvent("onWidgetManagerInitialized", uiContext);

        isInitialized = true;
    }

    /**
     * Loading custom default fonts
     */
    private static void loadDefaultFonts() {
        try {
            File coreJarFile = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            createFont("Montserrat-Regular", coreJarFile.getPath(), "media/fonts/Montserrat-Regular.ttf");
            createFont("Arial-Regular", coreJarFile.getPath(), "media/fonts/Arial-Regular.ttf");
            createFont("Roboto-Regular", coreJarFile.getPath(), "media/fonts/Roboto-Regular.ttf");
            createFont("FontAwesome", coreJarFile.getPath(), "media/fonts/FontAwesome.ttf");
        } catch (Exception e) {
            System.out.println("[!] Failed to load custom fonts: " + e.getMessage());
        }
    }

    /**
     * Creates and loads a font into NanoVG with the specified name from a font file inside a JAR.
     *
     * @param fontName         the name to assign to the font.
     * @param jarFilePath      the path to the JAR file containing the font.
     * @param internalFilePath the path to the font file inside the JAR.
     * @throws IOException if an I/O error occurs while reading the font file.
     */
    public static void createFont(String fontName, String jarFilePath, String internalFilePath) throws IOException {
        URL jarUrl = new URL("jar:file:" + jarFilePath + "!/" + internalFilePath);
        try (InputStream inputStream = jarUrl.openStream()) {
            Path tempFontFile = Files.createTempFile(fontName + "_temp-font", ".ttf");
            Files.copy(inputStream, tempFontFile, StandardCopyOption.REPLACE_EXISTING);
            createFont(fontName, tempFontFile);

            Files.delete(tempFontFile);
        } catch (IOException e) {
            System.out.printf("[!] File '%s' not found inside JAR file '%s'!%n", internalFilePath, jarFilePath);
            throw e;
        }
    }

    /**
     * Creates and loads a font into NanoVG with the specified name from the given path.
     *
     * @param fontName the name to assign to the font.
     * @param fontPath the path to the font file (e.g., a TrueType font file).
     */
    public static void createFont(String fontName, Path fontPath) {
        if (uiContext == null) {
            System.out.println("[!] Font creation must occur after the WidgetManager is initialized! Use the `OnWidgetManagerInitializedEvent`!");
            return;
        }

        int fontId = nvgCreateFont(uiContext.get(), fontName, fontPath.toString());
        if (fontId == -1) {
            System.out.printf("[!] Failed to load font '%s' at path: '%s'%n", fontName, fontPath);
        }
    }

    /**
     * Renders all widgets and processes input events.
     * Initializes the UI context if it is not already set.
     * Updates mouse events and renders each visible widget.
     */
    public static void onRender() {
        if (!isInitialized) init();

        widgetList.sort(Comparator.comparing(Widget::isAlwaysOnTop));

        renderWidgetList.clear();
        renderWidgetList.addAll(widgetList);

        updateMouseEvent();

        uiContext.beginFrame(WindowUtils.getWindowWidth(), WindowUtils.getWindowHeight(), 1);

        for (Widget widget : renderWidgetList) {
            if (widget.getContext() == null) widget.setContext(uiContext);

            if (!widget.isVisible()) continue;

            widget.startScissor(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());

            widget.update();
            widget.render();

            widget.endScissor();
        }

        uiContext.endFrame();
    }

    /**
     * Moves the specified widget to the front of the rendering order, ensuring it is drawn above other widgets.
     *
     * @param widget the widget to bring to the front
     */
    public static void bringWidgetToTop(Widget widget) {
        if (widgetList.remove(widget)) {
            widgetList.add(widget);
        }
    }

    /**
     * Adds a widget to the list of widgets to be managed and displayed.
     * The widget is added only if it is not already present in the list.
     *
     * @param widget the widget to be added
     */
    public static void addWidget(Widget widget) {
        if (!widgetList.contains(widget)) {
            widgetList.add(widget);
        }
    }

    /**
     * Removes a widget from the list of widgets to be managed and displayed.
     *
     * @param widget the widget to be removed
     */
    public static void removeWidget(Widget widget) {
        widgetList.remove(widget);
    }

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
        for (Widget widget : renderWidgetList) {
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
    private static void updateMouseEvent() {
        int mouseX = Mouse.getXA();
        int mouseY = Mouse.getYA();
        int currentWheel = Mouse.getWheelState();

        boolean mouseMoved = mouseX != lastMouseX || mouseY != lastMouseY;

        Widget topWidget = null;

        for (int i = renderWidgetList.size() - 1; i >= 0; i--) {
            Widget widget = renderWidgetList.get(i);

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
                } else {
                    widget.onMouseMoveOutside(mouseX, mouseY);
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