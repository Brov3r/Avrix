package com.avrix.example;

import com.avrix.events.EventManager;
import com.avrix.plugin.Metadata;
import com.avrix.plugin.Plugin;
import com.avrix.ui.NVGColor;
import com.avrix.ui.widgets.ButtonWidget;
import com.avrix.ui.widgets.InputTextWidget;
import com.avrix.ui.widgets.WindowWidget;

/**
 * Main entry point of the example plugin
 */
public class Main extends Plugin {
    public static int buttonCount = 1;

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
        loadDefaultConfig();

        EventManager.addListener(new HUDHandler(this));
        EventManager.addListener(new WidgetManagerInitHandler());

        System.out.println("[#] Config: " + getDefaultConfig().getString("test"));

        WindowWidget root = new WindowWidget("Hello window", 10, 300, 300, 200);
        root.setBorderRadius(8);
        root.setDraggable(true);

        ButtonWidget btn = new ButtonWidget("Click", 10, 40, 100, 32, 0, NVGColor.BABY_BLUE, () -> {
            ButtonWidget btn2 = new ButtonWidget("Click - " + buttonCount, 10 + 115 * buttonCount, 40, 100, 32, 0, NVGColor.BABY_BLUE, () -> {
                System.out.println("[#] Click! #" + buttonCount);
            });
            btn2.setDrawBorder(false);
            root.addChild(btn2);

            buttonCount++;
        });
        btn.setDrawBorder(false);
        root.addChild(btn);

        InputTextWidget input = new InputTextWidget(10, 100, 200, 32);
        input.setPlaceholder("Placeholder...");
        root.addChild(input);

        InputTextWidget input2 = new InputTextWidget(10, 150, 200, 32);
        input2.setBackgroundColor(NVGColor.DARK_GRAY);
        input2.setBorderRadius(8);
        input2.setDrawBorder(false);
        input2.setPlaceholder("Placeholder text...");
        root.addChild(input2);

        root.addToScreen();
    }
}