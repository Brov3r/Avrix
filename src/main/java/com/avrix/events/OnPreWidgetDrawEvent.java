package com.avrix.events;

import com.avrix.ui.NVGContext;

/**
 * Trigger before widgets are drawn, but after NanoVG frame creation begins
 */
public abstract class OnPreWidgetDrawEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onPreWidgetRender";
    }

    /**
     * Called Event Handling Method
     *
     * @param context {@link NVGContext} in which NanoVG is initialized
     */
    public abstract void handleEvent(NVGContext context);
}
