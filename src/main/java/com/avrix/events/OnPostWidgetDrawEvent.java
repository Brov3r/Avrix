package com.avrix.events;

import com.avrix.ui.NanoContext;

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
     * @param context {@link NanoContext} in which NanoVG is initialized
     */
    public abstract void handleEvent(NanoContext context);
}
