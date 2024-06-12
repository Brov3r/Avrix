package com.avrix.example;

import com.avrix.commands.CommandsManager;
import com.avrix.events.EventManager;
import com.avrix.example.services.Example;
import com.avrix.example.services.ExampleService;
import com.avrix.plugin.Metadata;
import com.avrix.plugin.Plugin;
import com.avrix.plugin.ServiceManager;
import com.avrix.utils.YamlFile;

/**
 * Main entry point of the example plugin
 */
public class Main extends Plugin {
    /**
     * Constructs a new {@link Plugin} with the specified metadata.
     * Metadata is transferred when the plugin is loaded into the game context.
     *
     * @param metadata The {@link Metadata} associated with this plugin.
     */
    public Main(Metadata metadata) {
        super(metadata);
    }

    /**
     * Called when the plugin is initialized.
     * <p>
     * Implementing classes should override this method to provide the initialization logic.
     */
    @Override
    public void onInitialize() {
        ServiceManager.register(Example.class, new ExampleService());

        loadDefaultConfig();

        EventManager.addListener(new OnServerInitHandler());

        CommandsManager.addCommand(new TestCommand());

        YamlFile test = loadConfig("test/test.yml");
        YamlFile test2 = loadConfig("test/test2.yml"); // The file will be created

        System.out.println("[#] Hello world from " + getMetadata().getName());
        System.out.println("[$] Test.yml: " + test.getString("test"));
        System.out.println("[$] config.yml: " + getDefaultConfig().getString("config"));
        System.out.println("[$] config.yml new key: " + getDefaultConfig().getString("configTest"));

        getDefaultConfig().setString("configTest", "Hello!");
        getDefaultConfig().save();

        System.out.println("[$] config.yml new key: " + getDefaultConfig().getString("configTest"));
    }
}