package com.avrix.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * This class contains unit tests for the {@link CommandsManager} class.
 */
public class CommandsManagerTest {
    /**
     * Test the {@link CommandsManager#getCommandArgs(String)} method with normal arguments without quotes.
     */
    @Test
    public void testGetCommandArgs() {
        // Test with normal arguments without quotes
        String[] result1 = CommandsManager.getCommandArgs("!test arg1 arg2 arg3");
        String[] expected1 = {"test", "arg1", "arg2", "arg3"};
        assertArrayEquals(expected1, result1);

        // Test with arguments in double quotes
        String[] result2 = CommandsManager.getCommandArgs("!test \"arg1 with spaces\" \"arg2\"");
        String[] expected2 = {"test", "arg1 with spaces", "arg2"};
        assertArrayEquals(expected2, result2);

        // Test with arguments in single quotes
        String[] result3 = CommandsManager.getCommandArgs("!test 'arg1' 'arg2'");
        String[] expected3 = {"test", "'arg1'", "'arg2'"};
        assertArrayEquals(expected3, result3);

        // Test with a combination of arguments in different types of quotes
        String[] result4 = CommandsManager.getCommandArgs("!test \"arg1 'with' quotes\" 'arg2 \"with\" quotes'");
        String[] expected4 = {"test", "arg1 'with' quotes", "'arg2", "with", "quotes'"};
        assertArrayEquals(expected4, result4);

        // Test with an empty string
        String[] result5 = CommandsManager.getCommandArgs("");
        assertArrayEquals(null, result5);

        // Test with empty arguments
        String[] result6 = CommandsManager.getCommandArgs("!test ''");
        String[] expected6 = {"test", "''"};
        assertArrayEquals(expected6, result6);

        // Test with arguments starting with quotes
        String[] result7 = CommandsManager.getCommandArgs("!test \"arg1 arg2");
        String[] expected7 = {"test", "\"arg1", "arg2"};
        assertArrayEquals(expected7, result7);

        // Test with arguments ending with quotes
        String[] result8 = CommandsManager.getCommandArgs("/test arg1\"");
        String[] expected8 = {"test", "arg1\""};
        assertArrayEquals(expected8, result8);

        // Test with arguments containing quotes within quotes
        String[] result9 = CommandsManager.getCommandArgs("/test \"arg1 \"arg2");
        String[] expected9 = {"test", "arg1", "arg2"};
        assertArrayEquals(expected9, result9);

        // Test with arguments containing quotes only
        String[] result10 = CommandsManager.getCommandArgs("!test \"\"arg1");
        String[] expected10 = {"test", "arg1"};
        assertArrayEquals(expected10, result10);
    }

    /**
     * Test the {@link CommandsManager#getCommandArgs(String, String)} method with custom prefixes.
     */
    @Test
    public void testGetCommandArgsWithCustomPrefix() {
        // Test with normal arguments without quotes and custom prefix
        String[] result1 = CommandsManager.getCommandArgs("#", "#test arg1 arg2 arg3");
        String[] expected1 = {"test", "arg1", "arg2", "arg3"};
        assertArrayEquals(expected1, result1);

        // Test with arguments in double quotes and custom prefix
        String[] result2 = CommandsManager.getCommandArgs("$", "$test \"arg1 with spaces\" \"arg2\"");
        String[] expected2 = {"test", "arg1 with spaces", "arg2"};
        assertArrayEquals(expected2, result2);
    }
}