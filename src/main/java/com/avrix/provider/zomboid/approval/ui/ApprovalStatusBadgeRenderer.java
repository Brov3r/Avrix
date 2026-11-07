package com.avrix.provider.zomboid.approval.ui;

import com.avrix.provider.zomboid.approval.ApprovalMessage;
import com.avrix.provider.zomboid.approval.ApprovalMessages;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Rounded status pill renderer for the State column.
 */
public final class ApprovalStatusBadgeRenderer extends JLabel implements TableCellRenderer {
    /**
     * Localized messages.
     */
    private final ApprovalMessages messages;

    /**
     * Current badge background.
     */
    private Color badgeBackground;

    /**
     * Current badge foreground.
     */
    private Color badgeForeground;

    /**
     * Creates a status badge renderer.
     *
     * @param messages localized messages
     */
    public ApprovalStatusBadgeRenderer(ApprovalMessages messages) {
        this.messages = messages;
        setOpaque(true);
        setFont(ZomboidApprovalTheme.BADGE_FONT);
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    }

    /**
     * Configures the status badge renderer for one cell.
     *
     * @param table      table being rendered
     * @param value      cell value
     * @param isSelected whether the cell is selected
     * @param hasFocus   whether the cell has focus
     * @param row        row index
     * @param column     column index
     * @return configured renderer component
     */
    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        String state = value == null ? "" : value.toString();
        boolean changed = "CHANGED".equals(state);
        boolean created = "NEW".equals(state);
        if (changed || created) {
            setText(messages.get(changed
                    ? ApprovalMessage.STATE_CHANGED
                    : ApprovalMessage.STATE_NEW));
        } else {
            setText(state);
        }
        if (changed) {
            badgeBackground = ZomboidApprovalTheme.BADGE_CHANGED_BG;
            badgeForeground = ZomboidApprovalTheme.BADGE_CHANGED_FG;
        } else if (created) {
            badgeBackground = ZomboidApprovalTheme.BADGE_NEW_BG;
            badgeForeground = ZomboidApprovalTheme.BADGE_NEW_FG;
        } else {
            badgeBackground = ZomboidApprovalTheme.SURFACE_ALT;
            badgeForeground = ZomboidApprovalTheme.MUTED;
        }
        setBackground(isSelected
                ? ZomboidApprovalTheme.SELECTION
                : (row % 2 == 0 ? ZomboidApprovalTheme.SURFACE : ZomboidApprovalTheme.SURFACE_ALT));
        return this;
    }

    /**
     * Paints the rounded status badge.
     *
     * @param graphics target graphics
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        String text = getText();
        if (text.isEmpty()) {
            g2.dispose();
            return;
        }

        g2.setFont(getFont());
        FontMetrics metrics = g2.getFontMetrics();
        int badgeWidth = metrics.stringWidth(text) + 28;
        int badgeHeight = 20;
        int x = Math.max(8, (getWidth() - badgeWidth) / 2);
        int y = (getHeight() - badgeHeight) / 2;

        g2.setColor(badgeBackground);
        g2.fillRoundRect(x, y, badgeWidth, badgeHeight, badgeHeight, badgeHeight);
        int dotSize = 6;
        int dotX = x + 8;
        int dotY = y + (badgeHeight - dotSize) / 2;
        ZomboidApprovalIcons.paint(g2, "circle", dotX - 2, dotY - 2, dotSize + 4, badgeForeground);
        g2.setColor(badgeForeground);
        int baseline = y + (badgeHeight - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.drawString(text, x + 19, baseline);
        g2.dispose();
    }
}
