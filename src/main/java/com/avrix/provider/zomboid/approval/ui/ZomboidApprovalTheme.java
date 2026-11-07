package com.avrix.provider.zomboid.approval.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Small, dependency-free dark theme used by the approval window.
 */
public final class ZomboidApprovalTheme {

    /**
     * Main window background.
     */
    public static final Color WINDOW = new Color(0x1A1A1A);

    /**
     * Default table and panel surface.
     */
    public static final Color SURFACE = new Color(0x222222);

    /**
     * Alternating row surface.
     */
    public static final Color SURFACE_ALT = new Color(0x1E1E1E);

    /**
     * Expanded detail row background.
     */
    public static final Color DETAIL_BG = new Color(0x1E1E1E);

    /**
     * Table header and hover background.
     */
    public static final Color HEADER = new Color(0x2A2A2A);

    /**
     * Standard separator and grid color.
     */
    public static final Color BORDER = new Color(0x3A3A3A);

    /**
     * Emphasized border and scrollbar thumb color.
     */
    public static final Color BORDER_STRONG = new Color(0x4D4D4D);

    /**
     * Primary text color.
     */
    public static final Color TEXT = new Color(0xECECEC);

    /**
     * Secondary and hint text color.
     */
    public static final Color MUTED = new Color(0xB9B9B9);

    /**
     * Accent color for active controls and badges.
     */
    public static final Color ACCENT = new Color(0x9BD1C1);

    /**
     * Accent color used while hovering.
     */
    public static final Color ACCENT_HOVER = new Color(0xBCE3D5);

    /**
     * Allow action and positive status color.
     */
    public static final Color ALLOW = new Color(0xAEDCB6);

    /**
     * Warning and changed status color.
     */
    public static final Color WARN = new Color(0xE8D4A2);

    /**
     * Deny action and error color.
     */
    public static final Color DENY = new Color(0xE9B4B4);

    /**
     * Selected row and pressed control background.
     */
    public static final Color SELECTION = new Color(0x26332F);

    /**
     * Secondary emphasis color for target classes.
     */
    public static final Color SECONDARY = new Color(0xB9B4D6);

    /**
     * Dark foreground used on accent backgrounds.
     */
    public static final Color INK = new Color(0x0D0D0F);

    /**
     * Background of the NEW status badge.
     */
    public static final Color BADGE_NEW_BG = new Color(0x26332F);

    /**
     * Foreground of the NEW status badge.
     */
    public static final Color BADGE_NEW_FG = new Color(0xAEDCB6);

    /**
     * Background of the CHANGED status badge.
     */
    public static final Color BADGE_CHANGED_BG = new Color(0x2D2B33);

    /**
     * Foreground of the CHANGED status badge.
     */
    public static final Color BADGE_CHANGED_FG = new Color(0xE8D4A2);

    /**
     * Edge length of the custom checkbox icon in pixels.
     */
    public static final int CHECKBOX_SIZE = 16;

    /**
     * Base UI font.
     */
    public static final Font BASE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    /**
     * Bold variant of the base font.
     */
    public static final Font BOLD_FONT = BASE_FONT.deriveFont(Font.BOLD);

    /**
     * Font used for the window heading.
     */
    public static final Font TITLE_FONT = BASE_FONT.deriveFont(Font.BOLD, 20.0f);

    /**
     * Font used for the subtitle and identity note.
     */
    public static final Font SUBTITLE_FONT = BASE_FONT.deriveFont(12.0f);

    /**
     * Small font used for hints and detail text.
     */
    public static final Font SMALL_FONT = BASE_FONT.deriveFont(11.0f);

    /**
     * Bold small font used for section labels.
     */
    public static final Font LABEL_FONT = BOLD_FONT.deriveFont(11.0f);

    /**
     * Compact bold font used for status badges.
     */
    public static final Font BADGE_FONT = BOLD_FONT.deriveFont(10.5f);

    /**
     * Monospaced font used for hashes.
     */
    public static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private ZomboidApprovalTheme() {
        throw new UnsupportedOperationException("Theme utility class cannot be instantiated");
    }

    /**
     * Applies dark defaults to the current look and feel.
     */
    public static void apply() {
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
        UIManager.put("ComboBox.background", SURFACE_ALT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", SELECTION);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ComboBox.font", BASE_FONT);
        UIManager.put("ScrollBar.background", SURFACE);
        UIManager.put("ScrollBar.thumb", BORDER_STRONG);
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
    public static TableCellRenderer headerRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setOpaque(true);
        renderer.setBackground(HEADER);
        renderer.setForeground(MUTED);
        renderer.setFont(BOLD_FONT.deriveFont(11.0f));
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        renderer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_STRONG),
                BorderFactory.createEmptyBorder(9, 8, 9, 8)
        ));
        return renderer;
    }

    /**
     * Styles a rounded action button.
     *
     * @param button     button to style
     * @param background base background color
     * @param foreground text color
     */
    public static void styleButton(JButton button, Color background, Color foreground) {
        button.setUI(new ApprovalButtonUi());
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(BOLD_FONT);
        button.setIconTextGap(7);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(9, 17, 9, 17));

        Color hover = blend(background, Color.WHITE, 0.08f);
        button.addMouseListener(new MouseAdapter() {
            /**
             * Highlights the button while the pointer is over it.
             *
             * @param event mouse event
             */
            @Override
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            /**
             * Restores the base button color when the pointer leaves.
             *
             * @param event mouse event
             */
            @Override
            public void mouseExited(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(background);
                }
            }
        });
    }

    /**
     * Applies the modern dark checkbox style.
     *
     * @param checkBox checkbox to style
     */
    public static void styleCheckBox(JCheckBox checkBox) {
        checkBox.setUI(new BasicCheckBoxUI());
        ApprovalCheckBoxIcon icon = new ApprovalCheckBoxIcon();
        checkBox.setIcon(icon);
        checkBox.setSelectedIcon(icon);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.setBorderPainted(false);
        checkBox.setContentAreaFilled(false);
        checkBox.setIconTextGap(8);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkBox.setFont(BASE_FONT);
        checkBox.setForeground(TEXT);
    }

    /**
     * Applies the dark combo box style used by the language selector.
     *
     * @param comboBox combo box to style
     */
    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setUI(new ApprovalComboBoxUi());
        comboBox.setOpaque(true);
        comboBox.setBackground(SURFACE_ALT);
        comboBox.setForeground(TEXT);
        comboBox.setFont(BASE_FONT);
        comboBox.setFocusable(false);
        comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG),
                BorderFactory.createEmptyBorder(4, 10, 4, 4)
        ));
        comboBox.setRenderer(new ApprovalComboBoxRenderer());
        comboBox.setMaximumRowCount(Math.max(4, comboBox.getItemCount()));
    }

    /**
     * Draws the shared rounded checkbox state.
     *
     * @param graphics target graphics
     * @param x        left edge
     * @param y        top edge
     * @param selected whether the check mark is shown
     * @param hover    whether the pointer is over the checkbox
     * @param enabled  whether the checkbox is interactive
     */
    public static void paintCheckBox(Graphics2D graphics, int x, int y, boolean selected, boolean hover, boolean enabled) {
        paintCheckBox(graphics, x, y, selected, false, hover, enabled);
    }

    /**
     * Draws the shared rounded checkbox state, including an indeterminate state.
     *
     * @param graphics target graphics
     * @param x        left edge
     * @param y        top edge
     * @param selected whether the check mark is shown
     * @param partial  whether the indeterminate dash is shown
     * @param hover    whether the pointer is over the checkbox
     * @param enabled  whether the checkbox is interactive
     */
    public static void paintCheckBox(
            Graphics2D graphics,
            int x,
            int y,
            boolean selected,
            boolean partial,
            boolean hover,
            boolean enabled
    ) {
        boolean highlighted = selected || partial;
        Color background;
        if (highlighted) {
            background = enabled ? ACCENT : blend(ACCENT, WINDOW, 0.55f);
        } else {
            background = hover ? HEADER : SURFACE_ALT;
        }
        Color border = highlighted ? ACCENT : (hover ? ACCENT_HOVER : BORDER_STRONG);

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, 6, 6);
        g2.setColor(border);
        g2.drawRoundRect(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, 6, 6);

        if (selected || partial) {
            ZomboidApprovalIcons.paint(
                    g2,
                    partial ? "minus" : "tick",
                    x + 2,
                    y + 2,
                    CHECKBOX_SIZE - 4,
                    enabled ? INK : MUTED
            );
        }
        g2.dispose();
    }





    /**
     * Applies the modern dark scrollbar style.
     *
     * @param scrollBar scrollbar to style
     */
    public static void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setUI(new ApprovalScrollBarUi());
        scrollBar.setOpaque(false);
        scrollBar.setBackground(SURFACE);
        scrollBar.setForeground(BORDER_STRONG);
        scrollBar.setPreferredSize(scrollBar.getOrientation() == SwingConstants.VERTICAL
                ? new Dimension(10, 0)
                : new Dimension(0, 10));
    }

    /**
     * Blends two colors.
     *
     * @param base    base color
     * @param overlay overlay color
     * @param amount  blend amount from {@code 0.0} to {@code 1.0}
     * @return blended color
     */
    public static Color blend(Color base, Color overlay, float amount) {
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
