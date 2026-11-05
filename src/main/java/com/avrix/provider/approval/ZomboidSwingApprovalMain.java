package com.avrix.provider.approval;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standalone Swing entry point for the Project Zomboid approval window.
 * <p>
 * The process reads one {@link ZomboidApprovalBatch} request from standard input and writes one response to standard
 * output. All diagnostics go to standard error so the pipe protocol is never corrupted.
 */
public final class ZomboidSwingApprovalMain {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final int WINDOW_WIDTH = 1040;
    private static final int WINDOW_HEIGHT = 560;
    private static final int ALLOW_COLUMN = 0;
    private static final int MOD_COLUMN = 1;
    private static final int AUTHOR_COLUMN = 2;
    private static final int MOD_ID_COLUMN = 3;
    private static final int SOURCE_COLUMN = 4;
    private static final int STATE_COLUMN = 5;
    private static final int SIZE_COLUMN = 6;
    private static final int MODIFIED_COLUMN = 7;
    private static final int SHA_COLUMN = 8;

    private ZomboidSwingApprovalMain() {
        throw new UnsupportedOperationException("UI entrypoint class cannot be instantiated");
    }

    /**
     * Runs the approval window for one request.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        PrintStream protocol = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        System.setOut(System.err);

        try {
            List<ZomboidApprovalEntry> entries = ZomboidApprovalBatch.readRequest(
                    new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
            );

            if (entries.isEmpty()) {
                ZomboidApprovalBatch.writeResponse(
                        new BufferedWriter(new OutputStreamWriter(protocol, StandardCharsets.UTF_8)),
                        new ZomboidApprovalResponse(false, List.of())
                );
                System.exit(0);
                return;
            }

            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                ZomboidApprovalTheme.apply();
            } catch (Exception ignored) {
            }

            AtomicReference<ZomboidApprovalResponse> result =
                    new AtomicReference<>(new ZomboidApprovalResponse(false, List.of()));
            SwingUtilities.invokeAndWait(() -> result.set(showDialog(entries)));

            ZomboidApprovalBatch.writeResponse(
                    new BufferedWriter(new OutputStreamWriter(protocol, StandardCharsets.UTF_8)),
                    result.get()
            );
            System.exit(0);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            System.exit(3);
        }
    }

    private static ZomboidApprovalResponse showDialog(List<ZomboidApprovalEntry> entries) {
        ApprovalTableModel model = new ApprovalTableModel(entries);
        AtomicReference<ZomboidApprovalResponse> result =
                new AtomicReference<>(new ZomboidApprovalResponse(false, List.of()));

        JDialog dialog = new JDialog((java.awt.Frame) null, "Avrix Java Plugin Approval", true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.getContentPane().setBackground(ZomboidApprovalTheme.WINDOW);
        dialog.addWindowListener(new WindowAdapter() {
            /** {@inheritDoc} */
            @Override
            public void windowClosing(WindowEvent event) {
                result.set(new ZomboidApprovalResponse(false, List.of()));
                dialog.dispose();
            }
        });

        JTable table = new TooltipTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(ZomboidApprovalTheme.SURFACE);
        table.setForeground(ZomboidApprovalTheme.TEXT);
        table.setSelectionBackground(ZomboidApprovalTheme.SELECTION);
        table.setSelectionForeground(ZomboidApprovalTheme.TEXT);
        table.setFont(ZomboidApprovalTheme.BASE_FONT);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setDefaultRenderer(Object.class, new DarkCellRenderer());
        table.setDefaultRenderer(Boolean.class, new DarkBooleanRenderer());
        table.getTableHeader().setDefaultRenderer(ZomboidApprovalTheme.headerRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(ALLOW_COLUMN).setPreferredWidth(48);
        columns.getColumn(MOD_COLUMN).setPreferredWidth(185);
        columns.getColumn(AUTHOR_COLUMN).setPreferredWidth(130);
        columns.getColumn(MOD_ID_COLUMN).setPreferredWidth(145);
        columns.getColumn(SOURCE_COLUMN).setPreferredWidth(155);
        columns.getColumn(STATE_COLUMN).setPreferredWidth(75);
        columns.getColumn(SIZE_COLUMN).setPreferredWidth(70);
        columns.getColumn(MODIFIED_COLUMN).setPreferredWidth(105);
        columns.getColumn(SHA_COLUMN).setPreferredWidth(155);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ZomboidApprovalTheme.BORDER));
        scroll.getViewport().setBackground(ZomboidApprovalTheme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(WINDOW_WIDTH - 36, WINDOW_HEIGHT - 220));

        JLabel title = new JLabel("Java plugins require approval");
        title.setFont(ZomboidApprovalTheme.TITLE_FONT);
        title.setForeground(ZomboidApprovalTheme.TEXT);
        JLabel subtitle = new JLabel(
                "Review new or changed archives. Nothing loads until you press Apply; unchecked rows are denied."
        );
        subtitle.setFont(ZomboidApprovalTheme.SUBTITLE_FONT);
        subtitle.setForeground(ZomboidApprovalTheme.MUTED);
        JLabel identityNote = new JLabel(
                "SHA-256 is the identity. Mod names, authors, and source metadata are not cryptographically verified."
        );
        identityNote.setFont(ZomboidApprovalTheme.SMALL_FONT);
        identityNote.setForeground(ZomboidApprovalTheme.MUTED);

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.CENTER);
        heading.add(identityNote, BorderLayout.SOUTH);

        JPanel accent = new JPanel();
        accent.setBackground(ZomboidApprovalTheme.ACCENT);
        accent.setPreferredSize(new Dimension(0, 3));

        JPanel headingBlock = new JPanel(new BorderLayout(0, 10));
        headingBlock.setOpaque(false);
        headingBlock.add(accent, BorderLayout.NORTH);
        headingBlock.add(heading, BorderLayout.CENTER);

        JCheckBox remember = new JCheckBox("Remember for future launches");
        remember.setOpaque(false);
        remember.setForeground(ZomboidApprovalTheme.TEXT);
        remember.setFont(ZomboidApprovalTheme.BASE_FONT);
        remember.setFocusPainted(false);
        remember.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        remember.setToolTipText("When unchecked, decisions apply to this game session only.");

        JLabel rememberHint = new JLabel("Session-only unless checked");
        rememberHint.setForeground(ZomboidApprovalTheme.MUTED);
        rememberHint.setFont(ZomboidApprovalTheme.SMALL_FONT);

        JPanel rememberRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rememberRow.setOpaque(false);
        rememberRow.add(remember);
        rememberRow.add(rememberHint);

        JButton allowAll = new JButton("Allow all");
        JButton denyAll = new JButton("Deny all");
        JButton apply = new JButton("Apply");
        JButton cancel = new JButton("Cancel");

        ZomboidApprovalTheme.styleButton(allowAll, new Color(0x1B3A2A), new Color(0x8FE3B0));
        ZomboidApprovalTheme.styleButton(denyAll, new Color(0x3A1D22), new Color(0xFF9AA2));
        ZomboidApprovalTheme.styleButton(apply, ZomboidApprovalTheme.ACCENT, Color.WHITE);
        ZomboidApprovalTheme.styleButton(cancel, ZomboidApprovalTheme.SURFACE_ALT, ZomboidApprovalTheme.TEXT);

        allowAll.addActionListener(event -> model.setAll(true));
        denyAll.addActionListener(event -> model.setAll(false));
        cancel.addActionListener(event -> {
            result.set(new ZomboidApprovalResponse(false, List.of()));
            dialog.dispose();
        });
        apply.addActionListener(event -> {
            result.set(new ZomboidApprovalResponse(remember.isSelected(), model.decisions()));
            dialog.dispose();
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(allowAll);
        actions.add(denyAll);
        actions.add(apply);
        actions.add(cancel);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        footer.add(rememberRow, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(ZomboidApprovalTheme.WINDOW);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        root.add(headingBlock, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(apply);
        dialog.setAlwaysOnTop(true);
        dialog.pack();

        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(WINDOW_WIDTH, Math.max(720, screen.width - 140));
        int height = Math.min(WINDOW_HEIGHT, Math.max(420, screen.height - 120));
        dialog.setMinimumSize(new Dimension(720, 400));
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(null);
        SwingUtilities.invokeLater(dialog::toFront);
        dialog.setVisible(true);
        return result.get();
    }

    /**
     * Formats a byte count for display.
     *
     * @param bytes byte count
     * @return compact human-readable size
     */
    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format("%.1f KiB", bytes / 1024.0);
        }
        return String.format("%.1f MiB", bytes / (1024.0 * 1024.0));
    }

    private static String formatDate(long epochMillis) {
        return epochMillis <= 0L ? "unknown" : DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }

    private static String shortHash(String hash) {
        return hash.length() <= 16 ? hash : hash.substring(0, 16) + "…";
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Table model backed by the pending approval entries.
     */
    private static final class ApprovalTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Allow", "Mod", "Author", "Mod ID", "Source", "State", "Size", "Modified", "SHA-256"
        };

        private final List<ZomboidApprovalEntry> entries;
        private final boolean[] allow;

        private ApprovalTableModel(List<ZomboidApprovalEntry> entries) {
            this.entries = List.copyOf(entries);
            this.allow = new boolean[entries.size()];
        }

        /** {@inheritDoc} */
        @Override
        public int getRowCount() {
            return entries.size();
        }

        /** {@inheritDoc} */
        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        /** {@inheritDoc} */
        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        /** {@inheritDoc} */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == ALLOW_COLUMN ? Boolean.class : String.class;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == ALLOW_COLUMN;
        }

        /** {@inheritDoc} */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ZomboidApprovalEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case ALLOW_COLUMN -> allow[rowIndex];
                case MOD_COLUMN -> entry.displayName().isBlank() ? entry.pluginId() : entry.displayName();
                case AUTHOR_COLUMN -> blankToDash(entry.modAuthor());
                case MOD_ID_COLUMN -> blankToDash(entry.modId());
                case SOURCE_COLUMN -> source(entry);
                case STATE_COLUMN -> entry.previousSha256().isBlank() ? "NEW" : "CHANGED";
                case SIZE_COLUMN -> formatSize(entry.size());
                case MODIFIED_COLUMN -> formatDate(entry.modifiedEpochMillis());
                case SHA_COLUMN -> shortHash(entry.sha256());
                default -> "";
            };
        }

        /** {@inheritDoc} */
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == ALLOW_COLUMN) {
                allow[rowIndex] = Boolean.TRUE.equals(value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        private void setAll(boolean value) {
            for (int index = 0; index < allow.length; index++) {
                allow[index] = value;
            }
            fireTableDataChanged();
        }

        private List<ZomboidApprovalDecision> decisions() {
            List<ZomboidApprovalDecision> decisions = new ArrayList<>(entries.size());
            for (int index = 0; index < entries.size(); index++) {
                decisions.add(new ZomboidApprovalDecision(entries.get(index).sha256(), allow[index]));
            }
            return decisions;
        }

        private ZomboidApprovalEntry entryAt(int row) {
            return entries.get(row);
        }

        private static String source(ZomboidApprovalEntry entry) {
            if (!entry.workshopId().isBlank()) {
                return "Workshop " + entry.workshopId();
            }
            if (!entry.sourceName().isBlank()) {
                return entry.sourceName();
            }
            return entry.sourceType().isBlank() ? "local" : entry.sourceType();
        }
    }

    /**
     * Table with full-value tooltips for archive paths, ids, authors, and hashes.
     */
    private static final class TooltipTable extends JTable {
        private TooltipTable(ApprovalTableModel model) {
            super(model);
        }

        /** {@inheritDoc} */
        @Override
        public String getToolTipText(MouseEvent event) {
            int row = rowAtPoint(event.getPoint());
            int column = columnAtPoint(event.getPoint());
            if (row < 0 || column < 0) {
                return null;
            }

            ApprovalTableModel model = (ApprovalTableModel) getModel();
            ZomboidApprovalEntry entry = model.entryAt(row);
            if (column == ALLOW_COLUMN) {
                return "Check to allow this archive, leave unchecked to deny it.";
            }
            if (column == SHA_COLUMN) {
                return entry.sha256();
            }
            if (column == STATE_COLUMN && !entry.previousSha256().isBlank()) {
                return "Previously approved SHA-256: " + entry.previousSha256();
            }
            if (column >= MOD_COLUMN && column <= SOURCE_COLUMN) {
                return "<html><b>" + escapeHtml(entry.displayName()) + "</b><br/>"
                        + "Plugin ID: " + escapeHtml(entry.pluginId()) + "<br/>"
                        + "Mod ID: " + escapeHtml(entry.modId()) + "<br/>"
                        + "Author: " + escapeHtml(blankToDash(entry.modAuthor())) + "<br/>"
                        + escapeHtml(entry.jarPath().toString()) + "<br/>"
                        + "SHA-256: " + escapeHtml(entry.sha256()) + "</html>";
            }
            return null;
        }
    }

    /**
     * Dark cell renderer with alternating row colors and column-specific emphasis.
     */
    private static final class DarkCellRenderer extends DefaultTableCellRenderer {
        private DarkCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        /** {@inheritDoc} */
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setFont(column == MOD_COLUMN ? ZomboidApprovalTheme.BOLD_FONT : ZomboidApprovalTheme.BASE_FONT);

            if (isSelected) {
                setBackground(ZomboidApprovalTheme.SELECTION);
                setForeground(ZomboidApprovalTheme.TEXT);
            } else {
                setBackground(row % 2 == 0 ? ZomboidApprovalTheme.SURFACE : ZomboidApprovalTheme.SURFACE_ALT);
                setForeground(foregroundFor(column, value));
            }
            return this;
        }

        private Color foregroundFor(int column, Object value) {
            if (column == STATE_COLUMN) {
                return "CHANGED".equals(value) ? ZomboidApprovalTheme.ACCENT : ZomboidApprovalTheme.MUTED;
            }
            if (column == MOD_COLUMN) {
                return ZomboidApprovalTheme.TEXT;
            }
            return ZomboidApprovalTheme.MUTED;
        }
    }

    /**
     * Dark checkbox renderer for the Allow column.
     */
    private static final class DarkBooleanRenderer extends JCheckBox implements TableCellRenderer {
        private DarkBooleanRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        /** {@inheritDoc} */
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            setSelected(Boolean.TRUE.equals(value));
            setEnabled(table.isCellEditable(row, column));
            setBackground(isSelected
                    ? ZomboidApprovalTheme.SELECTION
                    : (row % 2 == 0 ? ZomboidApprovalTheme.SURFACE : ZomboidApprovalTheme.SURFACE_ALT));
            setForeground(ZomboidApprovalTheme.TEXT);
            return this;
        }
    }
}
