package com.avrix.provider.zomboid.approval.ui;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;

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
}
