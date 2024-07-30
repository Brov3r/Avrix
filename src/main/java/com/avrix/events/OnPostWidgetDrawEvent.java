package com.avrix.events;

import com.avrix.ui.NVGContext;

/**
 * Trigger when all widgets are rendered, but before the NanoVG frame has finished rendering
 */
public abstract class OnPostWidgetDrawEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onPostWidgetRender";
    }

    /**
     * Called Event Handling Method
     *
     * @param context {@link NVGContext} in which NanoVG is initialized
     */
    public abstract void handleEvent(NVGContext context);
}
