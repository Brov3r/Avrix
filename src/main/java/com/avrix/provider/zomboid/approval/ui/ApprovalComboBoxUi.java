package com.avrix.provider.zomboid.approval.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.Cursor;

/**
 * Combo box UI with a dark arrow button that matches the approval window.
 */
public final class ApprovalComboBoxUi extends BasicComboBoxUI {

    /**
     * Creates the combo box UI.
     */
    public ApprovalComboBoxUi() {
    }

    /**
     * Creates the dropdown arrow button.
     *
     * @return arrow button
     */
    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton(new ApprovalChevronIcon());
        button.setBackground(ZomboidApprovalTheme.HEADER);
        button.setForeground(ZomboidApprovalTheme.MUTED);
        button.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
