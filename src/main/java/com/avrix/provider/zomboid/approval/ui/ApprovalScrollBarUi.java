package com.avrix.provider.zomboid.approval.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Thin scrollbar with a rounded thumb and no arrow buttons.
 */
public final class ApprovalScrollBarUi extends BasicScrollBarUI {

    /**
     * Creates the rounded scrollbar UI.
     */
    public ApprovalScrollBarUi() {
    }

    /**
     * Creates an invisible decrease button.
     *
     * @param orientation scrollbar orientation
     * @return empty button
     */
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return emptyButton();
    }

    /**
     * Creates an invisible increase button.
     *
     * @param orientation scrollbar orientation
     * @return empty button
     */
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return emptyButton();
    }

    private JButton emptyButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusable(false);
        return button;
    }

    /**
     * Paints the scrollbar track.
     *
     * @param graphics    target graphics
     * @param component   scrollbar component
     * @param trackBounds track bounds
     */
    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setColor(ZomboidApprovalTheme.SURFACE);
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2.dispose();
    }

    /**
     * Paints the rounded scrollbar thumb.
     *
     * @param graphics    target graphics
     * @param component   scrollbar component
     * @param thumbBounds thumb bounds
     */
    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isThumbRollover() ? ZomboidApprovalTheme.MUTED : ZomboidApprovalTheme.BORDER_STRONG);
        int inset = 2;
        g2.fillRoundRect(
                thumbBounds.x + inset,
                thumbBounds.y + inset,
                Math.max(2, thumbBounds.width - inset * 2),
                Math.max(2, thumbBounds.height - inset * 2),
                8,
                8
        );
        g2.dispose();
    }
}
