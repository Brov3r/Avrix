package com.avrix.enums;

/**
 * Enumerates the scopes where a command can be executed.
 */
public enum CommandScope {
    /**
     * Indicates that the command is available for execution only in the chat.
     */
    CHAT,

    /**
     * Indicates that the command is available for execution only in the console.
     */
    CONSOLE,

    /**
     * Indicates that the command is available for execution in both chat and console.
     */
    BOTH;
}