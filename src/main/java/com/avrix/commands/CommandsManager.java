package com.avrix.commands;

import com.avrix.api.PlayerUtils;
import com.avrix.enums.AccessLevel;
import com.avrix.enums.CommandScope;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of tools for handling custom commands
 */
public class CommandsManager {
    /**
     * Repository of all custom commands
     */
    private static final Map<String, Command> commandsMap = new HashMap<>();

    /**
     * Checking the command name for emptiness and treating extra characters ('/' and '!') as prefixes
     *
     * @param command command class instance
     * @return processed and verified command name as string
     * @throws NullPointerException in case the command name is empty
     */
    private static String validateCommandName(Command command) throws NullPointerException {
        CommandName commandNameAnnotation = command.getClass().getAnnotation(CommandName.class);
        if (commandNameAnnotation == null || commandNameAnnotation.command().isEmpty()) {
            throw new NullPointerException("[!] There is no name specified for the custom command: " + command.getClass());
        }

        String commandName = commandNameAnnotation.command();

        if (commandName.startsWith("!") || commandName.startsWith("/")) {
            commandName = commandName.substring(1);
        }
        return commandName;
    }

    /**
     * Adding a command to the repository
     *
     * @param command chat command instance
     * @throws Exception in case an attempt is made to re-register a command (duplicate names) or missing annotations
     */
    public static void addCommand(Command command) throws Exception {
        if (!command.getClass().isAnnotationPresent(CommandName.class)) {
            throw new Exception("[!] The command must be annotated with @CommandName.");
        }

        if (!command.getClass().isAnnotationPresent(CommandAccessLevel.class)) {
            throw new Exception("[!] The command must be annotated with @UserAccessLevel.");
        }

        if (!command.getClass().isAnnotationPresent(CommandChatReturn.class)) {
            throw new Exception("[!] The command must be annotated with @CommandChatReturn.");
        }

        if (!command.getClass().isAnnotationPresent(CommandExecutionScope.class)) {
            throw new Exception("[!] The command must be annotated with @CommandExecutionScope.");
        }

        String commandName = validateCommandName(command).toLowerCase();

        if (commandsMap.containsKey(commandName)) {
            throw new Exception(String.format("The '%s' command is already registered in the system!", commandName));
        }

        System.out.printf("[#] Added new custom command: '%s'%n", commandName);

        commandsMap.put(commandName, command);
    }

    /**
     * Checks if the execution of a specified command is allowed in a given scope.
     *
     * @param chatCommand The name of the command to be checked.
     * @param scopeType   The scope (CHAT, CONSOLE, or BOTH) in which to check the command's allowance.
     * @return true if the command is allowed in the specified scope, false otherwise.
     */
    private static boolean isCommandAllowed(String chatCommand, CommandScope scopeType) {
        Command command = commandsMap.get(chatCommand);
        if (command == null) {
            return false;
        }

        CommandExecutionScope executionScope = command.getClass().getAnnotation(CommandExecutionScope.class);
        if (executionScope == null) {
            return false;
        }

        // Getting the value of the scope attribute of the CommandExecutionScope annotation
        CommandScope scope = executionScope.scope();

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
        String[] commandArgs = parseAndValidateCommand(chatCommand);
        if (commandArgs == null) {
            return null;
        }

        Command command = commandsMap.get(commandArgs[0].toLowerCase());
        if (command == null) {
            return null;
        }

        boolean isConsole = playerConnection == null;

        if (!isCommandAllowed(commandArgs[0], isConsole ? CommandScope.CONSOLE : CommandScope.CHAT)) {
            return "This command is not allowed here.";
        }

        if (!isConsole) {
            IsoPlayer player = PlayerUtils.getPlayerByUsername(playerConnection.username);
            if (player == null) {
                return "Could not check your access level! Please try later...";
            }

            CommandAccessLevel accessLevelAnnotation = command.getClass().getAnnotation(CommandAccessLevel.class);
            if (accessLevelAnnotation != null) {
                AccessLevel requiredAccessLevel = accessLevelAnnotation.accessLevel();
                AccessLevel userAccessLevel = AccessLevel.fromString(player.accessLevel.toLowerCase());
                if (requiredAccessLevel.getPriority() > userAccessLevel.getPriority()) {
                    return "You do not have permission to execute this command.";
                }
            }
        }

        String[] commandArgsToInvoke = Arrays.copyOfRange(commandArgs, 1, commandArgs.length);
        String playerName = playerConnection == null ? "Console" : playerConnection.username;
        System.out.printf("[#] Player '%s' called command '%s' with arguments: '%s'%n", playerName, commandArgs[0], Arrays.toString(commandArgsToInvoke));
        command.onInvoke(playerConnection, commandArgsToInvoke);

        CommandChatReturn chatReturnAnnotation = command.getClass().getAnnotation(CommandChatReturn.class);
        if (chatReturnAnnotation != null && chatReturnAnnotation.text() != null) {
            return chatReturnAnnotation.text();
        }

        return "";
    }

    /**
     * Parses the given chat command string and validates its structure.
     * Splits the chat command into arguments and removes any command prefixes
     * like '!' or '/' from the first argument, which is typically the command name.
     *
     * @param chatCommand The chat command string to be parsed and validated.
     * @return An array of strings, where the first element is the command name
     * (without prefixes) and the remaining elements are the arguments.
     * Returns null if the command is invalid, empty, or cannot be parsed.
     */
    private static String[] parseAndValidateCommand(String chatCommand) {
        if (chatCommand == null || chatCommand.isEmpty()) {
            return null;
        }

        String[] commandArgs = chatCommand.split("\\s+");
        if (commandArgs.length == 0 || commandArgs[0].isEmpty()) {
            return null;
        }

        String commandName = commandArgs[0];
        if (commandName.startsWith("!") || commandName.startsWith("/")) {
            commandName = commandName.substring(1);
        }

        if (commandName.isEmpty()) {
            return null;
        }

        commandArgs[0] = commandName.toLowerCase();

        return commandArgs;
    }
}