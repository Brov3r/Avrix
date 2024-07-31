package com.avrix.example;

import com.avrix.events.OnServerInitializeEvent;

/**
 * Event handler 'OnServerInitializeEvent'
 */
public class OnServerInitHandler extends OnServerInitializeEvent {
    /**
     * Called Event Handling Method
     */
    @Override
    public void handleEvent() {
        System.out.println("[$$$] Server init event!");
    }
}
