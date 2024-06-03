package com.avrix.plugin;

import com.avrix.enums.PluginEnvironment;
import com.avrix.utils.YamlFile;
import com.avrix.utils.YamlFileTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Metadata} and {@link Metadata.MetadataBuilder}.
 */
public class MetadataTest {
    /**
     * Test of creating metadata from a YAML file
     */
    @Test
    void testYamlInvalidMetadataFromJar() throws IOException, URISyntaxException {
        URL resourceUrl = YamlFileTest.class.getClassLoader().getResource("yaml/testYamlJar.jar");
        if (resourceUrl == null) {
            throw new FileNotFoundException("test.yml not found in resources");
        }
        File jarFile = new File(resourceUrl.toURI());

        Metadata metadata = Metadata.createFromJar(jarFile, "test.yml");

        assertNull(metadata);
    }

    /**
     * Test of creating metadata from a YAML file
     */
    @Test
    void testYamlValidMetadataFromJar() throws IOException, URISyntaxException {
        URL resourceUrl = YamlFileTest.class.getClassLoader().getResource("yaml/testYamlJarMetadata.jar");
        if (resourceUrl == null) {
            throw new FileNotFoundException("test.yml not found in resources");
        }
        File jarFile = new File(resourceUrl.toURI());

        Metadata metadata = Metadata.createFromJar(jarFile, "test.yml");

        assertNotNull(metadata);
        assertEquals("Test metadata", metadata.getName());
        assertEquals("test_meta", metadata.getId());
        assertEquals("Test metadata", metadata.getDescription());
        assertEquals("Test", metadata.getAuthor());
        assertEquals("0.0.0", metadata.getVersion());
        assertEquals(PluginEnvironment.CLIENT, metadata.getEnvironment());
        assertEquals("MIT", metadata.getLicense());
        assertEquals("https://test.com", metadata.getContacts());
        assertEquals(Arrays.asList("test.class", "test.test.class"), metadata.getEntryPoints());
        assertEquals(Arrays.asList("testPatch.class", "test.testPatch.class"), metadata.getPatchList());
        assertEquals(Map.of("test-core", "1.0.0", "test", "0.1", "test.2", "2.1.3"), metadata.getDependencies());
    }

    /**
     * Test of creating metadata from a YAML file
     */
    @Test
    void testYamlMetadata() throws IOException, URISyntaxException {
        URL resourceUrl = YamlFileTest.class.getClassLoader().getResource("yaml/metadataTest.yml");
        if (resourceUrl == null) {
            throw new FileNotFoundException("metadataTest.yml not found in resources");
        }
        YamlFile yamlFile = new YamlFile(resourceUrl);

        Metadata.MetadataBuilder metadataBuilder = new Metadata.MetadataBuilder()
                .name(yamlFile.getString("name"))
                .id(yamlFile.getString("id"))
                .environment(yamlFile.getString("environment"))
                .description(yamlFile.getString("description"))
                .author(yamlFile.getString("author"))
                .version(yamlFile.getString("version"))
                .license(yamlFile.getString("license"))
                .contacts(yamlFile.getString("contacts"))
                .entryPointsList(yamlFile.getStringList("entrypoints"))
                .patchList(yamlFile.getStringList("patches"))
                .dependencies(yamlFile.getStringMap("dependencies"));

        Metadata metadata = metadataBuilder.build();

        assertNotNull(metadata);
        assertEquals("Test metadata", metadata.getName());
        assertEquals("test_meta", metadata.getId());
        assertEquals("Test metadata", metadata.getDescription());
        assertEquals("Test", metadata.getAuthor());
        assertEquals("0.0.0", metadata.getVersion());
        assertEquals(PluginEnvironment.CLIENT, metadata.getEnvironment());
        assertEquals("MIT", metadata.getLicense());
        assertEquals("https://test.com", metadata.getContacts());
        assertEquals(Arrays.asList("test.class", "test.test.class"), metadata.getEntryPoints());
        assertEquals(Arrays.asList("testPatch.class", "test.testPatch.class"), metadata.getPatchList());
        assertEquals(Map.of("test-core", "1.0.0", "test", "0.1", "test.2", "2.1.3"), metadata.getDependencies());
    }

    /**
     * Test case to verify that a {@link Metadata} object is correctly built with all fields specified.
     */
    @Test
    public void testBuilderWithAllFields() {
        List<String> entryPoints = Arrays.asList("entry1", "entry2");
        List<String> patches = Arrays.asList("patch1", "patch2");
        Map<String, String> dependencies = new HashMap<>();
        dependencies.put("dependency1", "1.0");
        dependencies.put("dependency2", "2.0");

        Metadata metadata = new Metadata.MetadataBuilder()
                .name("Test Plugin")
                .id("plugin123")
                .description("This is a test plugin")
                .author("Author Name")
                .version("1.0.0")
                .license("MIT")
                .contacts("author@example.com")
                .entryPointsList(entryPoints)
                .environment("client")
                .patchList(patches)
                .dependencies(dependencies)
                .build();

        assertEquals("Test Plugin", metadata.getName());
        assertEquals("plugin123", metadata.getId());
        assertEquals("This is a test plugin", metadata.getDescription());
        assertEquals("Author Name", metadata.getAuthor());
        assertEquals("1.0.0", metadata.getVersion());
        assertEquals("MIT", metadata.getLicense());
        assertEquals(PluginEnvironment.CLIENT, metadata.getEnvironment());
        assertEquals("author@example.com", metadata.getContacts());
        assertEquals(entryPoints, metadata.getEntryPoints());
        assertEquals(patches, metadata.getPatchList());
        assertEquals(dependencies, metadata.getDependencies());
    }

    /**
     * Test case to verify that building a {@link Metadata} object without specifying required fields throws a {@link NullPointerException}.
     */
    @Test
    public void testBuilderWithMissingFields() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new Metadata.MetadataBuilder()
                    .name("Test Plugin")
                    .id("plugin123")
                    .description("This is a test plugin")
                    .author("Author Name")
                    .version("1.0.0")
                    .build();
        });

        assertTrue(exception.getMessage().contains("[!] The required field 'license' is not specified in the metadata!"));
    }

    /**
     * Test case to verify that a {@link Metadata} object is correctly built with only required fields specified.
     */
    @Test
    public void testBuilderWithOnlyRequiredFields() {
        List<String> entryPoints = Arrays.asList("entry1", "entry2");

        Metadata metadata = new Metadata.MetadataBuilder()
                .name("Test Plugin")
                .id("plugin123")
                .author("Author Name")
                .version("1.0.0")
                .license("MIT")
                .environment("server")
                .entryPointsList(entryPoints)
                .build();

        assertEquals("Test Plugin", metadata.getName());
        assertEquals("plugin123", metadata.getId());
        assertEquals("Author Name", metadata.getAuthor());
        assertEquals(PluginEnvironment.SERVER, metadata.getEnvironment());
        assertEquals("1.0.0", metadata.getVersion());
        assertEquals("MIT", metadata.getLicense());
        assertEquals(entryPoints, metadata.getEntryPoints());
    }
}