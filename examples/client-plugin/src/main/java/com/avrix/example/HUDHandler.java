package com.avrix.example;

import com.avrix.events.OnPreWidgetDrawEvent;
import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGContext;
import com.avrix.ui.NVGDrawer;

import java.io.File;

/**
 * Draw HUD
 */
public class HUDHandler extends OnPreWidgetDrawEvent {
    private File coreJarFile;
    private static final float AMPLITUDE = 10.0f; // Максимальное смещение вверх и вниз
    private static final float FREQUENCY = 5.0f; // Частота движения (цикл в секунду)
    private float phase = 0.0f; // Фаза синусоиды для расчета вертикальной позиции
    private long lastTime = System.currentTimeMillis();

    public HUDHandler(Main main) {
        super();

        try {
            coreJarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            System.out.println("[!] Exception HUD: " + e.getMessage());
        }
    }

    /**
     * Called Event Handling Method
     *
     * @param context {@link NVGContext} in which NanoVG is initialized
     */
    @Override
    public void handleEvent(NVGContext context) {
        NVGDrawer.drawText("Hello client plugin!", "Endeavourforever", 10, 10, 32, NVGColor.ORANGE);

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;

        phase += FREQUENCY * deltaTime;
        if (phase > 2 * Math.PI) {
            phase -= (float) (2 * Math.PI);
        }

        float y = 70 + AMPLITUDE * (float) Math.sin(phase);
        float x = 120 + AMPLITUDE * (float) Math.cos(phase);

        if (coreJarFile != null) {
            NVGDrawer.drawImage(coreJarFile.getAbsolutePath(), "media/image_test.jpg", 10, (int) y, 100, 100);
        }
        NVGDrawer.drawImage("https://ltdfoto.ru/images/2024/07/31/c53d539b59c19087182f0b1c53bb52de.jpg", (int) x, (int) y, 100, 100);
    }
}
