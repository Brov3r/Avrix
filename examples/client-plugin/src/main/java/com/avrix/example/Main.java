package com.avrix.example;

import com.avrix.events.EventManager;
import com.avrix.plugin.Metadata;
import com.avrix.plugin.Plugin;
import com.avrix.ui.NanoColor;
import com.avrix.ui.notify.Notify;
import com.avrix.ui.widgets.*;
import zombie.input.Mouse;

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
        loadDefaultConfig();

        EventManager.addListener(new HUDHandler());
        EventManager.addListener(new WidgetManagerInitHandler());

        System.out.println("[#] Config: " + getDefaultConfig().getString("test"));

        WindowWidget root = new WindowWidget("Hello window", 10, 150, 300, 400);
        root.setResizable(true);
        root.setBorderRadius(8);
        root.setDraggable(true);

        VerticalBoxWidget vb = new VerticalBoxWidget(10, root.getHeaderHeight() + 10, 0, 0, true);
        root.addChild(vb);

        HorizontalBoxWidget hbForButtons = new HorizontalBoxWidget(0, 0, 0, 0, true);
        vb.addChild(hbForButtons);

        ButtonWidget btn = new ButtonWidget("Add btn", 0, 0, 100, 32, 0, NanoColor.BABY_BLUE, () -> {
            ButtonWidget btn2 = new ButtonWidget("Click", 0, 0, 100, 32, 0, NanoColor.BABY_BLUE, () -> {
                PopupWidget pw = new PopupWidget(Mouse.getXA(), Mouse.getYA(), 100, 100);
                for (int i = 0; i < 10; i++) {
                    int index = i;
                    pw.addChild(new ButtonWidget("Hey" + i, 0, i * 50, 100, 40, 0, NanoColor.CORAL, () -> {
                        System.out.println("Popup click " + index);
                        Notify.info("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.", 10);
                        Notify.info("Info notify loooooooooooooooooooong meeeeeeeeeeeeesssssssssssaaaaaaaaaggge", 10);
                        Notify.info("Looooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooong", 10);
                        Notify.info("Info notify", 10);
                        Notify.success("Success notify", 12);
                        Notify.warn("Warn notify", 14);
                        Notify.error("Error notify", 16);
                        Notify.critical("Critical notify", 18);
                        Notify.hint("Hint notify", 20);
                    }));
                }
                pw.show();
            });
            btn2.setDrawBorder(false);
            hbForButtons.addChild(btn2);
        });

        ButtonWidget modalBtn = new ButtonWidget("Modal", 0, 0, 100, 32, 0, NanoColor.BABY_BLUE, () -> {
            ModalWidget mw = new ModalWidget(300, 100);
            mw.getContentPanel().addChild(new ButtonWidget("Close", 10, 10, 100, 30, 0, NanoColor.BABY_BLUE, mw::close));
            mw.show();
        });

        btn.setDrawBorder(false);
        modalBtn.setDrawBorder(false);
        hbForButtons.addChild(modalBtn);
        hbForButtons.addChild(btn);

        InputTextWidget input = new InputTextWidget(0, 0, 200, 32);
        input.setPlaceholder("Placeholder...");
        vb.addChild(input);

        InputTextWidget input2 = new InputTextWidget(0, 0, 200, 32);
        input2.setBackgroundColor(NanoColor.DARK_GRAY);
        input2.setBorderRadius(8);
        input2.setDrawBorder(false);
        input2.setPlaceholder("Secure text...");
        input2.setSecure(true);
        vb.addChild(input2);

        CheckboxWidget cb = new CheckboxWidget("Example checkbox", 10, 200, 150, 16, (checked) -> {
            System.out.println("CHECK - " + checked);
        });
        vb.addChild(cb);

        SliderWidget sw = new SliderWidget(0, 0, 200, (value) -> {
            System.out.println("Value - " + value);
        });
        vb.addChild(sw);

        ComboBoxWidget cbw = new ComboBoxWidget(0, 0, 200, 32);
        cbw.addValue("Example text value looooooooong");
        cbw.addValue("Hello world!");
        cbw.addValue("Hi");
        cbw.addValue("123321");
        cbw.addValue("####");
        vb.addChild(cbw);

        RadioButtonWidget rbw = new RadioButtonWidget(0, 0, 300, 300, false, (index, value) -> {
            System.out.println("Radio #" + index + " value: " + value);
        });
        rbw.addRadio("Option 1");
        rbw.addRadio("Option 2");
        rbw.addRadio("Option 3");
        rbw.resizeToContent();
        vb.addChild(rbw);

        HorizontalBoxWidget hb2 = new HorizontalBoxWidget(0, 0, 0, 0, true);
        ColorPickerWidget cp = new ColorPickerWidget(0, 0, 15, 15, NanoColor.BABY_BLUE);
        cp.setOnChangeColorAction((nanoColor -> System.out.println("New color: " + nanoColor.toHEX())));
        hb2.addChild(new LabelWidget("Color picker: ", "Montserrat-Regular", 0, 0, 100, 14, NanoColor.WHITE));
        hb2.addChild(cp);

        vb.addChild(hb2);


        TableWidget tw = new TableWidget(0, 0, vb.getWidth(), 200);
        tw.setOnDoubleSelectAction((map) -> {
            System.out.println("Double left click");
            map.forEach((key, value) -> {
                System.out.println("Key: " + key + ", Value: " + value);
            });
        });

        tw.setOnSelectAction((map) -> {
            System.out.println("Left click");
            map.forEach((key, value) -> {
                System.out.println("Key: " + key + ", Value: " + value);
            });
        });

        tw.setOnRightClickAction((map) -> {
            System.out.println("Right click");
            map.forEach((key, value) -> {
                System.out.println("Key: " + key + ", Value: " + value);
            });
        });

        tw.addColumn("ID");
        tw.addColumn("Name");
        tw.addColumn("Description");
        tw.addColumn("Version");

        tw.addRow("Looooooooooooooong ID", "Hello worldDDDDDDDDDDDDDDDDDDD!");
        tw.addRow("id<", "name>", "desc");
        tw.addRow("id<", "name>", null, "ver");
        for (int i = 0; i < 10; i++) {
            tw.addRow(String.valueOf(i), "Hello world $" + i);
        }

        vb.addChild(tw);
        root.addToScreen();
    }
}