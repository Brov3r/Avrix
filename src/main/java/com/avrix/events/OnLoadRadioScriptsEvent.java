package com.avrix.events;

import zombie.radio.scripting.RadioScriptManager;

/**
 * Triggered when radio scripts are being loaded.
 */
public abstract class OnLoadRadioScriptsEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnLoadRadioScripts";
    }

    /**
     * Called Event Handling Method
     *
     * @param radioScriptManager The radio script manager.
     * @param worldInit          True if the world has not yet been initialized.
     */
    public abstract void handleEvent(RadioScriptManager radioScriptManager, Boolean worldInit);
}
