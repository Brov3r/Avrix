package com.avrix.commands;

import com.avrix.enums.AccessLevel;
import com.avrix.enums.CommandScope;
import zombie.core.raknet.UdpConnection;

/**
 * Interface for implementing a new command.
 * Implementing classes must use the {@link CommandName} annotation to specify the command name.
 * They may also use the {@link CommandAccessLevel} annotation to specify the required access level for the command.
 * The {@link CommandChatReturn} annotation can be used to provide text that will be displayed in chat when the command is invoked.
 * The {@link CommandExecutionScope} annotation should be used to define where the command is available (e.g., in chat, in console, or both).
 * The {@link CommandDescription} annotation can be used to add a description to the command, providing additional information about its functionality.
 */
public abstract class Command {
    /**
     * Performing a chat command action
     *
     * @param playerConnection {@link UdpConnection}, if called from the console, the connection will return as {@code null}
     * @param args             arguments of the received command
     */
    public abstract void onInvoke(UdpConnection playerConnection, String[] args);

    /**
     * Retrieves the command name from the {@link CommandName} annotation.
     *
     * @return the command name, or {@code null} if the annotation is not present
     */
    public String getCommandName() {
        CommandName annotation = this.getClass().getAnnotation(CommandName.class);
        return (annotation != null) ? annotation.value() : null;
    }

    /**
     * Retrieves the command description from the {@link CommandDescription} annotation.
     *
     * @return the command description, or {@code null} if the annotation is not present
     */
    public String getDescription() {
        CommandDescription annotation = this.getClass().getAnnotation(CommandDescription.class);
        return (annotation != null) ? annotation.value() : null;
    }

    /**
     * Retrieves the access level required for the command from the {@link CommandAccessLevel} annotation.
     *
     * @return the required {@link AccessLevel}, or {@code null} if the annotation is not present
     */
    public AccessLevel getAccessLevel() {
        CommandAccessLevel annotation = this.getClass().getAnnotation(CommandAccessLevel.class);
        return (annotation != null) ? annotation.value() : null;
    }

    /**
     * Retrieves the chat return message from the {@link CommandChatReturn} annotation.
     *
     * @return the chat return message, or {@code null} if the annotation is not present
     */
    public String getChatReturnText() {
        CommandChatReturn annotation = this.getClass().getAnnotation(CommandChatReturn.class);
        return (annotation != null) ? annotation.value() : null;
    }

    /**
     * Retrieves the execution scope of the command from the {@link CommandExecutionScope} annotation.
     *
     * @return the {@link CommandScope}, or {@code null} if the annotation is not present
     */
    public CommandScope getExecutionScope() {
        CommandExecutionScope annotation = this.getClass().getAnnotation(CommandExecutionScope.class);
        return (annotation != null) ? annotation.value() : null;
    }
}