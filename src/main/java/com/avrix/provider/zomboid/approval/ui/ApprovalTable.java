package com.avrix.provider.zomboid.approval.ui;

import com.avrix.provider.zomboid.approval.ApprovalMessage;
import com.avrix.provider.zomboid.approval.ApprovalMessages;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * Table with expandable detail rows and full-value tooltips.
 */
public final class ApprovalTable extends JTable {
    /**
     * Localized messages.
     */
    private final ApprovalMessages messages;

    /**
     * Cached detail panels keyed by entry index.
     */
    private final Map<Integer, ApprovalDetailPanel> detailPanels = new HashMap<>();

    /**
     * Cached detail heights keyed by entry index.
     */
    private final Map<Integer, Integer> detailHeights = new HashMap<>();

    /**
     * Row header attached to the table.
     */
    private ApprovalRowHeader rowHeader;

    /**
     * Viewport position captured before a selection change.
     */
    private java.awt.Point viewPositionBeforeSelection;

    /**
     * Creates a table for the given model.
     *
     * @param model    approval table model
     * @param messages localized messages
     */
    public ApprovalTable(ApprovalTableModel model, ApprovalMessages messages) {
        super(model);
        this.messages = messages;
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        addMouseListener(new MouseAdapter() {
            /**
             * Toggles the detail row when the mod name cell is clicked.
             *
             * @param event mouse event
             */
            @Override
            public void mousePressed(MouseEvent event) {
                ApprovalTableModel model = approvalModel();
                int row = rowAtPoint(event.getPoint());
                int column = columnAtPoint(event.getPoint());
                if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 1
                        && model != null && row >= 0 && column == ApprovalColumns.MOD
                        && !model.isDetailRow(row)) {
                    javax.swing.JViewport viewport = getParent() instanceof javax.swing.JViewport candidate
                            ? candidate
                            : null;
                    java.awt.Point viewPosition = viewPositionBeforeSelection != null
                            ? viewPositionBeforeSelection
                            : (viewport == null ? null : viewport.getViewPosition());
                    model.toggleExpanded(row);
                    detailHeights.clear();
                    updateRowHeights();
                    revalidate();
                    repaint();
                    if (viewport != null && viewPosition != null) {
                        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(
                                () -> viewport.setViewPosition(viewPosition)
                        ));
                    }
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            /**
             * Updates the cursor when the pointer moves over an expandable row.
             *
             * @param event mouse event
             */
            @Override
            public void mouseMoved(MouseEvent event) {
                ApprovalTableModel model = approvalModel();
                int row = rowAtPoint(event.getPoint());
                int column = columnAtPoint(event.getPoint());
                boolean hand = model != null && row >= 0 && column == ApprovalColumns.MOD
                        && !model.isDetailRow(row);
                setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        addComponentListener(new ComponentAdapter() {
            /**
             * Recomputes detail row heights after a resize.
             *
             * @param event component event
             */
            @Override
            public void componentResized(ComponentEvent event) {
                detailHeights.clear();
                updateRowHeights();
                revalidate();
                repaint();
            }
        });
    }

    /**
     * Returns the cell bounds, stretching detail rows across the table.
     *
     * @param row            row index
     * @param column         column index
     * @param includeSpacing whether intercell spacing is included
     * @return cell bounds
     */
    @Override
    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        Rectangle rect = super.getCellRect(row, column, includeSpacing);
        ApprovalTableModel model = approvalModel();
        if (model != null && row >= 0 && row < getRowCount() && model.isDetailRow(row)) {
            if (column == ApprovalColumns.STATE) {
                rect.width = getColumnModel().getTotalColumnWidth();
            } else {
                rect.width = 0;
            }
        }
        return rect;
    }

    /**
     * Returns the row index at the given point.
     *
     * @param point point in table coordinates
     * @return row index, or {@code -1} when the point is outside the rows
     */
    @Override
    public int rowAtPoint(java.awt.Point point) {
        if (approvalModel() == null) {
            return super.rowAtPoint(point);
        }
        for (int row = 0; row < getRowCount(); row++) {
            Rectangle rect = super.getCellRect(row, 0, true);
            if (point.y >= rect.y && point.y < rect.y + rect.height) {
                return row;
            }
        }
        return -1;
    }

    /**
     * Returns the preferred size including all row heights.
     *
     * @return preferred table size
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width = getColumnModel().getTotalColumnWidth();
        int height = 0;
        for (int row = 0; row < getRowCount(); row++) {
            height += getRowHeight(row);
        }
        size.height = Math.max(size.height, height);
        return size;
    }

    /**
     * Returns the tooltip for the cell under the pointer.
     *
     * @param event mouse event
     * @return tooltip text, or {@code null} when the cell has none
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int column = columnAtPoint(event.getPoint());
        if (row < 0 || column < 0) {
            return null;
        }

        ApprovalTableModel model = approvalModel();
        if (model == null || model.isDetailRow(row)) {
            return null;
        }

        int entryIndex = model.entryIndex(row);
        if (column == ApprovalColumns.MOD) {
            return model.isExpanded(entryIndex)
                    ? messages.get(ApprovalMessage.TOOLTIP_DETAILS_HIDE)
                    : messages.get(ApprovalMessage.TOOLTIP_DETAILS_SHOW);
        }

        ZomboidApprovalEntry entry = model.entryAt(entryIndex);
        if (column == ApprovalColumns.STATE) {
            return entry.previousSha256().isBlank()
                    ? messages.get(ApprovalMessage.TOOLTIP_STATE_NEW)
                    : messages.format(ApprovalMessage.TOOLTIP_STATE_PREVIOUS, entry.previousSha256());
        }
        if (column == ApprovalColumns.AUTHOR) {
            return "<html><b>" + ApprovalFormatting.escapeHtml(entry.displayName()) + "</b><br/>"
                    + messages.format(ApprovalMessage.LABEL_PLUGIN_ID, ApprovalFormatting.escapeHtml(entry.pluginId())) + "<br/>"
                    + messages.format(ApprovalMessage.LABEL_MOD_ID, ApprovalFormatting.escapeHtml(entry.modId())) + "<br/>"
                    + messages.format(
                            ApprovalMessage.LABEL_AUTHOR,
                            ApprovalFormatting.escapeHtml(ApprovalFormatting.blankToDash(entry.modAuthor()))
                    ) + "<br/>"
                    + ApprovalFormatting.escapeHtml(entry.jarPath().toString()) + "<br/>"
                    + messages.format(ApprovalMessage.LABEL_SHA256, ApprovalFormatting.escapeHtml(entry.sha256())) + "</html>";
        }
        if (column == ApprovalColumns.SIZE) {
            return ApprovalFormatting.formatSize(entry.size());
        }
        return null;
    }

    /**
     * Remembers the viewport position before changing the selection.
     *
     * @param rowIndex    row index
     * @param columnIndex column index
     * @param toggle      whether to toggle the selection
     * @param extend      whether to extend the selection
     */
    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        if (getParent() instanceof javax.swing.JViewport viewport) {
            viewPositionBeforeSelection = viewport.getViewPosition();
        }
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
    }

    private ApprovalTableModel approvalModel() {
        return getModel() instanceof ApprovalTableModel model ? model : null;
    }

    /**
     * Sets the row header used by the table.
     *
     * @param rowHeader row header
     */
    public void setRowHeader(ApprovalRowHeader rowHeader) {
        this.rowHeader = rowHeader;
        refreshRowHeader();
    }

    private void refreshRowHeader() {
        if (rowHeader != null) {
            rowHeader.revalidate();
            rowHeader.repaint();
        }
    }

    /**
     * Reloads localized column headers and invalidates cached detail panels.
     */
    public void languageChanged() {
        ApprovalTableModel model = approvalModel();
        if (model != null) {
            TableColumnModel columns = getColumnModel();
            for (int index = 0; index < columns.getColumnCount(); index++) {
                TableColumn column = columns.getColumn(index);
                column.setHeaderValue(model.getColumnName(column.getModelIndex()));
            }
            getTableHeader().repaint();
        }
        detailPanels.clear();
        detailHeights.clear();
        updateRowHeights();
        repaint();
    }

    /**
     * Recomputes detail row heights after a column resize.
     *
     * @param event change event
     */
    @Override
    public void columnMarginChanged(ChangeEvent event) {
        super.columnMarginChanged(event);
        detailHeights.clear();
        updateRowHeights();
    }

    /**
     * Recomputes every row height from the current layout.
     */
    public void updateRowHeights() {
        ApprovalTableModel model = approvalModel();
        if (model == null) {
            return;
        }
        for (int row = 0; row < getRowCount(); row++) {
            int height = model.isDetailRow(row) ? detailHeight(model.entryIndex(row)) : ApprovalColumns.BASE_ROW_HEIGHT;
            if (getRowHeight(row) != height) {
                setRowHeight(row, height);
            }
        }
        refreshRowHeader();
    }

    private int detailHeight(int entryIndex) {
        return detailHeights.computeIfAbsent(entryIndex, ignored -> detailPanel(entryIndex)
                .preferredHeight(Math.max(240, getColumnModel().getTotalColumnWidth())));
    }

    /**
     * Returns the cached detail panel for an entry.
     *
     * @param entryIndex entry index
     * @return detail panel
     */
    public ApprovalDetailPanel detailPanel(int entryIndex) {
        return detailPanels.computeIfAbsent(entryIndex, ignored -> {
            ApprovalTableModel model = approvalModel();
            return new ApprovalDetailPanel(model.entryAt(entryIndex), model.inspection(entryIndex), messages);
        });
    }
}
