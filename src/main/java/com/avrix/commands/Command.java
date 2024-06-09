package com.avrix.commands;

import zombie.core.raknet.UdpConnection;

/**
 * Interface for implementing a new command.
 * Implementing classes must use the {@link CommandName} annotation to specify the command name.
 * They may also use the {@link CommandAccessLevel} annotation to specify the required access level for the command.
 * The {@link CommandChatReturn} annotation can be used to provide text that will be displayed in chat when the command is invoked.
 * The {@link CommandExecutionScope} annotation should be used to define where the command is available (e.g., in chat, in console, or both).
 */
public interface Command {
    /**
     * Performing a chat command action
     *
     * @param playerConnection {@link UdpConnection}, if called from the console, the connection will return as {@code null}
     * @param args             arguments of the received command
     */
    void onInvoke(UdpConnection playerConnection, String[] args);
}