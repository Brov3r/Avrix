package com.avrix.provider.zomboid.approval.ui.detail;

import com.avrix.provider.zomboid.approval.ui.ApprovalFormatting;
import com.avrix.provider.zomboid.approval.ui.ZomboidApprovalTheme;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Detail panel chip block.
 */
public final class ApprovalChipElement implements ApprovalDetailElement {

    private static final int CHIP_HEIGHT = 24;
    private static final int CHIP_GAP = 6;

    private final List<ApprovalChipRow> rows;
    private final Color background;
    private final Color foreground;
    private final int height;

    /**
     * Creates a chip block.
     *
     * @param values     chip labels
     * @param background chip background
     * @param foreground chip text color
     * @param width      available width
     * @param metrics    font metrics used for layout
     */
    public ApprovalChipElement(
            List<String> values,
            Color background,
            Color foreground,
            int width,
            FontMetrics metrics
    ) {
        this.background = background;
        this.foreground = foreground;
        this.rows = layoutRows(values, width, metrics);
        this.height = rows.size() * CHIP_HEIGHT + Math.max(0, rows.size() - 1) * CHIP_GAP;
    }

    /**
     * Returns the chip block height.
     *
     * @return height in pixels
     */
    @Override
    public int height() {
        return height;
    }

    /**
     * Paints the chips row by row.
     *
     * @param graphics target graphics
     * @param x        left edge
     * @param y        top edge
     * @param width    available width
     */
    @Override
    public void paint(Graphics2D graphics, int x, int y, int width) {
        graphics.setFont(ZomboidApprovalTheme.SMALL_FONT);
        FontMetrics metrics = graphics.getFontMetrics();
        int rowY = y;
        for (ApprovalChipRow row : rows) {
            int chipX = x;
            for (ApprovalChip chip : row.chips()) {
                graphics.setColor(background);
                graphics.fillRoundRect(chipX, rowY, chip.width(), CHIP_HEIGHT, 12, 12);
                graphics.setColor(foreground);
                int baseline = rowY + (CHIP_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
                graphics.drawString(chip.text(), chipX + 16, baseline);
                chipX += chip.width() + CHIP_GAP;
            }
            rowY += CHIP_HEIGHT + CHIP_GAP;
        }
    }

    private List<ApprovalChipRow> layoutRows(List<String> values, int width, FontMetrics metrics) {
        List<ApprovalChipRow> result = new ArrayList<>();
        List<ApprovalChip> current = new ArrayList<>();
        int used = 0;

        for (String value : values) {
            String text = ApprovalFormatting.ellipsize(value, metrics, Math.max(40, width - 32));
            int chipWidth = Math.min(width, metrics.stringWidth(text) + 32);
            if (!current.isEmpty() && used + chipWidth > width) {
                result.add(new ApprovalChipRow(current));
                current = new ArrayList<>();
                used = 0;
            }
            current.add(new ApprovalChip(text, chipWidth));
            used += chipWidth + CHIP_GAP;
        }
        if (!current.isEmpty()) {
            result.add(new ApprovalChipRow(current));
        }
        return result;
    }
}
