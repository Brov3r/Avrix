package com.avrix.provider.zomboid.approval.ui;

import com.avrix.provider.zomboid.approval.ApprovalLanguage;
import com.avrix.provider.zomboid.approval.ApprovalLanguages;
import com.avrix.provider.zomboid.approval.ApprovalMessage;
import com.avrix.provider.zomboid.approval.ApprovalMessages;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;
import com.avrix.provider.zomboid.approval.ZomboidApprovalResponse;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builds and shows the Project Zomboid plugin approval dialog.
 */
public final class ApprovalDialog {

    private static final int WINDOW_WIDTH = 980;
    private static final int WINDOW_HEIGHT = 620;
    private static final int HEADER_TEXT_WIDTH = WINDOW_WIDTH - 80;

    private ApprovalDialog() {
        throw new UnsupportedOperationException("ApprovalDialog utility class cannot be instantiated");
    }

    /**
     * Shows the approval dialog and waits for the user decision.
     *
     * @param entries pending approval entries
     * @return user response
     */
    public static ZomboidApprovalResponse show(List<ZomboidApprovalEntry> entries) {
        ApprovalMessages messages = new ApprovalMessages();
        ApprovalTableModel model = new ApprovalTableModel(entries, messages);
        AtomicReference<ZomboidApprovalResponse> result =
                new AtomicReference<>(new ZomboidApprovalResponse(false, List.of()));

        JDialog dialog = new JDialog((java.awt.Frame) null, messages.get(ApprovalMessage.WINDOW_TITLE), true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        applyParentProcessIcon(dialog);
        dialog.getContentPane().setBackground(ZomboidApprovalTheme.WINDOW);
        dialog.addWindowListener(new WindowAdapter() {
            /**
             * Handles the window close request by returning an empty response.
             *
             * @param event window event
             */
            @Override
            public void windowClosing(WindowEvent event) {
                result.set(new ZomboidApprovalResponse(false, List.of()));
                dialog.dispose();
            }
        });

        ApprovalTable table = new ApprovalTable(model, messages);
        dialog.addWindowListener(new WindowAdapter() {
            /**
             * Cancels outstanding table inspections after the dialog has been closed.
             *
             * @param event window event
             */
            @Override
            public void windowClosed(WindowEvent event) {
                table.cancelInspections();
            }
        });
        table.setUI(new ApprovalTableUI());
        table.setTableHeader(new ApprovalTableHeader(table.getColumnModel()));
        table.setFillsViewportHeight(true);
        table.setRowHeight(ApprovalColumns.BASE_ROW_HEIGHT);
        table.updateRowHeights();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setGridColor(ZomboidApprovalTheme.BORDER);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setBackground(ZomboidApprovalTheme.SURFACE);
        table.setForeground(ZomboidApprovalTheme.TEXT);
        table.setSelectionBackground(ZomboidApprovalTheme.SELECTION);
        table.setSelectionForeground(ZomboidApprovalTheme.TEXT);
        table.setFont(ZomboidApprovalTheme.BASE_FONT);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setDefaultRenderer(Object.class, new ApprovalCellRenderer());
        table.getTableHeader().setDefaultRenderer(ZomboidApprovalTheme.headerRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));

        TableColumnModel columns = table.getColumnModel();
        configureColumn(columns.getColumn(ApprovalColumns.STATE), 90, 80, 120);
        configureColumn(columns.getColumn(ApprovalColumns.MOD), 320, 180, Integer.MAX_VALUE);
        configureColumn(columns.getColumn(ApprovalColumns.AUTHOR), 130, 90, Integer.MAX_VALUE);
        configureColumn(columns.getColumn(ApprovalColumns.SIZE), 80, 60, 100);

        columns.getColumn(ApprovalColumns.MOD).setCellRenderer(new ApprovalModNameRenderer());
        columns.getColumn(ApprovalColumns.STATE).setCellRenderer(new ApprovalStatusBadgeRenderer(messages));

        ApprovalRowHeader rowHeader = new ApprovalRowHeader(table, model);
        ApprovalTriStateCheckBox selectAll = new ApprovalTriStateCheckBox(model);
        selectAll.setToolTipText(messages.get(ApprovalMessage.TOOLTIP_SELECT_ALL));
        selectAll.setSelectionChanged(rowHeader::repaint);
        rowHeader.setSelectionChanged(() -> {
            selectAll.updateState();
            rowHeader.repaint();
        });
        table.setRowHeader(rowHeader);

        JScrollPane scroll = new JScrollPane(
                table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scroll.setBorder(BorderFactory.createLineBorder(ZomboidApprovalTheme.BORDER));
        scroll.getViewport().setBackground(ZomboidApprovalTheme.SURFACE);
        scroll.setRowHeaderView(rowHeader);
        JPanel allowHeader = new JPanel(new GridBagLayout());
        allowHeader.setOpaque(true);
        allowHeader.setBackground(ZomboidApprovalTheme.HEADER);
        allowHeader.setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 1, ZomboidApprovalTheme.BORDER_STRONG
        ));
        allowHeader.add(selectAll);
        allowHeader.setPreferredSize(new Dimension(ApprovalRowHeader.WIDTH, 38));
        scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, allowHeader);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getHorizontalScrollBar().setUnitIncrement(18);
        ZomboidApprovalTheme.styleScrollBar(scroll.getVerticalScrollBar());
        ZomboidApprovalTheme.styleScrollBar(scroll.getHorizontalScrollBar());
        scroll.setMinimumSize(new Dimension(0, 0));
        scroll.setPreferredSize(new Dimension(WINDOW_WIDTH - 36, WINDOW_HEIGHT - 230));

        JLabel title = new JLabel(messages.get(ApprovalMessage.HEADER_TITLE));
        title.setFont(ZomboidApprovalTheme.TITLE_FONT);
        title.setForeground(ZomboidApprovalTheme.TEXT);
        JLabel subtitle = new JLabel(ApprovalFormatting.htmlText(
                messages.get(ApprovalMessage.HEADER_SUBTITLE),
                HEADER_TEXT_WIDTH
        ));
        subtitle.setFont(ZomboidApprovalTheme.SUBTITLE_FONT);
        subtitle.setForeground(ZomboidApprovalTheme.MUTED);
        JLabel identityNote = new JLabel(ApprovalFormatting.htmlText(
                messages.get(ApprovalMessage.HEADER_IDENTITY),
                HEADER_TEXT_WIDTH
        ));
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

        JCheckBox remember = new JCheckBox(messages.get(ApprovalMessage.REMEMBER_LABEL));
        ZomboidApprovalTheme.styleCheckBox(remember);
        remember.setToolTipText(messages.get(ApprovalMessage.REMEMBER_TOOLTIP));
        remember.addItemListener(event -> table.repaint());

        JLabel rememberHint = new JLabel(messages.get(ApprovalMessage.REMEMBER_HINT));
        rememberHint.setForeground(ZomboidApprovalTheme.MUTED);
        rememberHint.setFont(ZomboidApprovalTheme.SMALL_FONT);

        JPanel rememberRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rememberRow.setOpaque(false);
        rememberRow.add(remember);
        rememberRow.add(rememberHint);

        JButton allowAll = new JButton(messages.get(ApprovalMessage.BUTTON_ALLOW_ALL));
        JButton denyAll = new JButton(messages.get(ApprovalMessage.BUTTON_DENY_ALL));
        JButton apply = new JButton(messages.get(ApprovalMessage.BUTTON_APPLY));
        JButton cancel = new JButton(messages.get(ApprovalMessage.BUTTON_CANCEL));

        ZomboidApprovalTheme.styleButton(allowAll, new Color(0x26332F), ZomboidApprovalTheme.ALLOW);
        ZomboidApprovalTheme.styleButton(denyAll, new Color(0x2D2B33), ZomboidApprovalTheme.DENY);
        ZomboidApprovalTheme.styleButton(apply, ZomboidApprovalTheme.ACCENT, ZomboidApprovalTheme.INK);
        ZomboidApprovalTheme.styleButton(cancel, ZomboidApprovalTheme.HEADER, ZomboidApprovalTheme.TEXT);
        allowAll.setIcon(ZomboidApprovalIcons.icon("tick", 14, ZomboidApprovalTheme.ALLOW));
        denyAll.setIcon(ZomboidApprovalIcons.icon("cross", 14, ZomboidApprovalTheme.DENY));
        apply.setIcon(ZomboidApprovalIcons.icon("tick", 14, ZomboidApprovalTheme.INK));
        cancel.setIcon(ZomboidApprovalIcons.icon("cross", 14, ZomboidApprovalTheme.MUTED));

        allowAll.addActionListener(event -> {
            model.setAll(true);
            table.updateRowHeights();
            selectAll.updateState();
            rowHeader.repaint();
        });
        denyAll.addActionListener(event -> {
            model.setAll(false);
            table.updateRowHeights();
            selectAll.updateState();
            rowHeader.repaint();
        });
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

        JComboBox<ApprovalLanguage> language = new JComboBox<>(
                ApprovalLanguages.available().toArray(new ApprovalLanguage[0])
        );
        language.setSelectedItem(messages.language());
        language.setToolTipText(messages.get(ApprovalMessage.LANGUAGE_TOOLTIP));
        language.setPreferredSize(new Dimension(170, 34));
        ZomboidApprovalTheme.styleComboBox(language);

        JPanel actionsRow = new JPanel(new BorderLayout(12, 0));
        actionsRow.setOpaque(false);
        actionsRow.add(language, BorderLayout.WEST);
        actionsRow.add(actions, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        footer.add(rememberRow, BorderLayout.NORTH);
        footer.add(actionsRow, BorderLayout.SOUTH);
        footer.setMinimumSize(footer.getPreferredSize());

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

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(WINDOW_WIDTH, Math.max(760, screen.width - 100));
        int height = Math.min(WINDOW_HEIGHT, Math.max(430, screen.height - 100));
        dialog.setMinimumSize(new Dimension(760, 430));
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(null);

        Runnable applyLanguage = () -> {
            dialog.setTitle(messages.get(ApprovalMessage.WINDOW_TITLE));
            title.setText(messages.get(ApprovalMessage.HEADER_TITLE));
            subtitle.setText(ApprovalFormatting.htmlText(
                    messages.get(ApprovalMessage.HEADER_SUBTITLE),
                    HEADER_TEXT_WIDTH
            ));
            identityNote.setText(ApprovalFormatting.htmlText(
                    messages.get(ApprovalMessage.HEADER_IDENTITY),
                    HEADER_TEXT_WIDTH
            ));
            remember.setText(messages.get(ApprovalMessage.REMEMBER_LABEL));
            remember.setToolTipText(messages.get(ApprovalMessage.REMEMBER_TOOLTIP));
            rememberHint.setText(messages.get(ApprovalMessage.REMEMBER_HINT));
            allowAll.setText(messages.get(ApprovalMessage.BUTTON_ALLOW_ALL));
            denyAll.setText(messages.get(ApprovalMessage.BUTTON_DENY_ALL));
            apply.setText(messages.get(ApprovalMessage.BUTTON_APPLY));
            cancel.setText(messages.get(ApprovalMessage.BUTTON_CANCEL));
            language.setToolTipText(messages.get(ApprovalMessage.LANGUAGE_TOOLTIP));
            selectAll.setToolTipText(messages.get(ApprovalMessage.TOOLTIP_SELECT_ALL));
            table.languageChanged();
            dialog.revalidate();
            dialog.repaint();
        };
        language.addActionListener(event -> {
            ApprovalLanguage selected = (ApprovalLanguage) language.getSelectedItem();
            if (selected == null || selected.equals(messages.language())) {
                return;
            }
            messages.setLanguage(selected);
            applyLanguage.run();
        });

        SwingUtilities.invokeLater(dialog::toFront);
        dialog.setVisible(true);
        return result.get();
    }

    /**
     * Applies the parent process executable icon when the operating system exposes one.
     *
     * @param dialog dialog to decorate
     */
    private static void applyParentProcessIcon(JDialog dialog) {
        ProcessHandle.current().parent()
                .map(ProcessHandle::info)
                .flatMap(info -> info.command())
                .ifPresent(command -> {
                    try {
                        Icon icon = FileSystemView.getFileSystemView().getSystemIcon(new File(command));
                        if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                            dialog.setIconImage(toImage(icon));
                        }
                    } catch (RuntimeException ignored) {
                        // The parent process icon is best-effort; the default window icon is fine.
                    }
                });
    }

    /**
     * Converts a Swing icon to an image.
     *
     * @param icon source icon
     * @return icon image
     */
    private static Image toImage(Icon icon) {
        if (icon instanceof ImageIcon imageIcon) {
            return imageIcon.getImage();
        }
        BufferedImage image = new BufferedImage(
                Math.max(1, icon.getIconWidth()),
                Math.max(1, icon.getIconHeight()),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void configureColumn(TableColumn column, int width, int minWidth, int maxWidth) {
        column.setPreferredWidth(width);
        column.setMinWidth(minWidth);
        column.setMaxWidth(maxWidth);
        column.setResizable(true);
    }
}
