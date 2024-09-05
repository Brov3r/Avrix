package com.avrix.example;

import com.avrix.api.client.WindowUtils;
import com.avrix.events.OnPreWidgetDrawEvent;
import com.avrix.ui.NanoColor;
import com.avrix.ui.NanoContext;
import com.avrix.ui.NanoDrawer;
import com.avrix.ui.NanoImage;
import zombie.GameWindow;

/**
 * Draw HUD
 */
public class HUDHandler extends OnPreWidgetDrawEvent {
    private static final float AMPLITUDE = 10.0f; // Maximum up and down displacement
    private static final float FREQUENCY = 5.0f; // Motion frequency (cycles per second)
    private float phase = 0.0f; // Sine wave phase to calculate vertical position
    private long lastTime = System.currentTimeMillis();

    private float smoothedFPS = GameWindow.averageFPS;
    private static final float SMOOTHING_FACTOR = 0.01f;


    /**
     * Called Event Handling Method
     *
     * @param context {@link NanoContext} in which NanoVG is initialized
     */
    @Override
    public void handleEvent(NanoContext context) {
        smoothedFPS += SMOOTHING_FACTOR * (GameWindow.averageFPS - smoothedFPS);
        String fps = String.format("FPS: %.0f", smoothedFPS);
        NanoDrawer.drawText("Hello client plugin!", "Endeavourforever", 10, 10, 32, NanoColor.ORANGE);
        NanoDrawer.drawText(fps, "Montserrat-Regular", 10, WindowUtils.getWindowHeight() - 24, 14, NanoColor.ORANGE);

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;

        phase += FREQUENCY * deltaTime;
        if (phase > 2 * Math.PI) {
            phase -= (float) (2 * Math.PI);
        }

        float y = 70 + AMPLITUDE * (float) Math.sin(phase);
        float x = 120 + AMPLITUDE * (float) Math.cos(phase);


        NanoDrawer.drawImage(NanoImage.loadImage(WidgetManagerInitHandler.jarCoreFile.getAbsolutePath(), "media/image_test.jpg"), 10, (int) y, 100, 100, 1);
        NanoDrawer.drawImage(NanoImage.loadImage("https://gas-kvas.com/uploads/posts/2023-02/1675462147_gas-kvas-com-p-fonovii-risunok-2k-2.jpg"), (int) x, (int) y, 100, 100, 1);
    }
}