package com.avrix.provider.zomboid.approval.ui.detail;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Detail panel text block.
 */
public final class ApprovalTextElement implements ApprovalDetailElement {

    private final List<String> lines;
    private final Font font;
    private final Color color;
    private final int indent;
    private final int height;

    /**
     * Creates a text block.
     *
     * @param lines  text lines
     * @param font   text font
     * @param color  text color
     * @param indent left indent in pixels
     */
    public ApprovalTextElement(List<String> lines, Font font, Color color, int indent) {
        this.lines = List.copyOf(lines);
        this.font = font;
        this.color = color;
        this.indent = indent;
        this.height = Math.max(1, lines.size()) * 15 + 2;
    }

    /**
     * Returns the text block height.
     *
     * @return height in pixels
     */
    @Override
    public int height() {
        return height;
    }

    /**
     * Paints the text lines.
     *
     * @param graphics target graphics
     * @param x        left edge
     * @param y        top edge
     * @param width    available width
     */
    @Override
    public void paint(Graphics2D graphics, int x, int y, int width) {
        graphics.setFont(font);
        graphics.setColor(color);
        FontMetrics metrics = graphics.getFontMetrics();
        int lineY = y;
        for (String line : lines) {
            graphics.drawString(line, x + indent, lineY + metrics.getAscent());
            lineY += 15;
        }
    }
}
