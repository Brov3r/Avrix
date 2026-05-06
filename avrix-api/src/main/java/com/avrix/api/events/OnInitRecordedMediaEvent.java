package com.avrix.api.events;

import zombie.radio.media.RecordedMedia;

/**
 * Triggered when a media is being recorded.
 */
public abstract class OnInitRecordedMediaEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnInitRecordedMedia";
    }

    /**
     * Called Event Handling Method
     *
     * @param recordedMedia The recorded media to be initialized.
     */
    public abstract void handle(RecordedMedia recordedMedia);
}
