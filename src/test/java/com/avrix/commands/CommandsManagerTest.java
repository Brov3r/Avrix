package com.avrix.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class CommandsManagerTest {
    @Test
    public void testGetCommandArgs() {
        // Тест с обычными аргументами без кавычек
        String[] result1 = CommandsManager.getCommandArgs("!test arg1 arg2 arg3");
        String[] expected1 = {"test", "arg1", "arg2", "arg3"};
        assertArrayEquals(expected1, result1);

        // Тест с аргументами в двойных кавычках
        String[] result2 = CommandsManager.getCommandArgs("!test \"arg1 with spaces\" \"arg2\"");
        String[] expected2 = {"test", "arg1 with spaces", "arg2"};
        assertArrayEquals(expected2, result2);

        // Тест с аргументами в одинарных кавычках
        String[] result3 = CommandsManager.getCommandArgs("!test 'arg1' 'arg2'");
        String[] expected3 = {"test", "'arg1'", "'arg2'"};
        assertArrayEquals(expected3, result3);

        // Тест с комбинацией аргументов в разных типах кавычек
        String[] result4 = CommandsManager.getCommandArgs("!test \"arg1 'with' quotes\" 'arg2 \"with\" quotes'");
        String[] expected4 = {"test", "arg1 'with' quotes", "'arg2", "with", "quotes'"};
        assertArrayEquals(expected4, result4);

        // Тест с пустой строкой
        String[] result5 = CommandsManager.getCommandArgs("");
        assertArrayEquals(null, result5);

        // Тест с пустыми аргументами
        String[] result6 = CommandsManager.getCommandArgs("!test ''");
        String[] expected6 = {"test", "''"};
        assertArrayEquals(expected6, result6);

        // Тест с аргументами, начинающимися с кавычек
        String[] result7 = CommandsManager.getCommandArgs("!test \"arg1 arg2");
        String[] expected7 = {"test", "\"arg1", "arg2"};
        assertArrayEquals(expected7, result7);

        // Тест с аргументами, заканчивающимися кавычками
        String[] result8 = CommandsManager.getCommandArgs("/test arg1\"");
        String[] expected8 = {"test", "arg1\""};
        assertArrayEquals(expected8, result8);

        String[] result9 = CommandsManager.getCommandArgs("/test \"arg1 \"arg2");
        String[] expected9 = {"test", "arg1", "arg2"};
        assertArrayEquals(expected9, result9);

        String[] result10 = CommandsManager.getCommandArgs("!test \"\"arg1");
        String[] expected10 = {"test", "arg1"};
        assertArrayEquals(expected10, result10);
    }
}
