package com.avrix.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains test cases for the YamlFile class.
 */
public class YamlFileTest {
    /**
     * The YamlFile instance used for testing.
     */
    private YamlFile yamlFile;

    /**
     * Test of saving and loading a YAML file
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void testSaveAndLoad() throws IOException {
        String filePath = "testSave.yml";
        YamlFile yamlFile = YamlFile.create(filePath);
        yamlFile.setString("key", "value");

        yamlFile.save();

        assertTrue(Files.exists(Paths.get(filePath)));

        YamlFile loadedYaml = YamlFile.load(filePath);
        assertEquals("value", loadedYaml.getString("key"));

        Files.deleteIfExists(Paths.get(filePath));
    }

    /**
     * Test to create YamlFile from path.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void testCreateFromFile() throws IOException {
        String filePath = "testCreate1.yml";
        YamlFile yamlFile = YamlFile.create(new File(filePath));
        yamlFile.setString("test", "Test String");

        assertNotNull(yamlFile);
        assertTrue(Files.exists(Paths.get(filePath)));

        assertFalse(yamlFile.isEmpty());
        Files.deleteIfExists(Paths.get(filePath));
    }

    /**
     * Test to create YamlFile from path.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void testCreateFromPath() throws IOException {
        String filePath = "testCreate2.yml";
        Path path = Paths.get(filePath);
        YamlFile yamlFile = YamlFile.create(path);
        yamlFile.setString("test", "Test String");

        assertNotNull(yamlFile);
        assertTrue(Files.exists(path));

        assertFalse(yamlFile.isEmpty());
        Files.deleteIfExists(Paths.get(filePath));
    }

    /**
     * Test to create YamlFile from path.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void testCreateFromString() throws IOException {
        String filePath = "testCreate3.yml";
        YamlFile yamlFile = YamlFile.create(filePath);
        yamlFile.setString("test", "Test String");

        assertNotNull(yamlFile);
        assertTrue(Files.exists(Paths.get(filePath)));

        assertFalse(yamlFile.isEmpty());
        Files.deleteIfExists(Paths.get(filePath));
    }

    /**
     * Sets up the test environment before each test case execution.
     *
     * @throws IOException        If an I/O error occurs.
     * @throws URISyntaxException If a URI syntax error occurs.
     */
    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        URL resourceUrl = YamlFileTest.class.getClassLoader().getResource("yaml/test.yml");
        if (resourceUrl == null) {
            throw new FileNotFoundException("test.yml not found in resources");
        }
        yamlFile = new YamlFile(resourceUrl);
    }

    /**
     * Tests the remove method of YamlFile.
     */
    @Test
    void testRemove() {
        assertTrue(yamlFile.contains("string"));
        yamlFile.remove("string");
        assertFalse(yamlFile.contains("string"));
    }

    /**
     * Tests the getString method of YamlFile.
     */
    @Test
    void testStringMethods() {
        assertEquals("This is a string", yamlFile.getString("string"));
    }

    /**
     * Tests the getInt method of YamlFile.
     */
    @Test
    void testIntMethods() {
        assertEquals(123, yamlFile.getInt("integer"));
        assertEquals(-456, yamlFile.getInt("negativeInteger"));
    }

    /**
     * Tests the getLong method of YamlFile.
     */
    @Test
    void testLongMethods() {
        assertEquals(9223372036854775807L, yamlFile.getLong("long"));
        assertEquals(-9223372036854775807L, yamlFile.getLong("negativeLong"));
    }

    /**
     * Tests the getDouble method of YamlFile.
     */
    @Test
    void testDoubleMethods() {
        assertEquals(1.7976931348623157E308, yamlFile.getDouble("double"));
        assertEquals(-1.7976931348623157E308, yamlFile.getDouble("negativeDouble"));
    }

    /**
     * Tests the getShort method of YamlFile.
     */
    @Test
    void testShortMethods() {
        assertEquals(32767, yamlFile.getShort("short"));
        assertEquals(-32767, yamlFile.getShort("negativeShort"));
    }

    /**
     * Tests the getByte method of YamlFile.
     */
    @Test
    void testByteMethods() {
        assertEquals(127, yamlFile.getByte("byte"));
        assertEquals(-127, yamlFile.getByte("negativeByte"));
    }

    /**
     * Tests the getBoolean method of YamlFile.
     */
    @Test
    void testBooleanMethods() {
        assertTrue(yamlFile.getBoolean("boolean_true"));
        assertFalse(yamlFile.getBoolean("boolean_false"));
    }

    /**
     * Tests the getStringList method of YamlFile.
     */
    @Test
    void testListMethods() {
        List<String> list = yamlFile.getStringList("list");
        assertTrue(list.contains("item1"));
        assertTrue(list.contains("item2"));
        assertTrue(list.contains("item3"));
    }

    /**
     * Tests the getValue method of YamlFile for dictionary entries.
     */
    @Test
    void testDictionaryMethods() {
        assertEquals("value1", yamlFile.getValue("dictionary.key1"));
        assertEquals("value2", yamlFile.getValue("dictionary.key2"));
    }

    /**
     * Tests the getValue method of YamlFile for mixed types.
     */
    @Test
    void testMixedMethods() {
        assertEquals("String in a list", yamlFile.getValue("mixed.string"));
        assertEquals(456, yamlFile.getInt("mixed.integer"));
        assertTrue(yamlFile.getBoolean("mixed.boolean"));
        List<String> list = yamlFile.getStringList("mixed.list");
        assertTrue(list.contains("subitem1"));
        assertTrue(list.contains("subitem2"));
    }

    /**
     * Tests the getList method of YamlFile for nested entries.
     */
    @Test
    void testNestedMethods() {
        assertEquals("Nested string", yamlFile.getList("nested.key1").get(0));
        assertEquals(123, yamlFile.getInt("nested.key2"));
    }

    /**
     * Tests the getIntegerList method of YamlFile.
     */
    @Test
    void testIntegerListMethods() {
        List<Integer> integers = yamlFile.getIntegerList("integers_list");
        assertTrue(integers.contains(1));
        assertTrue(integers.contains(-2));
        assertTrue(integers.contains(3));
    }

    /**
     * Tests the getLongList method of YamlFile.
     */
    @Test
    void testLongListMethods() {
        List<Long> longs = yamlFile.getLongList("longs_list");
        assertTrue(longs.contains(9223372036854775807L));
        assertTrue(longs.contains(-9223372036854775807L));
        assertTrue(longs.contains(9223372036854775806L));
    }

    /**
     * Tests the getDoubleList method of YamlFile.
     */
    @Test
    void testDoubleListMethods() {
        List<Double> doubles = yamlFile.getDoubleList("doubles_list");
        assertTrue(doubles.contains(1.7976931348623157E308));
        assertTrue(doubles.contains(-1.7976931348623157E308));
        assertTrue(doubles.contains(1.7976931348623156E308));
    }

    /**
     * Tests the getShortList method of YamlFile.
     */
    @Test
    void testShortListMethods() {
        List<Short> shorts = yamlFile.getShortList("shorts_list");
        assertTrue(shorts.contains((short) 32767));
        assertTrue(shorts.contains((short) -32767));
        assertTrue(shorts.contains((short) 32766));
    }

    /**
     * Tests the getByteList method of YamlFile.
     */
    @Test
    void testByteListMethods() {
        List<Byte> bytes = yamlFile.getByteList("bytes_list");
        assertTrue(bytes.contains((byte) 127));
        assertTrue(bytes.contains((byte) -127));
        assertTrue(bytes.contains((byte) 126));
    }

    /**
     * Tests the getChar method of YamlFile.
     */
    @Test
    void testCharMethods() {
        assertEquals('A', yamlFile.getChar("char"));
    }

    /**
     * Tests the getCharacterList method of YamlFile.
     */
    @Test
    void testCharacterListMethods() {
        List<Character> chars = yamlFile.getCharacterList("chars_list");
        assertTrue(chars.contains('A'));
        assertTrue(chars.contains('B'));
        assertTrue(chars.contains('C'));
    }

    /**
     * Tests the getBooleanList method of YamlFile.
     */
    @Test
    void testBooleanListMethods() {
        List<Boolean> booleans = yamlFile.getBooleanList("booleans_list");
        assertTrue(booleans.contains(true));
        assertTrue(booleans.contains(false));
        assertTrue(booleans.contains(true));
    }

    /**
     * Tests the setString method of YamlFile.
     */
    @Test
    void testSetStringMethod() {
        yamlFile.setString("new_string", "New string value");
        assertEquals("New string value", yamlFile.getString("new_string"));
    }

    /**
     * Tests the setInt method of YamlFile.
     */
    @Test
    void testSetIntMethod() {
        yamlFile.setInt("new_integer", 789);
        assertEquals(789, yamlFile.getInt("new_integer"));
    }

    /**
     * Tests the setLong method of YamlFile.
     */
    @Test
    void testSetLongMethod() {
        yamlFile.setLong("new_long", 123456789L);
        assertEquals(123456789L, yamlFile.getLong("new_long"));
    }

    /**
     * Tests the setDouble method of YamlFile.
     */
    @Test
    void testSetDoubleMethod() {
        yamlFile.setDouble("new_double", 3.14);
        assertEquals(3.14, yamlFile.getDouble("new_double"));
    }

    /**
     * Tests the setShort method of YamlFile.
     */
    @Test
    void testSetShortMethod() {
        yamlFile.setShort("new_short", (short) 123);
        assertEquals((short) 123, yamlFile.getShort("new_short"));
    }

    /**
     * Tests the setByte method of YamlFile.
     */
    @Test
    void testSetByteMethod() {
        yamlFile.setByte("new_byte", (byte) 100);
        assertEquals((byte) 100, yamlFile.getByte("new_byte"));
    }

    /**
     * Tests the setBoolean method of YamlFile.
     */
    @Test
    void testSetBooleanMethod() {
        yamlFile.setBoolean("new_boolean", true);
        assertTrue(yamlFile.getBoolean("new_boolean"));
    }

    /**
     * Tests the setStringList method of YamlFile.
     */
    @Test
    void testSetStringListMethod() {
        List<String> newList = List.of("new_item1", "new_item2", "new_item3");
        yamlFile.setStringList("new_list", newList);
        assertEquals(newList, yamlFile.getStringList("new_list"));
    }

    /**
     * Tests the setValue method of YamlFile for dictionary entries.
     */
    @Test
    void testSetDictionaryMethod() {
        yamlFile.setValue("new_dictionary.new_key", "New value");
        assertEquals("New value", yamlFile.getValue("new_dictionary.new_key"));
    }

    /**
     * Tests the methods of YamlFile for mixed types.
     */
    @Test
    void testSetMixedMethod() {
        yamlFile.setString("mixed.new_string", "New mixed string");
        yamlFile.setInt("mixed.new_integer", 789);
        yamlFile.setBoolean("mixed.new_boolean", true);
        List<String> newList = List.of("new_subitem1", "new_subitem2");
        yamlFile.setStringList("mixed.new_list", newList);

        assertEquals("New mixed string", yamlFile.getString("mixed.new_string"));
        assertEquals(789, yamlFile.getInt("mixed.new_integer"));
        assertTrue(yamlFile.getBoolean("mixed.new_boolean"));
        assertEquals(newList, yamlFile.getStringList("mixed.new_list"));
    }

    /**
     * Tests the setString and setInt method of YamlFile for nested entries.
     */
    @Test
    void testSetNestedMethod() {
        yamlFile.setString("nested.new_key", "New nested string");
        yamlFile.setInt("nested.new_integer", 789);

        assertEquals("New nested string", yamlFile.getString("nested.new_key"));
        assertEquals(789, yamlFile.getInt("nested.new_integer"));
    }

    /**
     * Tests the setIntegerList method of YamlFile.
     */
    @Test
    void testSetIntegerListMethod() {
        List<Integer> newIntegers = List.of(10, 20, 30);
        yamlFile.setIntegerList("new_integers_list", newIntegers);
        assertEquals(newIntegers, yamlFile.getIntegerList("new_integers_list"));
    }

    /**
     * Tests the setLongList method of YamlFile.
     */
    @Test
    void testSetLongListMethod() {
        List<Long> newLongs = List.of(123456789L, 987654321L, 555555555L);
        yamlFile.setLongList("new_longs_list", newLongs);
        assertEquals(newLongs, yamlFile.getLongList("new_longs_list"));
    }

    /**
     * Tests the setDoubleList method of YamlFile.
     */
    @Test
    void testSetDoubleListMethod() {
        List<Double> newDoubles = List.of(1.1, 2.2, 3.3);
        yamlFile.setDoubleList("new_doubles_list", newDoubles);
        assertEquals(newDoubles, yamlFile.getDoubleList("new_doubles_list"));
    }

    /**
     * Tests the setShortList method of YamlFile.
     */
    @Test
    void testSetShortListMethod() {
        List<Short> newShorts = List.of((short) 100, (short) 200, (short) 300);
        yamlFile.setShortList("new_shorts_list", newShorts);
        assertEquals(newShorts, yamlFile.getShortList("new_shorts_list"));
    }

    /**
     * Tests the setByteList method of YamlFile.
     */
    @Test
    void testSetByteListMethod() {
        List<Byte> newBytes = List.of((byte) 50, (byte) 100, (byte) 150);
        yamlFile.setByteList("new_bytes_list", newBytes);
        assertEquals(newBytes, yamlFile.getByteList("new_bytes_list"));
    }

    /**
     * Tests the setChar method of YamlFile.
     */
    @Test
    void testSetCharMethod() {
        yamlFile.setChar("new_char", 'X');
        assertEquals('X', yamlFile.getChar("new_char"));
    }

    /**
     * Tests the setCharacterList method of YamlFile.
     */
    @Test
    void testSetCharacterListMethod() {
        List<Character> newChars = List.of('A', 'B', 'C');
        yamlFile.setCharacterList("new_chars_list", newChars);
        assertEquals(newChars, yamlFile.getCharacterList("new_chars_list"));
    }

    /**
     * Tests the setBooleanList method of YamlFile.
     */
    @Test
    void testSetBooleanListMethod() {
        List<Boolean> newBooleans = List.of(true, false, true);
        yamlFile.setBooleanList("new_booleans_list", newBooleans);
        assertEquals(newBooleans, yamlFile.getBooleanList("new_booleans_list"));
    }

    /**
     * Tests the merge method of YamlFile.
     */
    @Test
    void testMerge() {
        try {
            URL resourceUrl = YamlFileTest.class.getClassLoader().getResource("yaml/test2.yml");
            if (resourceUrl == null) {
                throw new FileNotFoundException("test2.yml not found in resources");
            }
            YamlFile yamlFile2 = new YamlFile(resourceUrl);

            yamlFile.merge(yamlFile2.getAll());

            assertEquals("value3", yamlFile.getString("key3"));
            assertEquals("subvalue4", yamlFile.getString("key4.subkey"));
        } catch (IOException | URISyntaxException e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }
}