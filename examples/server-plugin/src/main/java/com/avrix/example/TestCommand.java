package com.avrix.example;

import com.avrix.commands.*;
import com.avrix.enums.AccessLevel;
import com.avrix.enums.CommandScope;
import zombie.core.raknet.UdpConnection;

import java.util.Arrays;

/**
 * Test Command
 */
@CommandName("test")
@CommandAccessLevel(AccessLevel.NONE)
@CommandExecutionScope(CommandScope.BOTH)
@CommandDescription("Command description")
public class TestCommand extends Command {
    /**
     * Performing a chat command action
     *
     * @param playerConnection {@link UdpConnection}, if called from the console, the connection will return as {@code null}
     * @param args             arguments of the received command
     */
    @Override
    public String onInvoke(UdpConnection playerConnection, String[] args) {
        return "[###] Test command. Args: " + Arrays.toString(args);
    }
}
