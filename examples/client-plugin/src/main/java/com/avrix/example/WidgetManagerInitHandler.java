package com.avrix.example;

import com.avrix.events.OnWidgetManagerInitEvent;
import com.avrix.ui.NVGContext;
import com.avrix.ui.NVGFont;

import java.io.File;

/**
 * Handle widget manager init
 */
public class WidgetManagerInitHandler extends OnWidgetManagerInitEvent {
    /**
     * Called Event Handling Method
     *
     * @param context {@link NVGContext} in which NanoVG is initialized
     */
    @Override
    public void handleEvent(NVGContext context) {
        // Load custom font
        try {
            File coreJarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            NVGFont.createFont("Endeavourforever", coreJarFile.getPath(), "media/Endeavourforever.ttf");
        } catch (Exception e) {
            System.out.println("[!] Failed to load custom fonts: " + e.getMessage());
        }
    }
}
