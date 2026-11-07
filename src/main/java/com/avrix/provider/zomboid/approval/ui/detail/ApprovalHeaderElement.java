package com.avrix.provider.zomboid.approval.ui.detail;

import com.avrix.provider.zomboid.approval.ApprovalMessage;
import com.avrix.provider.zomboid.approval.ApprovalMessages;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;
import com.avrix.provider.zomboid.approval.ui.ApprovalFormatting;
import com.avrix.provider.zomboid.approval.ui.ZomboidApprovalTheme;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Detail panel header that shows the plugin title and status badge.
 */
public final class ApprovalHeaderElement implements ApprovalDetailElement {

    private static final int HEIGHT = 34;

    private final String title;
    private final ApprovalMessages messages;
    private final boolean changed;

    /**
     * Creates a header element for one entry.
     *
     * @param entry    approval entry
     * @param messages localized messages
     */
    public ApprovalHeaderElement(ZomboidApprovalEntry entry, ApprovalMessages messages) {
        this.title = entry.displayName().isBlank() ? entry.pluginId() : entry.displayName();
        this.messages = messages;
        this.changed = !entry.previousSha256().isBlank();
    }

    /**
     * Returns the header element height.
     *
     * @return height in pixels
     */
    @Override
    public int height() {
        return HEIGHT;
    }

    /**
     * Paints the plugin title and status badge.
     *
     * @param graphics target graphics
     * @param x        left edge
     * @param y        top edge
     * @param width    available width
     */
    @Override
    public void paint(Graphics2D graphics, int x, int y, int width) {
        String state = messages.get(changed
                ? ApprovalMessage.STATE_CHANGED
                : ApprovalMessage.STATE_NEW);
        Font titleFont = ZomboidApprovalTheme.BOLD_FONT.deriveFont(14.0f);
        FontMetrics badgeMetrics = graphics.getFontMetrics(ZomboidApprovalTheme.BADGE_FONT);
        int pillWidth = badgeMetrics.stringWidth(state) + 18;
        int pillHeight = 18;
        int pillX = x + width - pillWidth;

        graphics.setFont(titleFont);
        graphics.setColor(ZomboidApprovalTheme.TEXT);
        FontMetrics titleMetrics = graphics.getFontMetrics();
        graphics.drawString(
                ApprovalFormatting.ellipsize(title, titleMetrics, Math.max(80, width - pillWidth - 12)),
                x,
                y + 18
        );

        graphics.setColor(changed
                ? ZomboidApprovalTheme.BADGE_CHANGED_BG
                : ZomboidApprovalTheme.BADGE_NEW_BG);
        graphics.fillRoundRect(pillX, y + 2, pillWidth, pillHeight, pillHeight, pillHeight);
        graphics.setFont(ZomboidApprovalTheme.BADGE_FONT);
        graphics.setColor(changed
                ? ZomboidApprovalTheme.BADGE_CHANGED_FG
                : ZomboidApprovalTheme.BADGE_NEW_FG);
        FontMetrics stateMetrics = graphics.getFontMetrics();
        graphics.drawString(
                state,
                pillX + 9,
                y + 2 + (pillHeight - stateMetrics.getHeight()) / 2 + stateMetrics.getAscent()
        );
    }
}
