package com.avrix.example;

import com.avrix.events.OnWidgetManagerInitEvent;
import com.avrix.ui.NanoContext;
import com.avrix.ui.NanoFont;

import java.io.File;

/**
 * Handle widget manager init
 */
public class WidgetManagerInitHandler extends OnWidgetManagerInitEvent {
    public static File jarCoreFile;

    /**
     * Called Event Handling Method
     *
     * @param context {@link NanoContext} in which NanoVG is initialized
     */
    @Override
    public void handleEvent(NanoContext context) {
        // Load custom font
        try {
            jarCoreFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            NanoFont.createFont("Endeavourforever", jarCoreFile.getPath(), "media/Endeavourforever.ttf");
        } catch (Exception e) {
            System.out.println("[!] Failed to load resources: " + e.getMessage());
        }
    }
}
