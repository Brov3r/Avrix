package com.avrix.provider.zomboid.approval.ui;

import com.avrix.provider.zomboid.approval.ApprovalLanguages;
import com.avrix.provider.zomboid.approval.ApprovalMessages;
import com.avrix.provider.zomboid.approval.MixinTargetInspector;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises approval model state, mouse selection, and background inspection without a top-level window.
 */
public class ApprovalTableHeadlessTest {

    @TempDir
    private Path directory;

    /**
     * Verifies that expanding rows preserves entry selection and that the row header toggles the right archive.
     *
     * @throws Exception if the EDT cannot execute the test
     */
    @Test
    public void shouldPreserveSelectionsAcrossExpansion() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var messages = new ApprovalMessages();
            var model = new ApprovalTableModel(List.of(entry("a"), entry("b")), messages,
                    (path, mixins) -> new MixinTargetInspector.Result(mixins, List.of(), List.of()));
            var table = new ApprovalTable(model, messages);
            try {
                var header = new ApprovalRowHeader(table, model);
                var selectAll = new ApprovalTriStateCheckBox(model);
                header.setSelectionChanged(selectAll::updateState);
                table.setRowHeader(header);
                table.setRowHeight(ApprovalColumns.BASE_ROW_HEIGHT);
                model.toggleExpanded(0);
                table.updateRowHeights();
                assertThat(model.getRowCount()).isEqualTo(3);
                assertThat(model.isDetailRow(1)).isTrue();
                assertThat(model.entryIndex(2)).isEqualTo(1);

                int secondRowY = table.getCellRect(2, 0, true).y + 5;
                header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                        0, 20, secondRowY, 1, false, MouseEvent.BUTTON1));
                assertThat(model.isAllowed(0)).isFalse();
                assertThat(model.isAllowed(1)).isTrue();
                assertThat(selectAll.isSelected()).isFalse();

                model.toggleExpanded(0);
                assertThat(model.getRowCount()).isEqualTo(2);
                assertThat(model.decisions()).extracting(decision -> decision.allow()).containsExactly(false, true);
                selectAll.doClick(0);
                assertThat(model.allowedCount()).isEqualTo(2);
                selectAll.doClick(0);
                assertThat(model.allowedCount()).isZero();
            } finally {
                table.cancelInspections();
            }
        });
    }

    /**
     * Verifies that slow inspection leaves the EDT responsive, is deduplicated, and refreshes the detail panel.
     *
     * @throws Exception if EDT coordination fails
     */
    @Test
    @Timeout(15)
    public void shouldInspectOffEdtAndRefreshDetails() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var responsive = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var inspectedOnEdt = new AtomicBoolean();
        var tableRef = new AtomicReference<ApprovalTable>();
        var firstPanel = new AtomicReference<ApprovalDetailPanel>();
        var messages = new ApprovalMessages();
        var model = new ApprovalTableModel(List.of(entry("a")), messages, (path, mixins) -> {
            calls.incrementAndGet();
            inspectedOnEdt.set(SwingUtilities.isEventDispatchThread());
            started.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new MixinTargetInspector.Result(mixins, List.of("example.Target"), List.of());
        });
        try {
            SwingUtilities.invokeLater(() -> {
                var table = new ApprovalTable(model, messages);
                tableRef.set(table);
                model.toggleExpanded(0);
                table.updateRowHeights();
                firstPanel.set(table.detailPanel(0));
                table.detailPanel(0);
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            SwingUtilities.invokeLater(responsive::countDown);
            assertThat(responsive.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(inspectedOnEdt).isFalse();
            assertThat(calls).hasValue(1);
            assertThat(model.cachedInspection(0)).isEmpty();
            release.countDown();

            var refreshed = new AtomicBoolean();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!refreshed.get() && System.nanoTime() < deadline) {
                SwingUtilities.invokeAndWait(() -> refreshed.set(model.cachedInspection(0).isPresent()
                        && tableRef.get().detailPanel(0) != firstPanel.get()));
                if (!refreshed.get()) {
                    Thread.sleep(10);
                }
            }
            assertThat(refreshed).isTrue();
            assertThat(model.cachedInspection(0).orElseThrow().targetClasses()).containsExactly("example.Target");
            SwingUtilities.invokeAndWait(() -> {
                messages.setLanguage(ApprovalLanguages.detect(Locale.of("ru")));
                tableRef.get().languageChanged();
                assertThat(tableRef.get().detailPanel(0)).isNotSameAs(firstPanel.get());
            });
            assertThat(calls).hasValue(1);
        } finally {
            release.countDown();
            SwingUtilities.invokeAndWait(() -> {
                if (tableRef.get() != null) {
                    tableRef.get().cancelInspections();
                }
            });
        }
    }

    /**
     * Verifies that closing the dialog cancels an active inspection and prevents new background work.
     *
     * @throws Exception if EDT coordination fails
     */
    @Test
    @Timeout(15)
    public void shouldCancelInspectionsAndPreventNewWork() throws Exception {
        var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var tableRef = new AtomicReference<ApprovalTable>();
        var messages = new ApprovalMessages();
        var model = new ApprovalTableModel(List.of(entry("a"), entry("b")), messages,
                (path, mixins) -> {
                    calls.incrementAndGet();
                    started.countDown();
                    try {
                        release.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        interrupted.countDown();
                        Thread.currentThread().interrupt();
                    }
                    return new MixinTargetInspector.Result(mixins, List.of("example.Target"), List.of());
                });
        try {
            SwingUtilities.invokeLater(() -> {
                var table = new ApprovalTable(model, messages);
                tableRef.set(table);
                model.toggleExpanded(0);
                table.updateRowHeights();
                table.detailPanel(0);
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

            SwingUtilities.invokeAndWait(() -> tableRef.get().cancelInspections());
            assertThat(interrupted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(calls).hasValue(1);

            SwingUtilities.invokeAndWait(() -> tableRef.get().detailPanel(1));
            assertThat(calls).hasValue(1);
        } finally {
            release.countDown();
            SwingUtilities.invokeAndWait(() -> {
                if (tableRef.get() != null) {
                    tableRef.get().cancelInspections();
                }
            });
        }
    }

    private ZomboidApprovalEntry entry(String hash) {
        return new ZomboidApprovalEntry("mod", "plugin", "Author", "Plugin", "mod", "Mod", "",
                directory.resolve(hash + ".jar"), hash.repeat(64), 0L, 0L, "", List.of("example.Mixin"));
    }
}
