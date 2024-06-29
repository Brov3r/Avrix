package com.avrix.commands;

import com.avrix.enums.AccessLevel;
import com.avrix.enums.CommandScope;
import com.avrix.utils.PlayerUtils;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A set of tools for handling custom commands
 */
public class CommandsManager {
    /**
     * Repository of all custom commands
     */
    private static final Map<String, Command> commandsMap = new HashMap<>();

    /**
     * Getting a map of registered user commands
     *
     * @return {@link Map} of registered commands. Key - command name, value - command instance
     */
    public static Map<String, Command> getRegisteredCommands() {
        return commandsMap;
    }

    /**
     * Adding a command to the system
     *
     * @param command chat command instance
     */
    public static void addCommand(Command command) {
        Class<? extends Command> commandClass = command.getClass();

        CommandName commandNameAnnotation = commandClass.getAnnotation(CommandName.class);
        CommandAccessLevel accessLevelAnnotation = commandClass.getAnnotation(CommandAccessLevel.class);
        CommandExecutionScope executionScopeAnnotation = commandClass.getAnnotation(CommandExecutionScope.class);
        CommandChatReturn chatReturnAnnotation = commandClass.getAnnotation(CommandChatReturn.class);

        if (commandNameAnnotation == null || commandNameAnnotation.value().isEmpty()) {
            System.out.printf("[!] Command '%s' is missing the @CommandName annotation or does not contain a value!%n", commandClass);
            return;
        }

        if (accessLevelAnnotation == null) {
            System.out.printf("[!] Command '%s' is missing the @CommandAccessLevel annotation!%n", commandClass);
            return;
        }

        if (executionScopeAnnotation == null) {
            System.out.printf("[!] Command '%s' is missing the @CommandExecutionScope annotation!%n", commandClass);
            return;
        }

        if (chatReturnAnnotation == null) {
            System.out.printf("[!] Command '%s' is missing the @CommandChatReturn annotation!%n", commandClass);
            return;
        }

        String commandName = commandNameAnnotation.value().toLowerCase();

        if (commandName.startsWith("!") || commandName.startsWith("/")) {
            commandName = commandName.substring(1);
        }

        if (commandsMap.containsKey(commandName)) {
            System.out.printf("[!] The '%s' command is already registered in the system!%n", commandName);
            return;
        }

        System.out.printf("[#] Added new custom command: '%s'%n", commandName);
        commandsMap.put(commandName, command);
    }

    /**
     * Checks if the execution of a specified command is allowed in a given scope.
     *
     * @param commandName The name of the command to be checked.
     * @param scopeType   The scope (CHAT, CONSOLE, or BOTH) in which to check the command's allowance.
     * @return true if the command is allowed in the specified scope, false otherwise.
     */
    private static boolean isCommandAllowed(String commandName, CommandScope scopeType) {
        Command command = commandsMap.get(commandName);

        if (command == null) return false;

        CommandExecutionScope executionScope = command.getClass().getAnnotation(CommandExecutionScope.class);

        CommandScope scope = executionScope.value();

        return scope == scopeType || scope == CommandScope.BOTH;
    }

    /**
     * Processing custom chat and console commands.
     *
     * @param playerConnection player connection (null if command is executed from console).
     * @param chatCommand      command entered by the player or console.
     * @return output text to chat when calling a command or null if there is no such command.
     */
    public static String handleCustomCommand(UdpConnection playerConnection, String chatCommand) {
        String[] commandArgs = getCommandArgs(chatCommand);
        if (commandArgs == null) return null;

        Command command = commandsMap.get(commandArgs[0].toLowerCase());
        if (command == null) return null;

        boolean isConsole = playerConnection == null;

        if (!isCommandAllowed(commandArgs[0], isConsole ? CommandScope.CONSOLE : CommandScope.CHAT)) {
            return "[!] This command is not allowed here.";
        }

        if (!isConsole) {
            IsoPlayer player = PlayerUtils.getPlayerByUsername(playerConnection.username);
            if (player == null) return "Could not check your access level! Please try later...";

            CommandAccessLevel accessLevelAnnotation = command.getClass().getAnnotation(CommandAccessLevel.class);

            AccessLevel requiredAccessLevel = accessLevelAnnotation.value();
            AccessLevel userAccessLevel = AccessLevel.fromString(player.accessLevel.toLowerCase());
            if (requiredAccessLevel.getPriority() > userAccessLevel.getPriority()) {
                return "[!] You do not have permission to execute this command.";
            }

        }

        String[] commandArgsToInvoke = Arrays.copyOfRange(commandArgs, 1, commandArgs.length);
        String playerName = playerConnection == null ? "Console" : playerConnection.username;
        System.out.printf("[#] Player '%s' called command '%s' with arguments: '%s'%n", playerName, commandArgs[0], Arrays.toString(commandArgsToInvoke));
        command.onInvoke(playerConnection, commandArgsToInvoke);

        CommandChatReturn chatReturnAnnotation = command.getClass().getAnnotation(CommandChatReturn.class);
        return chatReturnAnnotation.value();
    }

    /**
     * Parses a chat command string and extracts command arguments.
     *
     * @param chatCommand the input chat command string
     * @return an array of strings containing the command name and its arguments,
     * or null if the input is null, empty, or contains only whitespace
     */
    public static String[] getCommandArgs(String chatCommand) {
        if (chatCommand == null || chatCommand.trim().isEmpty()) return null;

        List<String> commandArgsList = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|\\S+").matcher(chatCommand.trim());
        while (matcher.find()) {
            String arg = matcher.group(1);
            if (arg == null) {
                arg = matcher.group();
            }

            commandArgsList.add(arg.trim());
        }

        if (commandArgsList.isEmpty()) return null;

        commandArgsList.removeIf(arg -> arg.isEmpty() && !arg.equals(commandArgsList.get(0)));

        String[] commandArgs = new String[commandArgsList.size()];
        commandArgs = commandArgsList.toArray(commandArgs);

        String commandName = commandArgs[0];
        if (commandName.startsWith("!") || commandName.startsWith("/")) {
            commandName = commandName.substring(1);
        }

        if (commandName.isEmpty()) return null;

        commandArgs[0] = commandName.toLowerCase();

        return commandArgs;
    }
}