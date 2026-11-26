package com.avrix.provider.zomboid.approval.ui;

import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies safe metadata rendering without opening a window.
 */
public class ApprovalRendererTest {

    /**
     * Verifies that external text cannot create an HTML view or load embedded resources.
     *
     * @param text metadata text to render
     * @throws Exception if the EDT cannot execute the test
     */
    @ParameterizedTest
    @ValueSource(strings = {"<html><b>Trusted author</b></html>",
            "<html><img src='http://127.0.0.1:1/unapproved'>Author</html>", "Author & contributors"})
    public void shouldRenderMetadataAsPlainText(String text) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var table = new JTable(1, 4);
            table.setDefaultRenderer(Object.class, new ApprovalCellRenderer());
            var renderer = table.getCellRenderer(0, ApprovalColumns.AUTHOR);
            var label = (JLabel) renderer.getTableCellRendererComponent(table, text, false, false, 0,
                    ApprovalColumns.AUTHOR);
            assertThat(label.getText()).isEqualTo(text);
            assertThat(label.getClientProperty("html")).isNull();

            label.setSize(300, 50);
            var image = new BufferedImage(300, 50, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            try {
                label.paint(graphics);
            } finally {
                graphics.dispose();
            }
            renderer.getTableCellRendererComponent(table, text, true, true, 0, ApprovalColumns.AUTHOR);
            assertThat(label.getClientProperty("html")).isNull();
        });
    }

    /**
     * Verifies that a sanitized approval entry renders without control characters in a table cell.
     *
     * @throws Exception if the EDT cannot execute the test
     */
    @Test
    public void shouldRenderSanitizedApprovalEntryWithoutControls() throws Exception {
        ZomboidApprovalEntry entry = new ZomboidApprovalEntry(
                "mod-id",
                "plugin-id",
                "Auth\u001B\r\nor",
                "Dis\u0000play\n\rName\u202E",
                "mod",
                "Sou\u001Brce",
                "",
                Path.of("plugin.jar"),
                "a".repeat(64),
                0L,
                0L,
                "",
                List.of()
        );

        SwingUtilities.invokeAndWait(() -> {
            var table = new JTable(1, 4);
            table.setDefaultRenderer(Object.class, new ApprovalCellRenderer());
            var renderer = table.getCellRenderer(0, ApprovalColumns.AUTHOR);
            var label = (JLabel) renderer.getTableCellRendererComponent(
                    table,
                    entry.modAuthor(),
                    false,
                    false,
                    0,
                    ApprovalColumns.AUTHOR
            );

            assertThat(label.getText()).isEqualTo("Auth or");
            assertThat(label.getText()).doesNotContain("\n", "\r", "\u001B");
            assertThat(entry.displayName()).isEqualTo("Dis play Name");
            assertThat(entry.displayName()).doesNotContain("\n", "\r", "\u001B", "\u202E", "\u0000");
        });
    }
}
