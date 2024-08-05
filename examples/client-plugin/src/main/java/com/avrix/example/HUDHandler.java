package com.avrix.example;

import com.avrix.events.OnPreWidgetDrawEvent;
import com.avrix.ui.NVGColor;
import com.avrix.ui.NVGContext;
import com.avrix.ui.NVGDrawer;
import com.avrix.utils.WindowUtils;

/**
 * Draw HUD
 */
public class HUDHandler extends OnPreWidgetDrawEvent {
    private static final float AMPLITUDE = 10.0f; // Maximum up and down displacement
    private static final float FREQUENCY = 5.0f; // Motion frequency (cycles per second)
    private float phase = 0.0f; // Sine wave phase to calculate vertical position
    private long lastTime = System.currentTimeMillis();


    /**
     * Called Event Handling Method
     *
     * @param context {@link NVGContext} in which NanoVG is initialized
     */
    @Override
    public void handleEvent(NVGContext context) {
        NVGDrawer.drawText("Hello client plugin!", "Endeavourforever", 10, 10, 32, NVGColor.ORANGE);
        NVGDrawer.drawText("Another hello", "Montserrat-Regular", 10, WindowUtils.getWindowHeight() - 24, 14, NVGColor.ORANGE);

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000.0f;
        lastTime = currentTime;

        phase += FREQUENCY * deltaTime;
        if (phase > 2 * Math.PI) {
            phase -= (float) (2 * Math.PI);
        }

        float y = 70 + AMPLITUDE * (float) Math.sin(phase);
        float x = 120 + AMPLITUDE * (float) Math.cos(phase);


        NVGDrawer.drawImage(WidgetManagerInitHandler.testImageID, 10, (int) y, 100, 100, 1);
        NVGDrawer.drawImage(WidgetManagerInitHandler.urlImageID, (int) x, (int) y, 100, 100, 1);
    }
}