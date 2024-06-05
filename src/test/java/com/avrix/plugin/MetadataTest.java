package com.avrix.plugin;

import com.avrix.utils.YamlFile;
import com.avrix.utils.YamlFileTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Metadata} and {@link Metadata.MetadataBuilder}.
 */
public class MetadataTest {
    private List<Metadata> metadataList;

    /**
     * Sets up the test environment by initializing a list of Metadata objects with dependencies.
     */
    @BeforeEach
    public void setUp() {
        metadataList = new ArrayList<>();

        Metadata p1 = new Metadata.MetadataBuilder()
                .id("P1")
                .name("Plugin 1")
                .version("1.0.0")
                .author("Author")
                .license("MIT")
                .environment("client")
                .entryPointsList(new ArrayList<>())
                .dependencies(new HashMap<>())
                .build();

        Metadata p2 = new Metadata.MetadataBuilder()
                .id("P2")
                .name("Plugin 2")
                .version("1.0.0")
                .author("Author")
                .license("MIT")
                .environment("client")
                .entryPointsList(new ArrayList<>())
                .dependencies(new HashMap<>())
                .build();

        Map<String, String> p3Deps = new HashMap<>();
        p3Deps.put("P4", "1.0.0");
        Metadata p3 = new Metadata.MetadataBuilder()
                .id("P3")
                .name("Plugin 3")
                .version("1.0.0")
                .author("Author")
                .license("MIT")
                .environment("client")
                .entryPointsList(new ArrayList<>())
                .dependencies(p3Deps)
                .build();

        Map<String, String> p4Deps = new HashMap<>();
        p4Deps.put("P2", "1.0.0");
        Metadata p4 = new Metadata.MetadataBuilder()
                .id("P4")
                .name("Plugin 4")
                .version("1.0.0")
                .author("Author")
                .license("MIT")
                .environment("client")
                .entryPointsList(new ArrayList<>())
                .dependencies(p4Deps)
                .build();

        metadataList.add(p3);
        metadataList.add(p4);
        metadataList.add(p1);
        metadataList.add(p2);
    }

    /**
     * Tests the sortMetadata method to ensure it correctly sorts metadata based on dependencies.
     */
    @Test
    public void testSortMetadata() {
        List<Metadata> sortedMetadata = Metadata.sortMetadata(metadataList);

        assertNotNull(sortedMetadata);
        assertEquals(4, sortedMetadata.size());
        assertEquals("P2", sortedMetadata.get(0).getId());
        assertEquals("P4", sortedMetadata.get(1).getId());
        assertEquals("P3", sortedMetadata.get(2).getId());
        assertEquals("P1", sortedMetadata.get(3).getId());
    }

    /**
     * Tests the sortMetadata method to ensure it detects and handles cycles in dependencies.
     */
    @Test
    public void testSortMetadataWithCycle() {
        Map<String, String> p2Deps = new HashMap<>();
        p2Deps.put("P4", "1.0.0");
        Metadata p2WithCycle = new Metadata.MetadataBuilder()
                .id("P2")
                .name("Plugin 2")
                .version("1.0.0")
                .author("Author")
                .license("MIT")
                .environment("client")
                .entryPointsList(new ArrayList<>())
                .dependencies(p2Deps)
                .build();

        List<Metadata> metadataListWithCycle = new ArrayList<>(metadataList);
        metadataListWithCycle.set(0, p2WithCycle);

        assertNotNull(metadataListWithCycle);
        assertThrows(IllegalStateException.class, () -> {
            Metadata.sortMetadata(metadataListWithCycle);
        });
    }

    /**
     * Tests the sortMetadata method to ensure it detects and handles missing dependencies.
     */
    @Test
    public void testSortMetadataWithMissingDependency() {
        Map<String, String> p5Deps = new HashMap<>();
        p5Deps.put("P8", "1.0.0");
        Metadata p5WithMissingDependency = new Metadata.MetadataBuilder()
                .id("P4")
                .name("Plugin 4")
                .version("1.0.0")
                .author("Author")
                .license("MIT")
                .environment("client")
                .entryPointsList(new ArrayList<>())
                .dependencies(p5Deps)
                .build();

        List<Metadata> metadataListWithMissingDependency = new ArrayList<>(metadataList);
        metadataListWithMissingDependency.add(p5WithMissingDependency);

        assertEquals(5, metadataListWithMissingDependency.size());
        assertNotNull(metadataListWithMissingDependency);
        assertThrows(IllegalArgumentException.class, () -> {
            Metadata.sortMetadata(metadataListWithMissingDependency);
        });
    }

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
        assertEquals(jarFile, metadata.getPluginFile());
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
        assertNull(metadata.getPluginFile());
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
        assertNull(metadata.getPluginFile());
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
        assertEquals("1.0.0", metadata.getVersion());
        assertEquals("MIT", metadata.getLicense());
        assertEquals(entryPoints, metadata.getEntryPoints());
    }
}