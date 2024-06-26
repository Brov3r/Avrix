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
@CommandAccessLevel(AccessLevel.ADMIN)
@CommandExecutionScope(CommandScope.BOTH)
@CommandChatReturn("Return text")
public class TestCommand implements Command {
    /**
     * Performing a chat command action
     *
     * @param playerConnection {@link UdpConnection}, if called from the console, the connection will return as {@code null}
     * @param args             arguments of the received command
     */
    @Override
    public void onInvoke(UdpConnection playerConnection, String[] args) {
        System.out.println("[###] Test command. Args: " + Arrays.toString(args));
    }
}
