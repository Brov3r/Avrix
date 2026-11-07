package com.avrix.provider.zomboid.approval.ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;

/**
 * List renderer that keeps the closed selector and the popup on the dark surface.
 */
public final class ApprovalComboBoxRenderer extends DefaultListCellRenderer {

    /**
     * Creates the combo box renderer.
     */
    public ApprovalComboBoxRenderer() {
    }

    /**
     * Styles one language entry in the selector or popup list.
     *
     * @param list         source list
     * @param value        language value
     * @param index        item index, or {@code -1} for the closed selector
     * @param isSelected   whether the item is selected
     * @param cellHasFocus whether the item has focus
     * @return configured renderer component
     */
    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
    ) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setOpaque(true);
        setFont(ZomboidApprovalTheme.BASE_FONT);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setBackground(isSelected ? ZomboidApprovalTheme.SELECTION : ZomboidApprovalTheme.SURFACE_ALT);
        setForeground(ZomboidApprovalTheme.TEXT);
        return this;
    }
}
