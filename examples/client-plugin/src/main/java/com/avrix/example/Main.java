package com.avrix.example;

import com.avrix.events.EventManager;
import com.avrix.plugin.Metadata;
import com.avrix.plugin.Plugin;
import com.avrix.ui.NVGColor;
import com.avrix.ui.widgets.*;
import zombie.input.Mouse;

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

        WindowWidget root = new WindowWidget("Hello window", 10, 10, 300, 500);
        root.setResizable(true);
        root.setBorderRadius(8);
        root.setDraggable(true);

        PopupWidget pw = new PopupWidget(0, 0, 100, 100);
        for (int i = 0; i < 10; i++) {
            int index = i;
            pw.addChild(new ButtonWidget("Hey" + i, 0, i * 50, 100, 40, 0, NVGColor.CORAL, () -> {
                System.out.println("Popup click " + index);
            }));
        }

        ModalWidget mw = new ModalWidget(300, 100);
        mw.getContentPanel().addChild(new ButtonWidget("Close", 10, 10, 100, 30, 0, NVGColor.BABY_BLUE, mw::close));

        ButtonWidget btn = new ButtonWidget("Add btn", 10, 40, 100, 32, 0, NVGColor.BABY_BLUE, () -> {
            ButtonWidget btn2 = new ButtonWidget("Click - " + buttonCount, 120 + 115 * buttonCount, 40, 100, 32, 0, NVGColor.BABY_BLUE, () -> {
                pw.setX(Mouse.getXA());
                pw.setY(Mouse.getYA());
                pw.show();
            });
            btn2.setDrawBorder(false);
            root.addChild(btn2);

            buttonCount++;
        });

        ButtonWidget modalBtn = new ButtonWidget("Modal", 115, 40, 100, 32, 0, NVGColor.BABY_BLUE, () -> {
            mw.show();
        });

        btn.setDrawBorder(false);
        modalBtn.setDrawBorder(false);
        root.addChild(modalBtn);
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

        CheckboxWidget cb = new CheckboxWidget("Example checkbox", 10, 200, 150, 16, (checked) -> {
            System.out.println("CHECK - " + checked);
        });
        root.addChild(cb);

        SliderWidget sw = new SliderWidget(10, 250, 200, (value) -> {
            System.out.println("Value - " + value);
        });
        root.addChild(sw);

        root.addToScreen();
    }
}