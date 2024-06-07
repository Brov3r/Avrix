package com.avrix.events;

import zombie.core.Language;

/**
 * Triggered when the language is changed through the settings
 */
public abstract class OnChangeLanguageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onChangeLanguage";
    }

    /**
     * Called Event Handling Method
     *
     * @param language New language information
     */
    public abstract void handleEvent(Language language);
}
