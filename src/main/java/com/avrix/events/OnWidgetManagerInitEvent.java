package com.avrix.events;

import com.avrix.ui.NanoContext;

/**
 * Triggers when the widget manager is fully initialized (contexts for NanoVG are installed)
 */
public abstract class OnWidgetManagerInitEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onWidgetManagerInitialized";
    }

    /**
     * Called Event Handling Method
     *
     * @param context {@link NanoContext} in which NanoVG is initialized
     */
    public abstract void handleEvent(NanoContext context);
}
