package com.avrix.example;

import com.avrix.commands.*;
import com.avrix.enums.AccessLevel;
import com.avrix.enums.CommandScope;
import zombie.core.raknet.UdpConnection;

import java.util.Arrays;

/**
 * Test Command
 */
@CommandAccessLevel(accessLevel = AccessLevel.ADMIN)
@CommandChatReturn(text = "Return text")
@CommandExecutionScope(scope = CommandScope.BOTH)
@CommandName(command = "test")
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
