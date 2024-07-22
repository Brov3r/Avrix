package com.avrix.ui;

import com.avrix.Launcher;
import com.avrix.events.EventManager;
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
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.nvgCreateFont;

/**
 * Manages a collection of {@link Widget} instances, handling rendering, input events, and updates.
 * Provides static methods to interact with the widget list and process user input.
 */
public class WidgetManager {
    private static UIContext uiContext;
    private static final List<Widget> widgetList = new ArrayList<>();
    private static boolean isInitialized = false;
    private static int lastMouseX, lastMouseY = 0;

    /**
     * Initializing the widget manager (installing NanoVG contexts)
     */
    private static void init() {
        if (uiContext == null) {
            uiContext = new UIContext();
        }

        // Load custom fonts
        try {
            File coreJarFile = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            createFont("Montserrat-Regular", coreJarFile.getPath(), "media/fonts/Montserrat-Regular.ttf");
            createFont("Arial-Regular", coreJarFile.getPath(), "media/fonts/Arial-Regular.ttf");
            createFont("Roboto-Regular", coreJarFile.getPath(), "media/fonts/Roboto-Regular.ttf");
            createFont("FontAwesome", coreJarFile.getPath(), "media/fonts/FontAwesome.ttf");
        } catch (Exception e) {
            System.out.println("[!] Failed to load custom fonts: " + e.getMessage());
        }

        EventManager.invokeEvent("onWidgetManagerInitialized", uiContext);

        isInitialized = true;
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

        updateMouseEvent();

        uiContext.beginFrame(WindowUtils.getWindowWidth(), WindowUtils.getWindowHeight(), 1);

        for (Widget widget : widgetList) {
            if (widget.getContext() == null) widget.setContext(uiContext);

            if (widget.isVisible()) {
                widget.update();
                widget.render();
            }
        }

        uiContext.endFrame();
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
        for (Widget widget : widgetList) {
            if (widget.isVisible()) {
                widget.onKeyPress(uiContext, key);
            }
        }
    }

    /**
     * Handles key repeat events for all visible widgets.
     *
     * @param key the code of the key that is being repeatedly pressed
     */
    public static void onKeyRepeat(int key) {
        for (Widget widget : widgetList) {
            if (widget.isVisible()) {
                widget.onKeyRepeat(uiContext, key);
            }
        }
    }

    /**
     * Handles key release events for all visible widgets.
     *
     * @param key the code of the key that was released
     */
    public static void onKeyRelease(int key) {
        for (Widget widget : widgetList) {
            if (widget.isVisible()) {
                widget.onKeyRelease(uiContext, key);
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

        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            for (Widget widget : widgetList) {
                if (widget.isVisible() && widget.isPointOver(mouseX, mouseY)) {
                    widget.onMouseMove(uiContext, (float) (mouseX - widget.getX()), (float) (mouseY - widget.getY()));
                    break;
                }
            }
        }

        if (Mouse.isLeftPressed()) {
            for (Widget widget : widgetList) {
                if (widget.isVisible() && widget.isPointOver(mouseX, mouseY)) {
                    widget.onLeftMouseDown(uiContext, (float) (mouseX - widget.getX()), (float) (mouseY - widget.getY()));
                    break;
                }
            }
        }

        if (Mouse.isLeftReleased()) {
            for (Widget widget : widgetList) {
                if (widget.isVisible() && widget.isPointOver(mouseX, mouseY)) {
                    widget.onLeftMouseUp(uiContext, (float) (mouseX - widget.getX()), (float) (mouseY - widget.getY()));
                    break;
                }
            }
        }

        if (Mouse.isRightPressed()) {
            for (Widget widget : widgetList) {
                if (widget.isVisible() && widget.isPointOver(mouseX, mouseY)) {
                    widget.onRightMouseDown(uiContext, (float) (mouseX - widget.getX()), (float) (mouseY - widget.getY()));
                    break;
                }
            }
        }

        if (Mouse.isRightReleased()) {
            for (Widget widget : widgetList) {
                if (widget.isVisible() && widget.isPointOver(mouseX, mouseY)) {
                    widget.onRightMouseUp(uiContext, (float) (mouseX - widget.getX()), (float) (mouseY - widget.getY()));
                    break;
                }
            }
        }

        int currentWheel = Mouse.getWheelState();
        if (currentWheel != 0) {
            for (Widget widget : widgetList) {
                if (widget.isVisible() && widget.isPointOver(mouseX, mouseY)) {
                    widget.onMouseWheel(uiContext, (float) (mouseX - widget.getX()), (float) (mouseY - widget.getY()), currentWheel);
                    break;
                }
            }
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }
}