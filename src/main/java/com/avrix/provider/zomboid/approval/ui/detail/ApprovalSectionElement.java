package com.avrix.provider.zomboid.approval.ui.detail;

import com.avrix.provider.zomboid.approval.ui.ZomboidApprovalIcons;
import com.avrix.provider.zomboid.approval.ui.ZomboidApprovalTheme;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Detail panel section header with a glyph, title, and separator line.
 */
public final class ApprovalSectionElement implements ApprovalDetailElement {

    private static final int HEIGHT = 22;

    private final String title;
    private final ApprovalDetailGlyph glyph;
    private final Color color;

    /**
     * Creates a section header.
     *
     * @param title section title
     * @param glyph section glyph
     * @param color glyph color
     */
    public ApprovalSectionElement(String title, ApprovalDetailGlyph glyph, Color color) {
        this.title = title;
        this.glyph = glyph;
        this.color = color;
    }

    /**
     * Returns the section title height.
     *
     * @return height in pixels
     */
    @Override
    public int height() {
        return HEIGHT;
    }

    /**
     * Paints the section glyph, title, and separator line.
     *
     * @param graphics target graphics
     * @param x        left edge
     * @param y        top edge
     * @param width    available width
     */
    @Override
    public void paint(Graphics2D graphics, int x, int y, int width) {
        ZomboidApprovalIcons.paint(graphics, glyph.icon(), x, y + 2, 18, color);
        graphics.setFont(ZomboidApprovalTheme.LABEL_FONT);
        graphics.setColor(ZomboidApprovalTheme.MUTED);
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(title, x + 22, y + 3 + metrics.getAscent());

        int lineStart = x + 22 + metrics.stringWidth(title) + 8;
        if (lineStart < x + width) {
            graphics.setColor(ZomboidApprovalTheme.BORDER);
            graphics.drawLine(lineStart, y + 10, x + width, y + 10);
        }
    }
}
