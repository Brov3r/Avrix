package com.avrix.example;

import com.avrix.events.OnWidgetManagerInitEvent;
import com.avrix.ui.NanoFont;
import com.avrix.ui.NanoImage;
import com.avrix.ui.NanoContext;

import java.io.File;

/**
 * Handle widget manager init
 */
public class WidgetManagerInitHandler extends OnWidgetManagerInitEvent {
    public static int testImageID = -1;
    public static int urlImageID = -1;

    /**
     * Called Event Handling Method
     *
     * @param context {@link NanoContext} in which NanoVG is initialized
     */
    @Override
    public void handleEvent(NanoContext context) {
        // Load custom font
        try {
            File coreJarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            NanoFont.createFont("Endeavourforever", coreJarFile.getPath(), "media/Endeavourforever.ttf");

            testImageID = NanoImage.loadImage(coreJarFile.getAbsolutePath(), "media/image_test.jpg");
            urlImageID = NanoImage.loadImage("https://avatarko.ru/img/kartinka/2/zhivotnye_kot_1990.jpg");
        } catch (Exception e) {
            System.out.println("[!] Failed to load resources: " + e.getMessage());
        }
    }
}
