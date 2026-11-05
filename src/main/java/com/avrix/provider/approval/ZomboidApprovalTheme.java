package com.avrix.provider.approval;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Small, dependency-free dark theme used by the approval window.
 */
final class ZomboidApprovalTheme {

    static final Color WINDOW = new Color(0x0F1115);
    static final Color SURFACE = new Color(0x171A21);
    static final Color SURFACE_ALT = new Color(0x1D212A);
    static final Color HEADER = new Color(0x222733);
    static final Color BORDER = new Color(0x303746);
    static final Color TEXT = new Color(0xE8EBF0);
    static final Color MUTED = new Color(0x9AA3B2);
    static final Color ACCENT = new Color(0x5B8DEF);
    static final Color ACCENT_HOVER = new Color(0x6E9BFF);
    static final Color ALLOW = new Color(0x52C58A);
    static final Color DENY = new Color(0xF07178);
    static final Color SELECTION = new Color(0x2A3B55);

    static final Font BASE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font BOLD_FONT = BASE_FONT.deriveFont(Font.BOLD);
    static final Font TITLE_FONT = BASE_FONT.deriveFont(Font.BOLD, 20.0f);
    static final Font SUBTITLE_FONT = BASE_FONT.deriveFont(12.0f);
    static final Font SMALL_FONT = BASE_FONT.deriveFont(11.0f);

    private ZomboidApprovalTheme() {
        throw new UnsupportedOperationException("Theme utility class cannot be instantiated");
    }

    /**
     * Applies dark defaults to the current look and feel.
     */
    static void apply() {
        UIManager.put("Panel.background", WINDOW);
        UIManager.put("OptionPane.background", WINDOW);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.font", BASE_FONT);
        UIManager.put("TableHeader.background", HEADER);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.font", BOLD_FONT.deriveFont(11.0f));
        UIManager.put("TableHeader.cellBorder", BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", BASE_FONT);
        UIManager.put("Button.background", SURFACE_ALT);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.select", SELECTION);
        UIManager.put("Button.font", BOLD_FONT);
        UIManager.put("CheckBox.background", WINDOW);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("CheckBox.font", BASE_FONT);
        UIManager.put("ScrollBar.background", SURFACE);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollBar.track", SURFACE);
        UIManager.put("ScrollBar.foreground", MUTED);
        UIManager.put("ToolTip.background", HEADER);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("ToolTip.font", SMALL_FONT);
    }

    /**
     * Creates a table header renderer that matches the dark surface.
     *
     * @return header renderer
     */
    static TableCellRenderer headerRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setOpaque(true);
        renderer.setBackground(HEADER);
        renderer.setForeground(MUTED);
        renderer.setFont(BOLD_FONT.deriveFont(11.0f));
        renderer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        return renderer;
    }

    /**
     * Styles a flat action button.
     *
     * @param button     button to style
     * @param background base background color
     * @param foreground text color
     */
    static void styleButton(JButton button, Color background, Color foreground) {
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(BOLD_FONT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        Color hover = blend(background, Color.WHITE, 0.08f);
        button.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void mouseExited(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(background);
                }
            }
        });
    }

    private static Color blend(Color base, Color overlay, float amount) {
        int red = Math.round(base.getRed() + (overlay.getRed() - base.getRed()) * amount);
        int green = Math.round(base.getGreen() + (overlay.getGreen() - base.getGreen()) * amount);
        int blue = Math.round(base.getBlue() + (overlay.getBlue() - base.getBlue()) * amount);
        return new Color(
                Math.max(0, Math.min(255, red)),
                Math.max(0, Math.min(255, green)),
                Math.max(0, Math.min(255, blue))
        );
    }
}
